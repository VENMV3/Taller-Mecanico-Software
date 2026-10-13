package com.taller.portal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import com.taller.portal.model.*;
import com.taller.portal.repository.*;
import com.taller.portal.service.ClienteRegistrationFacade;
import jakarta.validation.Validator;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.server.ResponseStatusException;

/** Cubre autorización, idempotencia y actualización atómica de reactivación. */
@ExtendWith(MockitoExtension.class)
class ReactivacionFacadeTest {
 @Mock ClienteRepository clientes; @Mock TallerRepository talleres; @Mock UserRepository usuarios; @Mock StatusRepository status; @Mock Validator validator;
 ClienteRegistrationFacade facade; Taller taller; Authentication admin;
 /** Inicializa el alcance y la identidad administradora. */ @BeforeEach void preparar(){facade=new ClienteRegistrationFacade(clientes,talleres,usuarios,status,validator);taller=new Taller();taller.id=1L;admin=new UsernamePasswordAuthenticationToken("admin@taller.local","",List.of(new SimpleGrantedAuthority("ROLE_ADMINISTRADOR_SISTEMA")));when(talleres.findById(1L)).thenReturn(Optional.of(taller));}
 /** Reactiva usando condición atómica y el catálogo ACT/SUS. */ @Test void administradorReactivaConUpdateAtomico(){Cliente c=suspendido(2L);StatusCatalog act=status(1L,StatusValues.ACTIVO),sus=status(2L,StatusValues.SUSPENDIDO);when(clientes.entidadPorTaller(2L,1L)).thenReturn(Optional.of(c));when(status.findByStrValorIgnoreCase(StatusValues.ACTIVO)).thenReturn(Optional.of(act));when(status.findByStrValorIgnoreCase(StatusValues.SUSPENDIDO)).thenReturn(Optional.of(sus));when(clientes.reactivarAtomico(2L,1L,1L,2L)).thenReturn(1);var r=facade.reactivar(2L,1L,admin);assertEquals("Cliente reactivado",r.mensaje());assertEquals(StatusValues.ACTIVO,r.estatus());verify(clientes).reactivarAtomico(2L,1L,1L,2L);}
 /** No actualiza dos veces un cliente activo. */ @Test void activoInformaSinActualizar(){Cliente c=suspendido(3L);c.isDeleted=false;c.status.strValor=StatusValues.ACTIVO;when(clientes.entidadPorTaller(3L,1L)).thenReturn(Optional.of(c));assertEquals("Cliente ya estaba activo",facade.reactivar(3L,1L,admin).mensaje());verify(clientes,never()).reactivarAtomico(anyLong(),anyLong(),anyLong(),anyLong());}
 /** Trata una carrera de dos peticiones como operación idempotente. */ @Test void carreraConCeroFilasInformaActivo(){Cliente c=suspendido(4L);StatusCatalog act=status(1L,StatusValues.ACTIVO),sus=status(2L,StatusValues.SUSPENDIDO);when(clientes.entidadPorTaller(4L,1L)).thenReturn(Optional.of(c));when(status.findByStrValorIgnoreCase(StatusValues.ACTIVO)).thenReturn(Optional.of(act));when(status.findByStrValorIgnoreCase(StatusValues.SUSPENDIDO)).thenReturn(Optional.of(sus));when(clientes.reactivarAtomico(4L,1L,1L,2L)).thenReturn(0);assertEquals("Cliente ya estaba activo",facade.reactivar(4L,1L,admin).mensaje());}
 /** Niega reactivación a recepción e informa 404 para identificador inexistente. */ @Test void validaRolYExistencia(){Authentication recepcion=new UsernamePasswordAuthenticationToken("r","",List.of(new SimpleGrantedAuthority("ROLE_RECEPCIONISTA")));assertThrows(AccessDeniedException.class,()->facade.reactivar(2L,1L,recepcion));when(clientes.entidadPorTaller(99L,1L)).thenReturn(Optional.empty());assertEquals(404,assertThrows(ResponseStatusException.class,()->facade.reactivar(99L,1L,admin)).getStatusCode().value());}
 /** Construye cliente suspendido. */ private Cliente suspendido(Long id){Cliente c=new Cliente();c.id=id;c.isDeleted=true;c.status=status(2L,StatusValues.SUSPENDIDO);return c;}
 /** Construye una fila de catálogo. */ private StatusCatalog status(Long id,String valor){StatusCatalog s=new StatusCatalog();s.id=id;s.strValor=valor;return s;}
}
