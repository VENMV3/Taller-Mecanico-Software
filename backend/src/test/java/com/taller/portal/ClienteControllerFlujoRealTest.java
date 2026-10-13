package com.taller.portal;

import com.taller.portal.model.RoleName;
import com.taller.portal.model.Taller;
import com.taller.portal.model.User;
import com.taller.portal.repository.RoleRepository;
import com.taller.portal.repository.TallerRepository;
import com.taller.portal.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Verifica desde cero migraciones, serialización HTTP y suspensión autenticada de clientes. */
@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:clientecontrolador;MODE=MySQL;DB_CLOSE_DELAY=-1","spring.datasource.driver-class-name=org.h2.Driver","spring.datasource.username=sa","spring.datasource.password="})
@AutoConfigureMockMvc
@Transactional
@DirtiesContext(classMode=DirtiesContext.ClassMode.AFTER_CLASS)
class ClienteControllerFlujoRealTest {
 @Autowired MockMvc mvc; @Autowired ObjectMapper json; @Autowired TallerRepository talleres; @Autowired UserRepository usuarios; @Autowired RoleRepository roles;
 Taller taller; User administrador;
 /** Crea el alcance de prueba después de que Flyway aplica todas las migraciones en H2. */
 @BeforeEach void preparar(){taller=new Taller();taller.nombre="Taller Control Real";taller.nombreNormalizado="TALLER CONTROL REAL";taller.razonSocial="Control Real Servicios";taller.razonSocialNormalizada="CONTROL REAL SERVICIOS";taller.rfc="TCR260101AB1";taller.telefono="5512345678";taller.email="control@taller.local";taller.calle="Avenida Central 1";taller.colonia="Centro";taller.municipio="Ciudad de Mexico";taller.estado="Ciudad de Mexico";taller.codigoPostal="01000";taller.idempotencyKey="controller-control-real";taller=talleres.save(taller);administrador=new User();administrador.nombre="Administrador Control";administrador.email="admin.control@taller.local";administrador.celular="5512345679";administrador.passwordHash="hash";administrador.rol=roles.findByNombre(RoleName.ADMINISTRADOR_SISTEMA).orElseThrow();administrador.taller=taller;administrador.activo=true;administrador.emailVerificado=true;usuarios.save(administrador);}
 /** Registra, suspende y comprueba que un duplicado se traduce a 409 JSON, no a un error vacío. */
 @Test void registroYSuspensionPorControllerConMigracionesCompletas() throws Exception {String datos="{\"nombreCompleto\":\"Sofia Navarro Rios\",\"contactoAlternativo\":\"Mario Rios\",\"edad\":36,\"fechaNacimiento\":\"1990-01-10\",\"telefonoPersonal\":\"5512345680\",\"telefonoTrabajo\":\"5512345681\",\"email\":\"sofia.navarro@correo.local\",\"direccion\":{\"calle\":\"Avenida Central 10\",\"colonia\":\"Centro\",\"municipio\":\"Ciudad de Mexico\",\"estado\":\"Ciudad de Mexico\",\"codigoPostal\":\"01000\"},\"tallerId\":"+taller.id+"}";var alta=mvc.perform(multipart("/api/clientes").file(parte("datos","datos.json",MediaType.APPLICATION_JSON_VALUE,datos.getBytes())).file(parte("fotografia","foto.png",MediaType.IMAGE_PNG_VALUE,imagen())).header("Idempotency-Key","controller-alta").with(user(administrador.email).roles("ADMINISTRADOR_SISTEMA"))).andExpect(status().isCreated()).andExpect(jsonPath("$.id").isNumber()).andReturn();long id=json.readTree(alta.getResponse().getContentAsString()).get("id").asLong();mvc.perform(post("/api/clientes/{id}/suspender",id).param("tallerId",taller.id.toString()).with(user(administrador.email).roles("ADMINISTRADOR_SISTEMA"))).andExpect(status().isOk()).andExpect(jsonPath("$.estatus").value("SUS"));mvc.perform(multipart("/api/clientes").file(parte("datos","datos.json",MediaType.APPLICATION_JSON_VALUE,datos.getBytes())).file(parte("fotografia","foto.png",MediaType.IMAGE_PNG_VALUE,imagen())).header("Idempotency-Key","controller-duplicado").with(user(administrador.email).roles("ADMINISTRADOR_SISTEMA"))).andExpect(status().isConflict()).andExpect(jsonPath("$.message").exists());}
 /** Construye una parte multipart. @return archivo de petición. */
 private MockMultipartFile parte(String campo,String nombre,String tipo,byte[] contenido){return new MockMultipartFile(campo,nombre,tipo,contenido);}
 /** Genera una imagen PNG válida para reutilizar el validador de producción. @return bytes PNG. @throws Exception por codificación. */
 private byte[] imagen() throws Exception {var imagen=new BufferedImage(2,2,BufferedImage.TYPE_INT_RGB);var salida=new ByteArrayOutputStream();ImageIO.write(imagen,"png",salida);return salida.toByteArray();}
}
