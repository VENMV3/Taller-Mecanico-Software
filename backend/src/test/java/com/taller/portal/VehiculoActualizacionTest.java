package com.taller.portal;

import static org.junit.jupiter.api.Assertions.*;

import com.taller.portal.dto.ClienteDtos;
import com.taller.portal.dto.VehiculoDtos;
import com.taller.portal.model.Role;
import com.taller.portal.model.RoleName;
import com.taller.portal.model.Taller;
import com.taller.portal.model.User;
import com.taller.portal.repository.RoleRepository;
import com.taller.portal.repository.TallerRepository;
import com.taller.portal.repository.UserRepository;
import com.taller.portal.repository.VehiculoAuditoriaRepository;
import com.taller.portal.service.ClienteRegistrationFacade;
import com.taller.portal.service.VehiculoFacade;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Verifica con H2 la ficha inicial de UC-CV-08 y reserva el resto para la escritura autorizada. */
@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:vehiculoactualizacion;MODE=MySQL;DB_CLOSE_DELAY=-1","spring.datasource.driver-class-name=org.h2.Driver","spring.datasource.username=sa","spring.datasource.password="})
@Transactional @Rollback
class VehiculoActualizacionTest {
 @Autowired VehiculoFacade vehiculos;
 @Autowired ClienteRegistrationFacade clientes;
 @Autowired TallerRepository talleres;
 @Autowired UserRepository usuarios;
 @Autowired RoleRepository roles;
 @Autowired JdbcTemplate jdbc;
 @Autowired VehiculoAuditoriaRepository auditorias;
 Taller taller;
 UsernamePasswordAuthenticationToken recepcion;
 UsernamePasswordAuthenticationToken administrador;

 /** Prepara identidades de dos talleres y el asentamiento requerido por el registro de cliente. */
 @BeforeEach void preparar() throws Exception {
  jdbc.update("insert into sepomex_asentamientos(codigo_postal,colonia,tipo_asentamiento,municipio,estado,c_estado,c_mnpio) values('03100','Del Valle Centro','Colonia','Benito Juárez','Ciudad de México','09','014')");
  taller=guardarTaller("Taller Actualización","vehiculo-actualizacion");
  recepcion=autenticacion(crearUsuario("recepcion.actualizacion@taller.local",RoleName.RECEPCIONISTA,taller),RoleName.RECEPCIONISTA);
  administrador=autenticacion(crearUsuario("admin.actualizacion@taller.local",RoleName.ADMINISTRADOR_SISTEMA,taller),RoleName.ADMINISTRADOR_SISTEMA);
 }

 /** Expone la ficha mínima al taller dueño y oculta el vehículo a recepción de otro taller. */
 @Test void fichaRespetaAlcanceYNoExponeContactoDelCliente() throws Exception {
  Long clienteId=registrarCliente();
  Long vehiculoId=vehiculos.registrar(new VehiculoDtos.Registro(clienteId,"1HGCM82633A004352","ABC-123","Honda","Civic",2025,"Touring","Azul",1200L),recepcion).id();
  VehiculoDtos.Detalle ficha=vehiculos.detalle(vehiculoId,recepcion);
  assertAll(()->assertEquals(vehiculoId,ficha.id()),()->assertEquals("1HGCM82633A004352",ficha.vin()),()->assertEquals("abc123",ficha.placa()),()->assertEquals(clienteId,ficha.clienteId()),()->assertEquals("María Actualización",ficha.clienteNombre()),()->assertEquals("ACT",ficha.estatus()));
  Taller otroTaller=guardarTaller("Taller Ajeno","vehiculo-actualizacion-ajeno");
  var ajena=autenticacion(crearUsuario("recepcion.ajena@taller.local",RoleName.RECEPCIONISTA,otroTaller),RoleName.RECEPCIONISTA);
  ResponseStatusException error=assertThrows(ResponseStatusException.class,()->vehiculos.detalle(vehiculoId,ajena));
  assertEquals(404,error.getStatusCode().value());
 }

 /** Actualiza un campo, audita su valor anterior y evita auditar un reenvío sin cambios. */
 @Test void actualizaConVersionYAditaSoloCambios() throws Exception {
  Long clienteId=registrarCliente();
  Long vehiculoId=vehiculos.registrar(new VehiculoDtos.Registro(clienteId,"1HGCM82633A004352","ABC-123","Honda","Civic",2025,"Touring","Azul",1200L),recepcion).id();
  VehiculoDtos.Detalle antes=vehiculos.detalle(vehiculoId,recepcion);
  VehiculoDtos.Actualizacion cambio=new VehiculoDtos.Actualizacion(antes.vin(),antes.placa(),antes.marca(),antes.modelo(),antes.anio(),antes.version(),"Rojo",antes.kilometraje(),antes.controlVersion());
  VehiculoDtos.Detalle despues=vehiculos.actualizar(vehiculoId,cambio,recepcion);
  assertAll(()->assertEquals("Rojo",despues.color()),()->assertTrue(despues.controlVersion()>antes.controlVersion()),()->assertEquals(1,auditorias.findByVehiculoIdOrderById(vehiculoId).size()),()->assertEquals("Azul",auditorias.findByVehiculoIdOrderById(vehiculoId).getFirst().valorAnterior),()->assertEquals("Rojo",auditorias.findByVehiculoIdOrderById(vehiculoId).getFirst().valorNuevo));
  vehiculos.actualizar(vehiculoId,new VehiculoDtos.Actualizacion(despues.vin(),despues.placa(),despues.marca(),despues.modelo(),despues.anio(),despues.version(),despues.color(),despues.kilometraje(),despues.controlVersion()),recepcion);
  assertEquals(1,auditorias.findByVehiculoIdOrderById(vehiculoId).size());
 }

 /** Una versión enviada tras otra edición produce conflicto y conserva el dato ya escrito. */
 @Test void rechazaVersionDesactualizada() throws Exception {
  Long clienteId=registrarCliente(); Long id=vehiculos.registrar(new VehiculoDtos.Registro(clienteId,"1HGCM82633A004352","ABC-123","Honda","Civic",2025,"Touring","Azul",1200L),recepcion).id();
  VehiculoDtos.Detalle ficha=vehiculos.detalle(id,recepcion);
  vehiculos.actualizar(id,new VehiculoDtos.Actualizacion(ficha.vin(),ficha.placa(),ficha.marca(),ficha.modelo(),ficha.anio(),ficha.version(),"Rojo",ficha.kilometraje(),ficha.controlVersion()),recepcion);
  ResponseStatusException error=assertThrows(ResponseStatusException.class,()->vehiculos.actualizar(id,new VehiculoDtos.Actualizacion(ficha.vin(),ficha.placa(),ficha.marca(),ficha.modelo(),ficha.anio(),ficha.version(),"Verde",ficha.kilometraje(),ficha.controlVersion()),recepcion));
  assertEquals(409,error.getStatusCode().value()); assertEquals("Rojo",vehiculos.detalle(id,recepcion).color());
 }

 /** Registra un cliente activo mediante su caso de uso para conservar todas sus restricciones. @return identificador técnico del propietario. */
 private Long registrarCliente() throws Exception {LocalDate nacimiento=LocalDate.of(1988,3,14);var registro=new ClienteDtos.Registro("María Actualización","Contacto Actualización",Period.between(nacimiento,LocalDate.now()).getYears(),nacimiento,"5512345680","5512345681","maria.actualizacion@taller.local","",new ClienteDtos.Direccion("Avenida Central 10","Del Valle Centro","Benito Juárez","Ciudad de México","03100"),taller.id);return clientes.registrar(registro,foto(),"cliente-actualizacion",administrador).id();}
 /** Persiste un taller mínimo válido. @param nombre identificación del taller. @param llave clave idempotente. @return taller persistido. */
 private Taller guardarTaller(String nombre,String llave){Taller creado=new Taller();creado.nombre=nombre;creado.nombreNormalizado=nombre.toUpperCase();creado.razonSocial=nombre+" Servicios";creado.razonSocialNormalizada=creado.razonSocial.toUpperCase();creado.rfc=llave.endsWith("ajeno")?"TAA260101AB1":"TAC260101AB1";creado.telefono=llave.endsWith("ajeno")?"5512345683":"5512345678";creado.email=llave+"@taller.local";creado.calle="Avenida Central 100";creado.colonia="Del Valle Centro";creado.municipio="Benito Juárez";creado.estado="Ciudad de México";creado.codigoPostal="03100";creado.idempotencyKey=llave;return talleres.save(creado);}
 /** Crea una identidad activa asociada al taller indicado. @param email correo único. @param rol rol de autorización. @param asignado taller del usuario. @return usuario persistido. */
 private User crearUsuario(String email,RoleName rol,Taller asignado){Role existente=roles.findByNombre(rol).orElseThrow();User usuario=new User();usuario.nombre="Operador Actualización";usuario.email=email;usuario.celular=rol==RoleName.ADMINISTRADOR_SISTEMA?"5512345682":email.contains("ajena")?"5512345683":"5512345679";usuario.passwordHash="hash";usuario.rol=existente;usuario.taller=asignado;usuario.activo=true;usuario.emailVerificado=true;return usuarios.save(usuario);}
 /** Construye autenticación con la autoridad correspondiente al usuario persistido. @param usuario identidad. @param rol autoridad de Spring Security. @return autenticación utilizable por la fachada. */
 private UsernamePasswordAuthenticationToken autenticacion(User usuario,RoleName rol){return new UsernamePasswordAuthenticationToken(usuario.email,"",List.of(new SimpleGrantedAuthority("ROLE_"+rol)));}
 /** Crea una imagen PNG mínima válida. @return archivo multipart aceptado por clientes. */
 private MockMultipartFile foto() throws Exception {BufferedImage imagen=new BufferedImage(2,2,BufferedImage.TYPE_INT_RGB);ByteArrayOutputStream salida=new ByteArrayOutputStream();ImageIO.write(imagen,"png",salida);return new MockMultipartFile("fotografia","cliente.png","image/png",salida.toByteArray());}
}
