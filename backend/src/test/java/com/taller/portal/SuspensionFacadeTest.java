package com.taller.portal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.taller.portal.dto.AdministracionClienteDtos;
import com.taller.portal.model.*;
import com.taller.portal.repository.*;
import com.taller.portal.service.ClienteRegistrationFacade;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.server.ResponseStatusException;
import jakarta.validation.Validator;

/** Prueba la autorización, paginación y condición atómica del caso de suspensión. */
@ExtendWith(MockitoExtension.class)
class SuspensionFacadeTest {
  @Mock ClienteRepository clientes; @Mock TallerRepository talleres; @Mock UserRepository usuarios; @Mock StatusRepository status; @Mock Validator validator;
  ClienteRegistrationFacade facade; Taller taller; Authentication admin;

  /** Inicializa la fachada aislada y una identidad administradora. @return void. */
  @BeforeEach void preparar(){facade=new ClienteRegistrationFacade(clientes,talleres,usuarios,status,validator);taller=new Taller();taller.id=1L;admin=new UsernamePasswordAuthenticationToken("admin@taller.local","",List.of(new SimpleGrantedAuthority("ROLE_ADMINISTRADOR_SISTEMA")));}
  /** Configura el taller requerido por las operaciones permitidas. @return void. */
  private void permitirTaller(){when(talleres.findById(1L)).thenReturn(Optional.of(taller));}
  /** Impone tamaño máximo diez y conserva la consulta proyectada. @return void. */
  @Test void limitaPaginaDeSuspensionADiez(){permitirTaller();when(clientes.consultarSuspensionPorTaller(eq(1L),anyString(),any())).thenReturn(new PageImpl<>(List.of(),PageRequest.of(0,10),0));var pagina=facade.consultarSuspension(0,99,"",1L,"nombreCompleto","asc",admin);assertEquals(10,pagina.tamanio());verify(clientes).consultarSuspensionPorTaller(eq(1L),anyString(),argThat(p->p.getPageSize()==10));}
  /** Rechaza columnas no permitidas antes de consultar datos. @return void. */
  @Test void rechazaOrdenNoPermitido(){permitirTaller();assertThrows(IllegalArgumentException.class,()->facade.consultarSuspension(0,10,"",1L,"email;drop","asc",admin));verifyNoInteractions(clientes);}
  /** Niega el listado a una identidad no administradora. @return void. */
  @Test void niegaSuspensionANoAdministrador(){Authentication recepcion=new UsernamePasswordAuthenticationToken("recepcion@taller.local","",List.of(new SimpleGrantedAuthority("ROLE_RECEPCIONISTA")));assertThrows(AccessDeniedException.class,()->facade.consultarSuspension(0,10,"",1L,"id","asc",recepcion));}
  /** Convierte cero filas del UPDATE condicional en respuesta idempotente de suspensión previa. @return void. */
  @Test void updateAtomicoSinFilasInformaSuspensionPrevia(){permitirTaller();Cliente cliente=new Cliente();cliente.id=2L;cliente.isDeleted=false;cliente.status=new StatusCatalog();cliente.status.strValor=StatusValues.ACTIVO;StatusCatalog suspendido=new StatusCatalog();suspendido.id=3L;suspendido.strValor=StatusValues.SUSPENDIDO;User usuario=new User();usuario.id=7L;when(clientes.entidadPorTaller(2L,1L)).thenReturn(Optional.of(cliente));when(status.findByStrValorIgnoreCase(StatusValues.SUSPENDIDO)).thenReturn(Optional.of(suspendido));when(usuarios.findByEmail("admin@taller.local")).thenReturn(Optional.of(usuario));when(clientes.suspenderAtomico(eq(2L),eq(1L),eq(3L),eq(7L))).thenReturn(0);var respuesta=facade.suspender(2L,1L,admin);assertEquals("Cliente ya estaba suspendido",respuesta.mensaje());}
  /** Devuelve 404 cuando el expediente no pertenece al taller o no existe. @return void. */
  @Test void clienteInexistenteDevuelve404(){permitirTaller();when(clientes.entidadPorTaller(99L,1L)).thenReturn(Optional.empty());var error=assertThrows(ResponseStatusException.class,()->facade.suspender(99L,1L,admin));assertEquals(404,error.getStatusCode().value());}
  /** Solicita el UPDATE atómico para conservar el registro y marcarlo como suspendido. @return void. */
  @Test void suspensionExitosaEjecutaUpdateAtomico(){permitirTaller();Cliente cliente=new Cliente();cliente.id=2L;cliente.status=new StatusCatalog();cliente.status.strValor=StatusValues.ACTIVO;StatusCatalog suspendido=new StatusCatalog();suspendido.id=3L;User usuario=new User();usuario.id=7L;when(clientes.entidadPorTaller(2L,1L)).thenReturn(Optional.of(cliente));when(status.findByStrValorIgnoreCase(StatusValues.SUSPENDIDO)).thenReturn(Optional.of(suspendido));when(usuarios.findByEmail("admin@taller.local")).thenReturn(Optional.of(usuario));when(clientes.suspenderAtomico(eq(2L),eq(1L),eq(3L),eq(7L))).thenReturn(1);var respuesta=facade.suspender(2L,1L,admin);assertEquals("Cliente suspendido",respuesta.mensaje());verify(clientes).suspenderAtomico(2L,1L,3L,7L);}
}
