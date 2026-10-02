package com.taller.portal;

import com.taller.portal.repository.CodigosPostalesRepository;
import com.taller.portal.service.CodigosPostalesFacade;
import com.taller.portal.service.SepomexImportService;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Prueba importación ISO-8859-1, Repository, Facade y ruta protegida en H2 aislado. */
@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:sepomex;MODE=MySQL;DB_CLOSE_DELAY=-1", "spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa", "spring.datasource.password=", "spring.jpa.hibernate.ddl-auto=create-drop", "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect"})
@AutoConfigureMockMvc
@Transactional
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class SepomexCatalogoTest {
    @Autowired SepomexImportService importador;
    @Autowired CodigosPostalesRepository repository;
    @Autowired CodigosPostalesFacade facade;
    @Autowired MockMvc mvc;
    /** @return autenticación permitida de prueba. */
    private UsernamePasswordAuthenticationToken autorizada() { return new UsernamePasswordAuthenticationToken("qa", "", List.of(new SimpleGrantedAuthority("ROLE_RECEPCIONISTA"))); }
    /** @return TXT oficial mínimo codificado latin1. @throws Exception si no se crea el temporal. */
    private Path archivo() throws Exception {
        Path ruta=Files.createTempFile("sepomex-", ".txt");
        String texto="Aviso legal\n"+"d_codigo|d_asenta|d_tipo_asenta|D_mnpio|d_estado|d_ciudad|d_CP|c_estado|c_oficina|c_CP|c_tipo_asenta|c_mnpio|id_asenta_cpcons|d_zona|c_cve_ciudad\n"+
            "01000|San Ángel|Colonia|Álvaro Obregón|Ciudad de México|Ciudad de México|01001|09|01001||09|010|0001|Urbano|01\n"+
            "01000|Florida|Colonia|Álvaro Obregón|Ciudad de México|Ciudad de México|01001|09|01001||09|010|0002|Urbano|01\n"+
            "72000|Centro|Colonia|Puebla|Puebla|Puebla|72001|21|72001||09|114|0001|Urbano|01\n";
        Files.writeString(ruta, texto, StandardCharsets.ISO_8859_1); return ruta;
    }
    /** Importa dos veces sin duplicados y conserva ñ/acento. @throws Exception por IO. */
    @Test void importacionIdempotenteYCaracteres() throws Exception {
        Path archivo=archivo(); try { var primera=importador.importar(archivo); var segunda=importador.importar(archivo);
            assertEquals(3, primera.filasArchivo()); assertEquals(3, primera.filasInsertadas()); assertEquals(3, primera.totalCatalogo());
            assertEquals(0, segunda.filasInsertadas()); assertEquals(3, segunda.totalCatalogo());
            assertTrue(repository.porCodigoPostal("01000").stream().anyMatch(colonia -> colonia.colonia().equals("San Ángel")));
            assertEquals("Álvaro Obregón", facade.porCodigoPostal("01000", autorizada()).municipio());
        } finally { Files.deleteIfExists(archivo); }
    }
    /** Comprueba consultas parametrizadas, entradas inválidas y estado sin municipios. @throws Exception por IO. */
    @Test void repositoryYFacade() throws Exception {
        Path archivo=archivo(); try { importador.importar(archivo);
            assertEquals(List.of("Ciudad de México", "Puebla"), repository.listarEstados());
            assertEquals(List.of("Álvaro Obregón"), repository.listarMunicipios("Ciudad de México"));
            assertEquals(0, repository.listarMunicipios("Sin estado").size()); assertEquals(2, repository.listarColonias("Ciudad de México", "Álvaro Obregón").size());
            assertNull(facade.porCodigoPostal("99999", autorizada()));
            assertThrows(IllegalArgumentException.class, () -> facade.porCodigoPostal("1000", autorizada()));
            assertThrows(IllegalArgumentException.class, () -> facade.listarMunicipios("x".repeat(121), autorizada()));
            assertThrows(AccessDeniedException.class, () -> facade.listarEstados(new UsernamePasswordAuthenticationToken("qa", "", List.of(new SimpleGrantedAuthority("ROLE_CLIENTE")))));
        } finally { Files.deleteIfExists(archivo); }
    }
    /** Comprueba que la ruta rechaza roles no autorizados. @throws Exception si MockMvc falla. */
    @Test void rutasAutorizadasYProhibidas() throws Exception {
        mvc.perform(get("/api/codigos-postales/estados")).andExpect(status().isForbidden());
        mvc.perform(get("/api/codigos-postales/estados").with(user("qa").roles("CLIENTE"))).andExpect(status().isForbidden());
        mvc.perform(get("/api/codigos-postales/abcde").with(user("qa").roles("RECEPCIONISTA"))).andExpect(status().isBadRequest());
    }
}
