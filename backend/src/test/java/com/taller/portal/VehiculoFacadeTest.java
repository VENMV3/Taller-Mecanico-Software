package com.taller.portal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.taller.portal.dto.VehiculoDtos.Registro;
import com.taller.portal.model.*;
import com.taller.portal.repository.*;
import com.taller.portal.service.VehiculoFacade;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.server.ResponseStatusException;

/** Pruebas dirigidas del registro transaccional y del catálogo vPIC local. */
@ExtendWith(MockitoExtension.class) @MockitoSettings(strictness=Strictness.LENIENT) class VehiculoFacadeTest {
 @Mock VehiculoRepository vehiculos; @Mock ClienteRepository clientes; @Mock UserRepository usuarios; @Mock StatusRepository status; @Mock VpicCatalogoRepository vpic;
 VehiculoFacade facade; User recepcion; Cliente cliente; Authentication auth;
 /** Inicializa una recepción activa, su taller y un cliente activo. */ @BeforeEach void preparar(){facade=new VehiculoFacade(vehiculos,clientes,usuarios,status,vpic);Taller taller=new Taller();taller.id=1L;Role rol=new Role();rol.nombre=RoleName.RECEPCIONISTA;recepcion=new User();recepcion.id=7L;recepcion.activo=true;recepcion.rol=rol;recepcion.taller=taller;auth=new UsernamePasswordAuthenticationToken("r@taller.local","x",List.of(new SimpleGrantedAuthority("ROLE_RECEPCIONISTA")));cliente=new Cliente();cliente.id=2L;cliente.taller=taller;cliente.status=new StatusCatalog();cliente.status.strValor=StatusValues.ACTIVO;when(usuarios.findByEmail("r@taller.local")).thenReturn(Optional.of(recepcion));when(clientes.findById(2L)).thenReturn(Optional.of(cliente));StatusCatalog activo=new StatusCatalog();activo.id=1L;activo.strValor=StatusValues.ACTIVO;when(status.findByStrValorIgnoreCase(StatusValues.ACTIVO)).thenReturn(Optional.of(activo));}
 /** Crea un vehículo válido, normaliza VIN/placa y conserva estado ACT. */ @Test void registraYNormaliza(){var r=facade.registrar(datos("1hgcm82633a004352"," AB-123 "),auth);assertEquals("Vehículo Activo",r.mensaje());verify(vehiculos).saveAndFlush(argThat(v->v.vin.equals("1HGCM82633A004352")&&v.placa.equals("ab-123")&&StatusValues.ACTIVO.equals(v.status.strValor)));}
 /** Rechaza VIN de longitud incorrecta y letras prohibidas antes de guardar. */ @Test void rechazaVinInvalido(){assertThrows(IllegalArgumentException.class,()->facade.registrar(datos("IO123","ABC-12"),auth));verifyNoInteractions(vehiculos);}
 /** Convierte una placa repetida, aun con capitalización distinta, en conflicto 409. */ @Test void rechazaPlacaDuplicada(){when(vehiculos.existsByPlaca("abc-12")).thenReturn(true);var e=assertThrows(ResponseStatusException.class,()->facade.registrar(datos("1HGCM82633A004352","ABC-12"),auth));assertEquals(409,e.getStatusCode().value());}
 /** Impide registrar vehículos para clientes suspendidos. */ @Test void rechazaClienteSuspendido(){cliente.isDeleted=true;var e=assertThrows(ResponseStatusException.class,()->facade.registrar(datos("1HGCM82633A004352","ABC-12"),auth));assertEquals(409,e.getStatusCode().value());}
 /** Impide a recepción asociar clientes de otro taller. */ @Test void rechazaClienteDeOtroTaller(){cliente.taller=new Taller();cliente.taller.id=8L;assertThrows(AccessDeniedException.class,()->facade.registrar(datos("1HGCM82633A004352","ABC-12"),auth));}
 /** Rechaza usuario inactivo y roles sin autorización. */ @Test void rechazaUsuarioInactivoORolNoPermitido(){recepcion.activo=false;assertThrows(AccessDeniedException.class,()->facade.marcas("NIS",auth));recepcion.activo=true;recepcion.rol.nombre=RoleName.MECANICO;assertThrows(AccessDeniedException.class,()->facade.marcas("NIS",auth));}
 /** Devuelve sugerencias locales por prefijo, sin red ni catálogos externos. */ @Test void consultaCatalogoLocal(){VpicMarca marca=new VpicMarca();marca.id=4L;marca.vpicMakeId=55L;marca.nombre="NISSAN";when(vpic.buscarMarcas("NIS")).thenReturn(List.of(marca));assertEquals("NISSAN",facade.marcas("nis",auth).get(0).nombre());}
 /** Construye una solicitud manual válida para los casos de alta. */ private Registro datos(String vin,String placa){return new Registro(2L,vin,placa,"Nissan","Sentra",2025,"SR","Azul",null);}
}
