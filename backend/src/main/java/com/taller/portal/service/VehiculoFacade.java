package com.taller.portal.service;

import com.taller.portal.dto.VehiculoDtos.*;
import com.taller.portal.model.*;
import com.taller.portal.repository.*;
import java.time.Year;
import java.util.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
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
 private final VehiculoRepository vehiculos; private final VehiculoAuditoriaRepository auditorias; private final ClienteRepository clientes; private final UserRepository usuarios; private final StatusRepository status; private final VpicCatalogoRepository vpic; private final VehiculoNormalizador normalizador;
 /** @param vehiculos repositorio de persistencia @param clientes expediente y aislamiento @param usuarios identidad activa @param status estado ACT @param vpic catálogo local @param normalizador forma canónica de VIN y placa. */
 public VehiculoFacade(VehiculoRepository vehiculos,VehiculoAuditoriaRepository auditorias,ClienteRepository clientes,UserRepository usuarios,StatusRepository status,VpicCatalogoRepository vpic,VehiculoNormalizador normalizador){this.vehiculos=vehiculos;this.auditorias=auditorias;this.clientes=clientes;this.usuarios=usuarios;this.status=status;this.vpic=vpic;this.normalizador=normalizador;}
 /** Busca hasta veinte marcas locales por prefijo. @param prefijo texto del usuario @param a identidad autenticada @return marcas canónicas. */
 @Transactional(readOnly=true) public List<Marca> marcas(String prefijo,Authentication a){autorizar(a);return vpic.buscarMarcas(prefijoCatalogo(prefijo)).stream().map(m->new Marca(m.id,m.vpicMakeId,m.nombre)).toList();}
 /** Busca hasta veinte modelos locales de una marca. @param marcaId marca local @param prefijo texto del usuario @param a identidad autenticada @return modelos canónicos. */
 @Transactional(readOnly=true) public List<Modelo> modelos(Long marcaId,String prefijo,Authentication a){autorizar(a);if(marcaId==null)throw new IllegalArgumentException("Marca inválida");return vpic.buscarModelos(marcaId,prefijoCatalogo(prefijo)).stream().map(m->new Modelo(m.id,m.vpicModelId,m.nombre)).toList();}
 /** Busca clientes activos del taller permitido para el selector de propietario. @param texto prefijo o texto parcial @param a identidad autenticada @return hasta veinte opciones. */
 @Transactional(readOnly=true) public List<ClienteOpcion> clientes(String texto,Authentication a){User u=autorizar(a);if(u.taller==null)throw new AccessDeniedException("Usuario sin taller asignado");String patron="%"+(texto==null?"":texto.trim().toLowerCase(Locale.ROOT))+"%";return clientes.activosParaVehiculo(u.taller.id,patron,PageRequest.of(0,20)).stream().map(c->new ClienteOpcion(c.id,c.nombreCompleto)).toList();}
 /** Devuelve la ficha de un vehículo sin exponer contacto de su propietario. @param id vehículo solicitado @param a identidad autenticada @return datos operativos y propietario mínimo. @throws ResponseStatusException 404 si no existe o queda fuera del taller permitido. */
 @Transactional(readOnly=true) public Detalle detalle(Long id,Authentication a){User usuario=autorizar(a);Vehiculo vehiculo=vehiculos.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Vehículo no encontrado"));if(esRecepcion(usuario)&&(usuario.taller==null||!Objects.equals(usuario.taller.id,vehiculo.cliente.taller.id)))throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Vehículo no encontrado");return detalle(vehiculo);}
 /** Actualiza campos permitidos y registra cada diferencia en la misma transacción. @param id vehículo @param d datos y versión leída @param a identidad autenticada @return ficha vigente. */
 @Transactional public Detalle actualizar(Long id,Actualizacion d,Authentication a){
  User usuario=autorizar(a); Vehiculo v=vehiculos.findById(id).orElseThrow(()->noEncontrado());
  if(esRecepcion(usuario)&&(usuario.taller==null||!Objects.equals(usuario.taller.id,v.cliente.taller.id)))throw noEncontrado();
  if(v.cliente.isDeleted||!StatusValues.ACTIVO.equals(v.cliente.status.strValor))throw new ResponseStatusException(HttpStatus.CONFLICT,"El cliente propietario no está activo");
  if(!Objects.equals(v.controlVersion,d.controlVersion()))throw new ResponseStatusException(HttpStatus.CONFLICT,"La ficha fue actualizada por otra persona");
  String vin=normalizador.normalizarVin(d.vin()),placa=normalizador.normalizarPlaca(d.placa()); validar(d,vin,placa); String[] canonicos=canonizar(d.marca(),d.modelo());
  if(iguales(v,vin,placa,canonicos[0],canonicos[1],d))return detalle(v);
  if(vehiculos.existsByIdNotAndVin(id,vin))throw conflicto("VIN"); if(vehiculos.existsByIdNotAndPlaca(id,placa))throw conflicto("placa");
  List<VehiculoAuditoria> cambios=cambios(v,vin,placa,canonicos[0],canonicos[1],d,usuario);
  v.vin=vin;v.placa=placa;v.marca=canonicos[0];v.modelo=canonicos[1];v.anio=d.anio();v.version=recortar(d.version(),120,"Versión");v.color=recortar(d.color(),80,"Color");v.kilometraje=d.kilometraje();
  try{vehiculos.saveAndFlush(v);auditorias.saveAll(cambios);auditorias.flush();}catch(ObjectOptimisticLockingFailureException e){throw new ResponseStatusException(HttpStatus.CONFLICT,"La ficha fue actualizada por otra persona",e);}catch(DataIntegrityViolationException e){throw conflicto("VIN o placa");}
  return detalle(v);
 }
 /** Registra un vehículo activo en una única transacción. @param d datos solicitados @param a identidad autenticada @return vehículo creado con mensaje Vehículo Activo. @throws ResponseStatusException 409 ante VIN o placa duplicados. */
 @Transactional public Respuesta registrar(Registro d,Authentication a){
  User usuario=autorizar(a); String vin=normalizador.normalizarVin(d.vin()),placa=normalizador.normalizarPlaca(d.placa()); validar(d,vin,placa);
  Cliente cliente=clientes.findById(d.clienteId()).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Cliente no encontrado"));
  if(cliente.isDeleted||!StatusValues.ACTIVO.equals(cliente.status.strValor))throw new ResponseStatusException(HttpStatus.CONFLICT,"El cliente no está activo");
  if(esRecepcion(usuario)&& (usuario.taller==null||!Objects.equals(usuario.taller.id,cliente.taller.id)))throw new AccessDeniedException("Cliente fuera del taller permitido");
  if(vehiculos.existsByVin(vin))throw conflicto("VIN"); if(vehiculos.existsByPlaca(placa))throw conflicto("placa");
  String[] canonicos=canonizar(d.marca(),d.modelo()); String marca=canonicos[0],modelo=canonicos[1];
  Vehiculo v=new Vehiculo();v.cliente=cliente;v.vin=vin;v.placa=placa;v.marca=marca;v.modelo=modelo;v.anio=d.anio();v.version=recortar(d.version(),120,"Versión");v.color=recortar(d.color(),80,"Color");v.kilometraje=d.kilometraje();v.status=status.findByStrValorIgnoreCase(StatusValues.ACTIVO).orElseThrow(()->new IllegalStateException("Falta Status ACT"));v.creadoPor=usuario;
  try{vehiculos.saveAndFlush(v);}catch(DataIntegrityViolationException e){throw conflicto("VIN o placa");} return new Respuesta(v.id,"Vehículo Activo",StatusValues.ACTIVO);
 }
 /** Valida reglas de formato y rangos previas a las consultas de duplicado. */
 private void validar(Registro d,String vin,String placa){if(!vin.matches("^[A-HJ-NPR-Z0-9]{17}$"))throw new IllegalArgumentException("VIN inválido: usa 17 caracteres sin I, O ni Q");if(!placa.matches("^[a-z0-9]{5,10}$"))throw new IllegalArgumentException("Placa inválida: usa de 5 a 10 letras o números");if(d.anio()==null||d.anio()<1950||d.anio()>Year.now().getValue()+1)throw new IllegalArgumentException("Año inválido");if(d.kilometraje()!=null&&(d.kilometraje()<0||d.kilometraje()>KILOMETRAJE_MAXIMO))throw new IllegalArgumentException("Kilometraje inválido (máximo 3000000)");recortar(d.marca(),120,"Marca");recortar(d.modelo(),120,"Modelo");}
 /** Reutiliza las reglas de alta para una edición sin propietario. */
 private void validar(Actualizacion d,String vin,String placa){validar(new Registro(0L,d.vin(),d.placa(),d.marca(),d.modelo(),d.anio(),d.version(),d.color(),d.kilometraje()),vin,placa);}
 /** Convierte la entidad visible en ficha sin campos de contacto del cliente. @param vehiculo entidad autorizada. @return contrato de salida. */
 private Detalle detalle(Vehiculo vehiculo){return new Detalle(vehiculo.id,vehiculo.vin,vehiculo.placa,vehiculo.marca,vehiculo.modelo,vehiculo.anio,vehiculo.version,vehiculo.color,vehiculo.kilometraje,vehiculo.status.strValor,vehiculo.cliente.id,vehiculo.cliente.nombreCompleto,vehiculo.controlVersion);}
 private String[] canonizar(String marca,String modelo){String m=canonico(marca),mo=canonico(modelo);var local=vpic.findByNombre(m);if(local.isPresent()){m=local.get().nombre;mo=vpic.findByMarcaIdAndNombre(local.get().id,mo).map(x->x.nombre).orElse(mo);}return new String[]{m,mo};}
 private boolean iguales(Vehiculo v,String vin,String placa,String marca,String modelo,Actualizacion d){return Objects.equals(v.vin,vin)&&Objects.equals(v.placa,placa)&&Objects.equals(v.marca,marca)&&Objects.equals(v.modelo,modelo)&&Objects.equals(v.anio,d.anio())&&Objects.equals(v.version,recortar(d.version(),120,"Versión"))&&Objects.equals(v.color,recortar(d.color(),80,"Color"))&&Objects.equals(v.kilometraje,d.kilometraje());}
 private List<VehiculoAuditoria> cambios(Vehiculo v,String vin,String placa,String marca,String modelo,Actualizacion d,User u){List<VehiculoAuditoria> r=new ArrayList<>();agregar(r,v,u,"vin",v.vin,vin);agregar(r,v,u,"placa",v.placa,placa);agregar(r,v,u,"marca",v.marca,marca);agregar(r,v,u,"modelo",v.modelo,modelo);agregar(r,v,u,"anio",v.anio,d.anio());agregar(r,v,u,"version",v.version,recortar(d.version(),120,"Versión"));agregar(r,v,u,"color",v.color,recortar(d.color(),80,"Color"));agregar(r,v,u,"kilometraje",v.kilometraje,d.kilometraje());return r;}
 private void agregar(List<VehiculoAuditoria> r,Vehiculo v,User u,String campo,Object antes,Object despues){if(Objects.equals(antes,despues))return;VehiculoAuditoria a=new VehiculoAuditoria();a.vehiculo=v;a.usuario=u;a.campo=campo;a.valorAnterior=antes==null?null:String.valueOf(antes);a.valorNuevo=despues==null?null:String.valueOf(despues);r.add(a);}
 private ResponseStatusException noEncontrado(){return new ResponseStatusException(HttpStatus.NOT_FOUND,"Vehículo no encontrado");}
 /** Autoriza únicamente recepción/secretaría y desarrollador con usuario activo. */
 private User autorizar(Authentication a){if(a==null||!a.isAuthenticated())throw new AccessDeniedException("Acceso denegado");User u=usuarios.findByEmail(a.getName()).orElseThrow(()->new AccessDeniedException("Usuario no encontrado"));if(!u.activo)throw new AccessDeniedException("Usuario inactivo");if(!esPermitido(u))throw new AccessDeniedException("Rol sin permiso para registrar vehículos");return u;}
 private boolean esPermitido(User u){return u.rol!=null&&(u.rol.nombre==RoleName.RECEPCIONISTA||u.rol.nombre==RoleName.SECRETARIA||u.rol.nombre==RoleName.DESARROLLADOR);}
 private boolean esRecepcion(User u){return u.rol.nombre==RoleName.RECEPCIONISTA||u.rol.nombre==RoleName.SECRETARIA;}
 private String canonico(String valor){return recortar(valor,120,"Dato").replaceAll("\\s+"," ").toUpperCase(Locale.ROOT);}
 /** Normaliza un prefijo opcional sin convertir una búsqueda vacía en error. */ private String prefijoCatalogo(String valor){return valor==null?"":valor.trim().replaceAll("\\s+"," ").toUpperCase(Locale.ROOT);}
 private String recortar(String valor,int maximo,String campo){if(valor==null||valor.trim().isEmpty()||valor.trim().length()>maximo)throw new IllegalArgumentException(campo+" inválido");return valor.trim();}
 private ResponseStatusException conflicto(String dato){return new ResponseStatusException(HttpStatus.CONFLICT,"Ya existe un vehículo con ese "+dato);}
}
