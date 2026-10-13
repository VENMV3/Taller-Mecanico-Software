package com.taller.portal.dto;

import jakarta.validation.constraints.*;

/** Contratos HTTP del registro y autocompletado local de vehículos. */
public final class VehiculoDtos {
 private VehiculoDtos() {}
 /** Datos recibidos para alta; los valores se normalizan en la fachada antes de persistir. */
 public record Registro(@NotNull Long clienteId,@NotBlank @Size(max=17) String vin,@NotBlank @Size(max=20) String placa,@NotBlank @Size(max=120) String marca,@NotBlank @Size(max=120) String modelo,@NotNull Integer anio,@NotBlank @Size(max=120) String version,@NotBlank @Size(max=80) String color,Long kilometraje) {}
 /** Resultado de una alta exitosa. */ public record Respuesta(Long id,String mensaje,String estatus) {}
 /** Marca canónica expuesta para selección local. */ public record Marca(Long id,Long vpicMakeId,String nombre) {}
 /** Modelo canónico expuesto para selección local. */ public record Modelo(Long id,Long vpicModelId,String nombre) {}
 /** Cliente activo expuesto para el selector de propietario. */ public record ClienteOpcion(Long id,String nombreCompleto) {}
}
