package com.taller.portal.service;

import com.taller.portal.dto.VehiculoDtos.*;
import com.taller.portal.model.*;
import com.taller.portal.repository.*;
import java.time.Year;
import java.util.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Fachada única del caso de uso vehículo: autoriza, normaliza, valida y registra atómicamente. */
@Service public class VehiculoFacade {
 private static final long KILOMETRAJE_MAXIMO=3_000_000L;
 private final VehiculoRepository vehiculos; private final ClienteRepository clientes; private final UserRepository usuarios; private final StatusRepository status; private final VpicCatalogoRepository vpic;
 /** @param vehiculos repositorio de persistencia @param clientes expediente y aislamiento @param usuarios identidad activa @param status estado ACT @param vpic catálogo local. */
 public VehiculoFacade(VehiculoRepository vehiculos,ClienteRepository clientes,UserRepository usuarios,StatusRepository status,VpicCatalogoRepository vpic){this.vehiculos=vehiculos;this.clientes=clientes;this.usuarios=usuarios;this.status=status;this.vpic=vpic;}
 /** Busca hasta veinte marcas locales por prefijo. @param prefijo texto del usuario @param a identidad autenticada @return marcas canónicas. */
 @Transactional(readOnly=true) public List<Marca> marcas(String prefijo,Authentication a){autorizar(a);return vpic.buscarMarcas(prefijoCatalogo(prefijo)).stream().map(m->new Marca(m.id,m.vpicMakeId,m.nombre)).toList();}
 /** Busca hasta veinte modelos locales de una marca. @param marcaId marca local @param prefijo texto del usuario @param a identidad autenticada @return modelos canónicos. */
 @Transactional(readOnly=true) public List<Modelo> modelos(Long marcaId,String prefijo,Authentication a){autorizar(a);if(marcaId==null)throw new IllegalArgumentException("Marca inválida");return vpic.buscarModelos(marcaId,prefijoCatalogo(prefijo)).stream().map(m->new Modelo(m.id,m.vpicModelId,m.nombre)).toList();}
 /** Busca clientes activos del taller permitido para el selector de propietario. @param texto prefijo o texto parcial @param a identidad autenticada @return hasta veinte opciones. */
 @Transactional(readOnly=true) public List<ClienteOpcion> clientes(String texto,Authentication a){User u=autorizar(a);if(u.taller==null)throw new AccessDeniedException("Usuario sin taller asignado");String patron="%"+(texto==null?"":texto.trim().toLowerCase(Locale.ROOT))+"%";return clientes.activosParaVehiculo(u.taller.id,patron,PageRequest.of(0,20)).stream().map(c->new ClienteOpcion(c.id,c.nombreCompleto)).toList();}
 /** Registra un vehículo activo en una única transacción. @param d datos solicitados @param a identidad autenticada @return vehículo creado con mensaje Vehículo Activo. @throws ResponseStatusException 409 ante VIN o placa duplicados. */
 @Transactional public Respuesta registrar(Registro d,Authentication a){
  User usuario=autorizar(a); String vin=normalizarVin(d.vin()),placa=normalizarPlaca(d.placa()); validar(d,vin,placa);
  Cliente cliente=clientes.findById(d.clienteId()).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Cliente no encontrado"));
  if(cliente.isDeleted||!StatusValues.ACTIVO.equals(cliente.status.strValor))throw new ResponseStatusException(HttpStatus.CONFLICT,"El cliente no está activo");
  if(esRecepcion(usuario)&& (usuario.taller==null||!Objects.equals(usuario.taller.id,cliente.taller.id)))throw new AccessDeniedException("Cliente fuera del taller permitido");
  if(vehiculos.existsByVin(vin))throw conflicto("VIN"); if(vehiculos.existsByPlaca(placa))throw conflicto("placa");
  String marca=canonico(d.marca()),modelo=canonico(d.modelo()); var marcaLocal=vpic.findByNombre(marca); if(marcaLocal.isPresent()){marca=marcaLocal.get().nombre; modelo=vpic.findByMarcaIdAndNombre(marcaLocal.get().id,modelo).map(x->x.nombre).orElse(modelo);}
  Vehiculo v=new Vehiculo();v.cliente=cliente;v.vin=vin;v.placa=placa;v.marca=marca;v.modelo=modelo;v.anio=d.anio();v.version=recortar(d.version(),120,"Versión");v.color=recortar(d.color(),80,"Color");v.kilometraje=d.kilometraje();v.status=status.findByStrValorIgnoreCase(StatusValues.ACTIVO).orElseThrow(()->new IllegalStateException("Falta Status ACT"));v.creadoPor=usuario;
  try{vehiculos.saveAndFlush(v);}catch(DataIntegrityViolationException e){throw conflicto("VIN o placa");} return new Respuesta(v.id,"Vehículo Activo",StatusValues.ACTIVO);
 }
 /** Valida reglas de formato y rangos previas a las consultas de duplicado. */
 private void validar(Registro d,String vin,String placa){if(!vin.matches("^[A-HJ-NPR-Z0-9]{17}$"))throw new IllegalArgumentException("VIN inválido");if(!placa.matches("^[a-z0-9-]{5,10}$"))throw new IllegalArgumentException("Placa inválida");if(d.anio()==null||d.anio()<1950||d.anio()>Year.now().getValue()+1)throw new IllegalArgumentException("Año inválido");if(d.kilometraje()!=null&&(d.kilometraje()<0||d.kilometraje()>KILOMETRAJE_MAXIMO))throw new IllegalArgumentException("Kilometraje inválido (máximo 3000000)");recortar(d.marca(),120,"Marca");recortar(d.modelo(),120,"Modelo");}
 /** Autoriza únicamente recepción/secretaría y desarrollador con usuario activo. */
 private User autorizar(Authentication a){if(a==null||!a.isAuthenticated())throw new AccessDeniedException("Acceso denegado");User u=usuarios.findByEmail(a.getName()).orElseThrow(()->new AccessDeniedException("Usuario no encontrado"));if(!u.activo)throw new AccessDeniedException("Usuario inactivo");if(!esPermitido(u))throw new AccessDeniedException("Rol sin permiso para registrar vehículos");return u;}
 private boolean esPermitido(User u){return u.rol!=null&&(u.rol.nombre==RoleName.RECEPCIONISTA||u.rol.nombre==RoleName.SECRETARIA||u.rol.nombre==RoleName.DESARROLLADOR);}
 private boolean esRecepcion(User u){return u.rol.nombre==RoleName.RECEPCIONISTA||u.rol.nombre==RoleName.SECRETARIA;}
 private String normalizarVin(String valor){return valor==null?"":valor.trim().toUpperCase(Locale.ROOT);}
 private String normalizarPlaca(String valor){return valor==null?"":valor.trim().replaceAll("\\s+","").toLowerCase(Locale.ROOT);}
 private String canonico(String valor){return recortar(valor,120,"Dato").replaceAll("\\s+"," ").toUpperCase(Locale.ROOT);}
 /** Normaliza un prefijo opcional sin convertir una búsqueda vacía en error. */ private String prefijoCatalogo(String valor){return valor==null?"":valor.trim().replaceAll("\\s+"," ").toUpperCase(Locale.ROOT);}
 private String recortar(String valor,int maximo,String campo){if(valor==null||valor.trim().isEmpty()||valor.trim().length()>maximo)throw new IllegalArgumentException(campo+" inválido");return valor.trim();}
 private ResponseStatusException conflicto(String dato){return new ResponseStatusException(HttpStatus.CONFLICT,"Ya existe un vehículo con ese "+dato);}
}
