package com.taller.portal.service;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.*;
import java.util.zip.ZipInputStream;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Importa el TXT oficial SEPOMEX al catálogo local, sin depender de servicios externos en ejecución. */
@Service
public class SepomexImportService {
    private static final int LOTE = 500;
    private final JdbcTemplate jdbc;
    /** @param jdbc acceso transaccional a la tabla de catálogo. */
    public SepomexImportService(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    /** Resultado medible de una importación. @param filasArchivo filas de datos leídas @param filasInsertadas nuevas filas @param totalCatalogo total posterior. */
    public record Resultado(long filasArchivo, long filasInsertadas, long totalCatalogo) {}

    /** Lee un TXT ISO-8859-1 o su ZIP oficial, normaliza texto e inserta solo asentamientos inexistentes.
     * @param archivo ruta al archivo descargado de Correos de México
     * @return conteos de lectura, altas y catálogo total
     * @throws IOException si el archivo es inválido o no puede leerse. */
    @Transactional
    public Resultado importar(Path archivo) throws IOException {
        if (archivo == null || !Files.isRegularFile(archivo)) throw new FileNotFoundException("No existe el archivo SEPOMEX: " + archivo);
        long leidas = 0; long insertadas = 0;
        try (InputStream original = Files.newInputStream(archivo); InputStream contenido = abrirContenido(original);
             BufferedReader lector = new BufferedReader(new InputStreamReader(contenido, StandardCharsets.ISO_8859_1))) {
            Map<String, Integer> columnas = null; List<Fila> lote = new ArrayList<>(LOTE); String linea;
            while ((linea = lector.readLine()) != null) {
                if (linea.isBlank()) continue;
                if (columnas == null) { if (!linea.startsWith("d_codigo|")) continue; columnas = indices(linea); continue; }
                Fila fila = fila(linea, columnas);
                if (fila == null) continue;
                leidas++; lote.add(fila);
                if (lote.size() == LOTE) { insertadas += insertar(lote); lote.clear(); }
            }
            if (columnas == null) throw new IOException("El archivo SEPOMEX no contiene el encabezado d_codigo esperado");
            if (!lote.isEmpty()) insertadas += insertar(lote);
        }
        Long total = jdbc.queryForObject("select count(*) from sepomex_asentamientos", Long.class);
        return new Resultado(leidas, insertadas, total == null ? 0 : total);
    }

    /** Abre la primera entrada ZIP o el stream TXT original. @param entrada bytes descargados @return stream de texto sin cerrar la entrada raíz. */
    private InputStream abrirContenido(InputStream entrada) throws IOException {
        PushbackInputStream prueba = new PushbackInputStream(entrada, 4); byte[] firma = prueba.readNBytes(4); prueba.unread(firma);
        if (firma.length == 4 && firma[0] == 'P' && firma[1] == 'K') { ZipInputStream zip = new ZipInputStream(prueba); if (zip.getNextEntry() == null) throw new IOException("El ZIP SEPOMEX no contiene TXT"); return zip; }
        return prueba;
    }
    /** Construye índices por nombre de las columnas oficiales. @param encabezado fila separada por | @return posiciones requeridas. @throws IOException si falta una columna. */
    private Map<String, Integer> indices(String encabezado) throws IOException {
        String[] partes = encabezado.split("\\|", -1); Map<String, Integer> resultado = new HashMap<>();
        for (int indice = 0; indice < partes.length; indice++) resultado.put(partes[indice].trim(), indice);
        for (String requerida : List.of("d_codigo", "d_asenta", "d_tipo_asenta", "D_mnpio", "d_estado", "c_estado", "c_mnpio"))
            if (!resultado.containsKey(requerida)) throw new IOException("Falta la columna SEPOMEX " + requerida);
        return resultado;
    }
    /** Convierte una línea oficial en una fila válida. @param linea datos delimitados @param columnas índices del encabezado @return fila o null si no tiene CP válido. */
    private Fila fila(String linea, Map<String, Integer> columnas) {
        String[] partes = linea.split("\\|", -1); String codigoPostal = valor(partes, columnas, "d_codigo");
        if (!codigoPostal.matches("\\d{5}")) return null;
        Fila resultado = new Fila(codigoPostal, valor(partes, columnas, "d_asenta"), valor(partes, columnas, "d_tipo_asenta"),
            valor(partes, columnas, "D_mnpio"), valor(partes, columnas, "d_estado"), valor(partes, columnas, "c_estado"), valor(partes, columnas, "c_mnpio"));
        return resultado.completa() ? resultado : null;
    }
    /** Normaliza espacios y limita a los tamaños de la migración. @param partes columnas de una línea @param columnas posiciones @param nombre nombre oficial @return texto seguro. */
    private String valor(String[] partes, Map<String, Integer> columnas, String nombre) {
        int indice = columnas.get(nombre); String texto = indice < partes.length ? partes[indice] : "";
        return texto.replaceAll("\\s+", " ").trim();
    }
    /** Inserta un lote con condición NOT EXISTS, haciendo la operación repetible sin duplicar filas.
     * @param filas lote normalizado @return filas nuevas insertadas. */
    private int insertar(List<Fila> filas) {
        String sql = "insert into sepomex_asentamientos (codigo_postal,colonia,tipo_asentamiento,municipio,estado,c_estado,c_mnpio) "
            + "select ?,?,?,?,?,?,? where not exists (select 1 from sepomex_asentamientos where codigo_postal=? and colonia=? and tipo_asentamiento=? and municipio=? and estado=? and c_estado=? and c_mnpio=?)";
        int[][] resultado = jdbc.batchUpdate(sql, filas, LOTE, this::parametros);
        return (int) Arrays.stream(resultado).flatMapToInt(Arrays::stream).filter(filasAfectadas -> filasAfectadas > 0).count();
    }
    /** Enlaza los catorce parámetros sin concatenar entrada externa. @param sentencia prepared statement @param fila datos normalizados @throws SQLException si JDBC no acepta el valor. */
    private void parametros(PreparedStatement sentencia, Fila fila) throws SQLException {
        String[] valores = fila.valores(); for (int indice = 0; indice < valores.length; indice++) sentencia.setString(indice + 1, valores[indice]);
        for (int indice = 0; indice < valores.length; indice++) sentencia.setString(indice + 8, valores[indice]);
    }
    /** Fila interna del catálogo. @param codigoPostal CP @param colonia asentamiento @param tipo tipo @param municipio municipio @param estado entidad @param cEstado clave @param cMnpio clave municipal. */
    private record Fila(String codigoPostal, String colonia, String tipo, String municipio, String estado, String cEstado, String cMnpio) {
        /** @return verdadero si todos los datos necesarios están presentes y caben en la tabla. */
        boolean completa() { return !colonia.isBlank() && !tipo.isBlank() && !municipio.isBlank() && !estado.isBlank() && cEstado.length() <= 2 && cMnpio.length() <= 4 && colonia.length() <= 160 && tipo.length() <= 80 && municipio.length() <= 120 && estado.length() <= 120; }
        /** @return valores en el orden SQL de inserción. */
        String[] valores() { return new String[] {codigoPostal, colonia, tipo, municipio, estado, cEstado, cMnpio}; }
    }
}
