package com.taller.portal;

import com.taller.portal.model.Cliente;
import com.taller.portal.repository.ClienteRepository;
import com.taller.portal.service.ClienteRegistrationFacade;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Verifica consultas, fachada y rutas con BD aislada; no modifica MySQL. */
@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:consulta;MODE=MySQL;DB_CLOSE_DELAY=-1", "spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa", "spring.datasource.password=", "spring.jpa.hibernate.ddl-auto=create-drop", "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect"})
@AutoConfigureMockMvc
@Transactional
class ClienteConsultaTest {
    @Autowired ClienteRepository repo;
    @Autowired ClienteRegistrationFacade facade;
    @Autowired MockMvc mvc;

    /** Crea datos aislados. @param n sufijo único @return entidad persistida. */
    private Cliente cliente(int n) {
        Cliente c=new Cliente(); c.nombreCompleto="Consulta Prueba "+n; c.contactoAlternativo="Contacto Alternativo";
        c.edad=26; c.fechaNacimiento=LocalDate.of(2000,1,1); c.telefonoPersonal="55000000"+n;
        c.telefonoTrabajo="56000000"+n; c.email="consulta"+n+"@example.test";
        c.fotografiaRuta="uploads/clientes/no-existe.png"; c.calle="Calle Uno"; c.colonia="Centro";
        c.municipio="Puebla"; c.estado="Puebla"; c.codigoPostal="72000"; c.idempotencyKey="consulta-"+n;
        return repo.saveAndFlush(c);
    }
    /** @param rol autoridad @return autenticación de prueba sin JWT. */
    private UsernamePasswordAuthenticationToken auth(String rol) {
        return new UsernamePasswordAuthenticationToken("qa", "", List.of(new SimpleGrantedAuthority("ROLE_"+rol)));
    }
    /** Comprueba paginación, orden, búsquedas parametrizadas e ID ausente. */
    @Test void repositoryConsultas() {
        var a=cliente(1); var b=cliente(2);
        var page=repo.consultar("%consulta%",org.springframework.data.domain.PageRequest.of(0,1,org.springframework.data.domain.Sort.by("id").descending()));
        assertEquals(2,page.getTotalElements()); assertEquals(b.id,page.getContent().get(0).id());
        assertEquals(a.id,repo.consultar("%550000001%",org.springframework.data.domain.PageRequest.of(0,10)).getContent().get(0).id());
        assertEquals(1,repo.consultar("%consulta2@example.test%",org.springframework.data.domain.PageRequest.of(0,10)).getTotalElements());
        assertTrue(repo.consultarDetalle(Long.MAX_VALUE).isEmpty());
        assertEquals(a.nombreCompleto,repo.consultarDetalle(a.id).orElseThrow().nombreCompleto());
    }
    /** Comprueba autorización y límites de parámetros sin modificar registros. */
    @Test void facadePermisosYParametros() {
        for(String rol: List.of("CLIENTE","DUENO","MECANICO","SECRETARIA","DESARROLLADOR"))
            assertThrows(AccessDeniedException.class,()->facade.consultar(0,10,"",auth(rol)));
        assertThrows(AccessDeniedException.class,()->facade.consultar(0,10,"",null));
        for(String rol:List.of("ADMINISTRADOR_SISTEMA","RECEPCIONISTA")) {
            var a=auth(rol);
            assertThrows(IllegalArgumentException.class,()->facade.consultar(-1,10,"",a));
            assertThrows(IllegalArgumentException.class,()->facade.consultar(0,51,"",a));
            assertThrows(IllegalArgumentException.class,()->facade.consultar(0,0,"",a));
            assertThrows(IllegalArgumentException.class,()->facade.consultar(0,10,"x".repeat(121),a));
            assertThrows(IllegalArgumentException.class,()->facade.consultar(0,10,"a\nb",a));
            assertThrows(IllegalArgumentException.class,()->facade.detalle(0L,a));
            assertEquals(0,facade.consultar(0,10,"%_!",a).total());
        }
    }
    /** Verifica HTTP 403 para listado, detalle y fotografía. @throws Exception por fallo HTTP. */
    @Test void rutasProhibidas() throws Exception {
        for(String rol:List.of("CLIENTE","DUENO","MECANICO","SECRETARIA","DESARROLLADOR"))
            for(String ruta:List.of("/api/clientes","/api/clientes/1","/api/clientes/1/fotografia"))
                mvc.perform(get(ruta).with(user("qa").roles(rol))).andExpect(status().isForbidden());
        mvc.perform(get("/api/clientes")).andExpect(status().isForbidden());
    }
    /** Verifica listado y detalle seguros, vacío, límites y 404. @throws Exception por fallo HTTP. */
    @Test void rutasPermitidas() throws Exception {
        var c=cliente(3);
        for(String rol:List.of("ADMINISTRADOR_SISTEMA","RECEPCIONISTA")) {
            mvc.perform(get("/api/clientes").with(user("qa").roles(rol)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.clientes[0].fotografiaRuta").doesNotExist());
            mvc.perform(get("/api/clientes/"+c.id).with(user("qa").roles(rol)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.nombreCompleto").value(c.nombreCompleto))
                .andExpect(jsonPath("$.idempotencyKey").doesNotExist());
            mvc.perform(get("/api/clientes?busqueda=inexistente").with(user("qa").roles(rol)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(0));
            mvc.perform(get("/api/clientes?tamanio=999").with(user("qa").roles(rol))).andExpect(status().isBadRequest());
            mvc.perform(get("/api/clientes/9223372036854775807").with(user("qa").roles(rol))).andExpect(status().isNotFound());
            mvc.perform(get("/api/clientes/"+c.id+"/fotografia").with(user("qa").roles(rol))).andExpect(status().isNotFound());
        }
    }
    /** Registra y consulta por HTTP; valida páginas y fotografía privada. @throws Exception por IO/HTTP. */
    @Test void registroConsultaYFoto() throws Exception {
        var image=new java.awt.image.BufferedImage(2,2,java.awt.image.BufferedImage.TYPE_INT_RGB);
        var bytes=new java.io.ByteArrayOutputStream(); javax.imageio.ImageIO.write(image,"png",bytes);
        var datos=new com.taller.portal.dto.ClienteDtos.Registro("QA Consulta HTTP","QA Contacto",java.time.Period.between(LocalDate.of(2000,1,1),LocalDate.now()).getYears(),LocalDate.of(2000,1,1),
            "5512345678","5612345678","qa-http@example.test",null,
            new com.taller.portal.dto.ClienteDtos.Direccion("Uno","Centro","Puebla","Puebla","72000"));
        var mapper=new com.fasterxml.jackson.databind.ObjectMapper().findAndRegisterModules();
        var json=new org.springframework.mock.web.MockMultipartFile("datos","datos.json","application/json",mapper.writeValueAsBytes(datos));
        var foto=new org.springframework.mock.web.MockMultipartFile("fotografia","qa.png","image/png",bytes.toByteArray());
        java.nio.file.Path creada=null;
        try {
            var response=mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart("/api/clientes")
                .file(json).file(foto).header("Idempotency-Key","qa-consulta-http").with(user("qa").roles("RECEPCIONISTA")))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.mensaje").value("Cliente Registrado")).andReturn();
            long id=mapper.readTree(response.getResponse().getContentAsString()).get("id").asLong();
            creada=java.nio.file.Path.of(repo.consultarRutaFoto(id).orElseThrow());
            for(int n=10;n<20;n++) cliente(n);
            var page=facade.consultar(0,10,"",auth("RECEPCIONISTA"));
            assertEquals(11,page.total()); assertEquals(10,page.clientes().size());
            assertEquals(id,facade.consultar(1,10,"",auth("RECEPCIONISTA")).clientes().get(0).id());
            mvc.perform(get("/api/clientes?busqueda=QA%20Consulta%20HTTP").with(user("qa").roles("RECEPCIONISTA")))
                .andExpect(status().isOk());
            assertEquals(id,facade.consultar(0,10,"QA Consulta HTTP",auth("RECEPCIONISTA")).clientes().get(0).id());
            mvc.perform(get("/api/clientes/"+id+"/fotografia").with(user("qa").roles("RECEPCIONISTA")))
                .andExpect(status().isOk()).andExpect(content().contentType("image/png"))
                .andExpect(header().string("Cache-Control","no-store"));
        } finally { if(creada!=null) java.nio.file.Files.deleteIfExists(creada); }
    }
}
