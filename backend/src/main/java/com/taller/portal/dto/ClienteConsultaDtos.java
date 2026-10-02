package com.taller.portal.dto;

import java.time.LocalDate;
import java.util.List;

/** Contratos de lectura: nunca incluyen rutas de disco ni claves idempotentes. */
public final class ClienteConsultaDtos {
    /** Impide instanciar el contenedor de contratos. */
    private ClienteConsultaDtos() {}
    /** Fila proyectada del listado; parámetros homónimos a columnas; fecha no persistida. */
    public record Resumen(Long id, String nombreCompleto, String telefonoPersonal,
                          String email, String municipio) {}
    /** Detalle de solo lectura, con todos los datos públicos del cliente. */
    public record Detalle(Long id, String nombreCompleto, String contactoAlternativo,
                          Integer edad, LocalDate fechaNacimiento, String telefonoPersonal,
                          String telefonoTrabajo, String email, String emailTrabajo,
                          String calle, String colonia, String municipio, String estado,
                          String codigoPostal) {}
    /** Página preparada por la fachada; total indica registros, no páginas. */
    public record Pagina(List<Resumen> clientes, int pagina, int tamanio, long total, int paginas) {}
    /** Imagen validada para servir bytes sin revelar su ubicación local. */
    public record Foto(byte[] contenido, String mime) {}
}
