package com.taller.portal.dto;

import jakarta.validation.constraints.*;

/** Contratos HTTP del registro y autocompletado local de vehículos. */
public final class VehiculoDtos {
 private VehiculoDtos() {}
 /** Datos recibidos para alta; los límites admiten separadores antes de que la fachada los normalice. */
 public record Registro(@NotNull Long clienteId,@NotBlank @Size(max=68) String vin,@NotBlank @Size(max=40) String placa,@NotBlank @Size(max=120) String marca,@NotBlank @Size(max=120) String modelo,@NotNull Integer anio,@NotBlank @Size(max=120) String version,@NotBlank @Size(max=80) String color,Long kilometraje) {}
 /** Resultado de una alta exitosa. */ public record Respuesta(Long id,String mensaje,String estatus) {}
 /** Ficha operativa de vehículo sin datos de contacto del propietario. */
 public record Detalle(Long id,String vin,String placa,String marca,String modelo,Integer anio,String version,String color,Long kilometraje,String estatus,Long clienteId,String clienteNombre,Long controlVersion) {}
 /** Datos editables de UC-CV-08; no admite propietario ni estatus. */
 public record Actualizacion(@NotBlank @Size(max=68) String vin,@NotBlank @Size(max=40) String placa,@NotBlank @Size(max=120) String marca,@NotBlank @Size(max=120) String modelo,@NotNull Integer anio,@NotBlank @Size(max=120) String version,@NotBlank @Size(max=80) String color,Long kilometraje,@NotNull Long controlVersion) {}
 /** Marca canónica expuesta para selección local. */ public record Marca(Long id,Long vpicMakeId,String nombre) {}
 /** Modelo canónico expuesto para selección local. */ public record Modelo(Long id,Long vpicModelId,String nombre) {}
 /** Cliente activo expuesto para el selector de propietario. */ public record ClienteOpcion(Long id,String nombreCompleto) {}
}
