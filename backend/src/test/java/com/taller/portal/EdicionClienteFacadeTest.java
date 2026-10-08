package com.taller.portal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.taller.portal.dto.AdministracionClienteDtos;
import com.taller.portal.dto.ClienteDtos;
import com.taller.portal.model.*;
import com.taller.portal.repository.*;
import com.taller.portal.service.ClienteRegistrationFacade;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.time.LocalDate;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.server.ResponseStatusException;

/** Prueba reglas de edición, aislamiento por taller, validación y duplicados. */
@ExtendWith(MockitoExtension.class)
class EdicionClienteFacadeTest {
  @Mock ClienteRepository clientes; @Mock TallerRepository talleres; @Mock UserRepository usuarios; @Mock StatusRepository status;
  ClienteRegistrationFacade facade; Taller tallerUno; Taller tallerDos; Authentication admin; Authentication recepcion;
  /** Inicializa entidades y permisos usados por cada caso. @return void. */
  @BeforeEach void preparar(){Validator validator=Validation.buildDefaultValidatorFactory().getValidator();facade=new ClienteRegistrationFacade(clientes,talleres,usuarios,status,validator);tallerUno=taller(1L,"Norte");tallerDos=taller(2L,"Sur");admin=auth("admin@taller.local",RoleName.ADMINISTRADOR_SISTEMA);recepcion=auth("recepcion@taller.local",RoleName.RECEPCIONISTA);}
  /** Crea identidad con un rol del dominio. @param email Usuario. @param rol Rol permitido. @returns Autenticación. */
  private Authentication auth(String email,RoleName rol){return new UsernamePasswordAuthenticationToken(email,"",List.of(new SimpleGrantedAuthority("ROLE_"+rol)));}
  /** Crea taller mínimo. @param id Identificador. @param nombre Nombre. @returns Taller. */
  private Taller taller(Long id,String nombre){Taller taller=new Taller();taller.id=id;taller.nombre=nombre;return taller;}
  /** Crea cliente activo persistido. @param id Identificador. @param taller Alcance. @returns Cliente editable. */
  private Cliente cliente(Long id,Taller taller){Cliente c=new Cliente();c.id=id;c.taller=taller;c.status=new StatusCatalog();c.status.strValor=StatusValues.ACTIVO;c.nombreCompleto="Ana Pérez";c.contactoAlternativo="Luis Pérez";c.edad=30;c.fechaNacimiento=LocalDate.now().minusYears(30);c.telefonoPersonal="5512345678";c.telefonoTrabajo="5587654321";c.email="ana@correo.mx";c.calle="Calle Uno";c.colonia="Centro";c.municipio="Monterrey";c.estado="Nuevo León";c.codigoPostal="64000";return c;}
  /** Genera datos de edición válidos con taller opcional. @param tallerId Destino. @returns Datos listos para editar. */
  private AdministracionClienteDtos.Edicion datos(Long tallerId){LocalDate fecha=LocalDate.now().minusYears(30);return new AdministracionClienteDtos.Edicion("Ana Pérez","Luis Pérez",30,fecha,"5512345678","5587654321","ana@correo.mx",null,new ClienteDtos.Direccion("Calle Uno","Centro","Monterrey","Nuevo León","64000"),tallerId);}
  /** Configura las consultas de duplicado como negativas. @return void. */
  private void sinDuplicados(){when(clientes.existsByIdNotAndEmail(anyLong(),anyString())).thenReturn(false);when(clientes.existsByIdNotAndTelefonoPersonal(anyLong(),anyString())).thenReturn(false);when(clientes.existsByIdNotAndTelefonoTrabajo(anyLong(),anyString())).thenReturn(false);when(clientes.existsByIdNotAndNombreCompletoAndFechaNacimiento(anyLong(),anyString(),any())).thenReturn(false);}
  /** Permite actualización válida de Administración y devuelve el cliente normalizado. @return void. */
  @Test void administradorEditaCliente(){Cliente c=cliente(8L,tallerUno);when(talleres.findById(1L)).thenReturn(Optional.of(tallerUno));when(clientes.entidadPorTaller(8L,1L)).thenReturn(Optional.of(c));sinDuplicados();when(clientes.saveAndFlush(c)).thenReturn(c);var respuesta=facade.editar(8L,datos(1L),1L,admin);assertEquals("Cliente actualizado",respuesta.mensaje());assertEquals(8L,respuesta.id());}
  /** Permite a Recepción editar solo su taller y omite el parámetro de alcance manipulado. @return void. */
  @Test void recepcionEditaSoloSuTaller(){Cliente c=cliente(8L,tallerUno);User usuario=new User();usuario.taller=tallerUno;when(usuarios.findByEmail("recepcion@taller.local")).thenReturn(Optional.of(usuario));when(clientes.entidadPorTaller(8L,1L)).thenReturn(Optional.of(c));sinDuplicados();when(clientes.saveAndFlush(c)).thenReturn(c);var respuesta=facade.editar(8L,datos(null),99L,recepcion);assertEquals(1L,respuesta.tallerId());}
  /** Bloquea que Recepción solicite traslado a otro taller. @return void. */
  @Test void recepcionNoPuedeCambiarTaller(){Cliente c=cliente(8L,tallerUno);User usuario=new User();usuario.taller=tallerUno;when(usuarios.findByEmail("recepcion@taller.local")).thenReturn(Optional.of(usuario));when(clientes.entidadPorTaller(8L,1L)).thenReturn(Optional.of(c));assertThrows(AccessDeniedException.class,()->facade.editar(8L,datos(2L),1L,recepcion));}
  /** Permite a Administración cambiar a taller existente. @return void. */
  @Test void administradorCambiaTaller(){Cliente c=cliente(8L,tallerUno);when(talleres.findById(1L)).thenReturn(Optional.of(tallerUno));when(talleres.findById(2L)).thenReturn(Optional.of(tallerDos));when(clientes.entidadPorTaller(8L,1L)).thenReturn(Optional.of(c));sinDuplicados();when(clientes.saveAndFlush(c)).thenReturn(c);facade.editar(8L,datos(2L),1L,admin);assertSame(tallerDos,c.taller);}
  /** Rechaza el traslado administrativo si el taller destino no existe. @return void. */
  @Test void administradorNoPuedeCambiarATallerInexistente(){Cliente c=cliente(8L,tallerUno);when(talleres.findById(1L)).thenReturn(Optional.of(tallerUno));when(talleres.findById(2L)).thenReturn(Optional.empty());when(clientes.entidadPorTaller(8L,1L)).thenReturn(Optional.of(c));assertThrows(IllegalArgumentException.class,()->facade.editar(8L,datos(2L),1L,admin));}
  /** Rechaza formatos Bean Validation y edad incoherente antes de persistir. @return void. */
  @Test void rechazaValidacionYEdadIncoherente(){Cliente c=cliente(8L,tallerUno);when(talleres.findById(1L)).thenReturn(Optional.of(tallerUno));when(clientes.entidadPorTaller(8L,1L)).thenReturn(Optional.of(c));var invalido=new AdministracionClienteDtos.Edicion("Ana","Luis",30,LocalDate.now().minusYears(20),"abc","5587654321","mal",null,new ClienteDtos.Direccion("x","x","x","x","12"),1L);assertThrows(IllegalArgumentException.class,()->facade.editar(8L,invalido,1L,admin));}
  /** Detecta incluso correo de expediente suspendido y devuelve conflicto. @return void. */
  @Test void duplicadoSuspendidoDevuelve409(){Cliente c=cliente(8L,tallerUno);when(talleres.findById(1L)).thenReturn(Optional.of(tallerUno));when(clientes.entidadPorTaller(8L,1L)).thenReturn(Optional.of(c));when(clientes.existsByIdNotAndEmail(8L,"ana@correo.mx")).thenReturn(true);var error=assertThrows(ResponseStatusException.class,()->facade.editar(8L,datos(1L),1L,admin));assertEquals(HttpStatus.CONFLICT,error.getStatusCode());}
  /** No marca como duplicado al mismo cliente cuando todas las comprobaciones son falsas. @return void. */
  @Test void editarSinCambiosNoEsDuplicado(){Cliente c=cliente(8L,tallerUno);when(talleres.findById(1L)).thenReturn(Optional.of(tallerUno));when(clientes.entidadPorTaller(8L,1L)).thenReturn(Optional.of(c));sinDuplicados();when(clientes.saveAndFlush(c)).thenReturn(c);assertDoesNotThrow(()->facade.editar(8L,datos(1L),1L,admin));}
  /** Trata cliente suspendido e inexistente como 404 y rechaza roles no permitidos. @return void. */
  @Test void bloqueaSuspendidoInexistenteYRolNoPermitido(){Cliente suspendido=cliente(8L,tallerUno);suspendido.isDeleted=true;when(talleres.findById(1L)).thenReturn(Optional.of(tallerUno));when(clientes.entidadPorTaller(8L,1L)).thenReturn(Optional.of(suspendido));assertEquals(404,assertThrows(ResponseStatusException.class,()->facade.editar(8L,datos(1L),1L,admin)).getStatusCode().value());Authentication mecanico=auth("mecanico@taller.local",RoleName.MECANICO);assertThrows(AccessDeniedException.class,()->facade.editar(8L,datos(1L),1L,mecanico));}
  /** Devuelve 404 cuando no existe un cliente activo en el taller indicado. @return void. */
  @Test void clienteInexistenteDevuelve404(){when(talleres.findById(1L)).thenReturn(Optional.of(tallerUno));when(clientes.entidadPorTaller(99L,1L)).thenReturn(Optional.empty());assertEquals(404,assertThrows(ResponseStatusException.class,()->facade.editar(99L,datos(1L),1L,admin)).getStatusCode().value());}
}
