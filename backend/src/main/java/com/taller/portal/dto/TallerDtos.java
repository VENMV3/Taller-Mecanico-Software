package com.taller.portal.dto;
import jakarta.validation.Valid; import jakarta.validation.constraints.*; import java.util.List;
/** Contratos HTTP del alta y consulta de talleres. */
public final class TallerDtos { private TallerDtos(){}
 /** Datos multipart de un taller; RFC se normaliza y valida en la fachada. */
 public record Registro(@NotBlank @Size(max=120) String nombre,@NotBlank @Size(max=160) String razonSocial,@NotBlank @Size(max=20) String rfc,@NotBlank @Pattern(regexp="^[0-9]{10}$") String telefono,@NotBlank @Email @Size(max=140) String email,@NotNull @Valid ClienteDtos.Direccion direccion){}
 /** Fila segura para selectores y listado. */
 public record Resumen(Long id,String nombre,String razonSocial,String rfc,String municipio){}
 /** Página limitada a diez registros en la fachada. */
 public record Pagina(List<Resumen> talleres,int pagina,int tamanio,long total,int paginas){}
 /** Resultado del alta idempotente. */
 public record Respuesta(Long id,String mensaje){}
}
