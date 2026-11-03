package com.taller.portal;

import static org.junit.jupiter.api.Assertions.*;

import com.taller.portal.dto.ClienteDtos;
import com.taller.portal.dto.VehiculoDtos;
import com.taller.portal.model.Role;
import com.taller.portal.model.RoleName;
import com.taller.portal.model.Taller;
import com.taller.portal.model.User;
import com.taller.portal.repository.ClienteRepository;
import com.taller.portal.repository.RoleRepository;
import com.taller.portal.repository.TallerRepository;
import com.taller.portal.repository.UserRepository;
import com.taller.portal.repository.VehiculoRepository;
import com.taller.portal.service.ClienteRegistrationFacade;
import com.taller.portal.service.VehiculoFacade;
import com.taller.portal.service.VehiculoNormalizador;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.Locale;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.AfterEach;
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

/** Verifica con H2 la normalización canónica y los conflictos de vehículos. */
@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:vehiculonormalizacion;MODE=MySQL;DB_CLOSE_DELAY=-1","spring.datasource.driver-class-name=org.h2.Driver","spring.datasource.username=sa","spring.datasource.password="})
@Transactional @Rollback
class VehiculoNormalizacionTest {
 @Autowired VehiculoNormalizador normalizador;
 @Autowired VehiculoFacade vehiculos;
 @Autowired ClienteRegistrationFacade clientes;
 @Autowired VehiculoRepository vehiculoRepo;
 @Autowired TallerRepository tallerRepo;
 @Autowired UserRepository usuarioRepo;
 @Autowired RoleRepository rolRepo;
 @Autowired ClienteRepository clienteRepo;
 @Autowired JdbcTemplate jdbc;
 Taller taller;
 UsernamePasswordAuthenticationToken recepcion;
 UsernamePasswordAuthenticationToken administrador;
 Locale localeOriginal;

 /** Prepara un taller, una recepción activa y el asentamiento requerido por el alta real de cliente. */
 @BeforeEach void preparar() throws Exception {
  localeOriginal=Locale.getDefault();
  jdbc.update("insert into sepomex_asentamientos(codigo_postal,colonia,tipo_asentamiento,municipio,estado,c_estado,c_mnpio) values('03100','Del Valle Centro','Colonia','Benito Juárez','Ciudad de México','09','014')");
  taller=new Taller();taller.nombre="Taller Normalización";taller.nombreNormalizado="TALLER NORMALIZACIÓN";taller.razonSocial="Taller Normalización Servicios";taller.razonSocialNormalizada="TALLER NORMALIZACIÓN SERVICIOS";taller.rfc="TNO260101AB1";taller.telefono="5512345678";taller.email="normalizacion@taller.local";taller.calle="Avenida Central 100";taller.colonia="Del Valle Centro";taller.municipio="Benito Juárez";taller.estado="Ciudad de México";taller.codigoPostal="03100";taller.idempotencyKey="vehiculo-normalizacion";taller=tallerRepo.save(taller);
  Role rol=rolRepo.findByNombre(RoleName.RECEPCIONISTA).orElseThrow();
  User usuario=new User();usuario.nombre="Recepción Normalización";usuario.email="recepcion.normalizacion@taller.local";usuario.celular="5512345679";usuario.passwordHash="hash";usuario.rol=rol;usuario.taller=taller;usuario.activo=true;usuario.emailVerificado=true;usuarioRepo.save(usuario);
  recepcion=new UsernamePasswordAuthenticationToken(usuario.email,"",List.of(new SimpleGrantedAuthority("ROLE_RECEPCIONISTA")));
  Role rolAdmin=rolRepo.findByNombre(RoleName.ADMINISTRADOR_SISTEMA).orElseThrow();
  User admin=new User();admin.nombre="Administración Normalización";admin.email="admin.normalizacion@taller.local";admin.celular="5512345682";admin.passwordHash="hash";admin.rol=rolAdmin;admin.taller=taller;admin.activo=true;admin.emailVerificado=true;usuarioRepo.save(admin);
  administrador=new UsernamePasswordAuthenticationToken(admin.email,"",List.of(new SimpleGrantedAuthority("ROLE_ADMINISTRADOR_SISTEMA")));
 }

 /** Restablece el Locale del proceso para aislar la comprobación de mayúsculas estables. */
 @AfterEach void restaurarLocale(){Locale.setDefault(localeOriginal);}

 /** Normaliza VIN y placa con separadores Unicode de forma idempotente y estable ante Locale turco. */
 @Test void normalizaVinYPlacaDeFormaIdempotente(){
  Locale.setDefault(Locale.forLanguageTag("tr-TR"));
  String vin=normalizador.normalizarVin(" 1hg\u2011cm82633a004352\u200B");
  String placa=normalizador.normalizarPlaca(" A\u00A0BC\u2014123 ");
  assertEquals("1HGCM82633A004352",vin);assertEquals("abc123",placa);
  assertEquals(vin,normalizador.normalizarVin(vin));assertEquals(placa,normalizador.normalizarPlaca(placa));
 }

 /** Rechaza VIN y placa nulos, vacíos, solo separadores, caracteres prohibidos y longitudes inválidas. */
 @Test void rechazaIdentificadoresInvalidos(){
  Long clienteId=registrarCliente();
  assertAll(
   ()->assertThrows(IllegalArgumentException.class,()->vehiculos.registrar(datos(clienteId,null,"abc123"),recepcion)),
   ()->assertThrows(IllegalArgumentException.class,()->vehiculos.registrar(datos(clienteId,"","abc123"),recepcion)),
   ()->assertThrows(IllegalArgumentException.class,()->vehiculos.registrar(datos(clienteId,"1HGCM82633A00435I","abc123"),recepcion)),
   ()->assertThrows(IllegalArgumentException.class,()->vehiculos.registrar(datos(clienteId,"1HGCM82633A00435O","abc123"),recepcion)),
   ()->assertThrows(IllegalArgumentException.class,()->vehiculos.registrar(datos(clienteId,"1HGCM82633A00435Q","abc123"),recepcion)),
   ()->assertThrows(IllegalArgumentException.class,()->vehiculos.registrar(datos(clienteId,"1HGCM82633A00435","abc123"),recepcion)),
   ()->assertThrows(IllegalArgumentException.class,()->vehiculos.registrar(datos(clienteId,"1HGCM82633A004352",null),recepcion)),
   ()->assertThrows(IllegalArgumentException.class,()->vehiculos.registrar(datos(clienteId,"1HGCM82633A004352","\u00A0\u2011\u200B"),recepcion)),
   ()->assertThrows(IllegalArgumentException.class,()->vehiculos.registrar(datos(clienteId,"1HGCM82633A004352","abc@123"),recepcion)),
   ()->assertThrows(IllegalArgumentException.class,()->vehiculos.registrar(datos(clienteId,"1HGCM82633A004352","abcd"),recepcion)));
 }

 /** Detecta duplicados por la forma canónica y conserva el id técnico y la relación con el cliente. */
 @Test void registraCanonicoYDetectaDuplicadosPorVinOPlaca() throws Exception {
  Long clienteId=registrarCliente();
  var alta=vehiculos.registrar(datos(clienteId,"1hg\u2011cm82633a004352","abc-123"),recepcion);
  var guardado=vehiculoRepo.findById(alta.id()).orElseThrow();
  assertEquals("1HGCM82633A004352",guardado.vin);assertEquals("abc123",guardado.placa);assertEquals(alta.id(),guardado.id);assertEquals(clienteId,guardado.cliente.id);
  ResponseStatusException vinDuplicado=assertThrows(ResponseStatusException.class,()->vehiculos.registrar(datos(clienteId,"1HG CM82633A004352","xyz987"),recepcion));
  ResponseStatusException placaDuplicada=assertThrows(ResponseStatusException.class,()->vehiculos.registrar(datos(clienteId,"1HGCM82633A004353","ABC 123"),recepcion));
  assertAll(()->assertEquals(409,vinDuplicado.getStatusCode().value()),()->assertTrue(vinDuplicado.getReason().contains("VIN")),()->assertEquals(409,placaDuplicada.getStatusCode().value()),()->assertTrue(placaDuplicada.getReason().contains("placa")),()->assertEquals(1,vehiculoRepo.count()),()->assertTrue(clienteRepo.findById(clienteId).isPresent()));
 }

 /** Registra un cliente activo mediante el caso de uso real para conservar sus restricciones. @return id técnico del cliente. */
 private Long registrarCliente() throws Exception {
  LocalDate nacimiento=LocalDate.of(1988,3,14);
  var registro=new ClienteDtos.Registro("María Normalización","Contacto Normalización",Period.between(nacimiento,LocalDate.now()).getYears(),nacimiento,"5512345680","5512345681","maria.normalizacion@taller.local","",new ClienteDtos.Direccion("Avenida Central 10","Del Valle Centro","Benito Juárez","Ciudad de México","03100"),taller.id);
  return clientes.registrar(registro,foto(),"cliente-normalizacion",administrador).id();
 }

 /** Construye una alta válida de vehículo. @param clienteId propietario activo @param vin VIN a probar @param placa placa a probar @return contrato de alta. */
 private VehiculoDtos.Registro datos(Long clienteId,String vin,String placa){return new VehiculoDtos.Registro(clienteId,vin,placa,"Nissan","Sentra",2025,"SR","Azul",1200L);}

 /** Crea una imagen PNG mínima aceptada por el registro de cliente. @return archivo multipart válido. */
 private MockMultipartFile foto() throws Exception {BufferedImage imagen=new BufferedImage(2,2,BufferedImage.TYPE_INT_RGB);ByteArrayOutputStream salida=new ByteArrayOutputStream();ImageIO.write(imagen,"png",salida);return new MockMultipartFile("fotografia","cliente.png","image/png",salida.toByteArray());}
}
