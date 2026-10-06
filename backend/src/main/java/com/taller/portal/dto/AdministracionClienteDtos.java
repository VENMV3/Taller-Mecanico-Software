package com.taller.portal.dto;
import com.taller.portal.model.EstatusCliente; import jakarta.validation.Valid; import jakarta.validation.constraints.*; import java.time.LocalDate; import java.util.List;
/** Contratos de administración de clientes aislados por taller. */
public final class AdministracionClienteDtos { private AdministracionClienteDtos(){}
 /** Datos editables; tallerId solo tiene efecto para Administrador. */
 public record Edicion(@NotBlank @Size(max=120) String nombreCompleto,@NotBlank @Size(max=120) String contactoAlternativo,@NotNull @Min(0) @Max(130) Integer edad,@NotNull @Past LocalDate fechaNacimiento,@NotBlank @Pattern(regexp="^[0-9+ ()-]{8,25}$") String telefonoPersonal,@NotBlank @Pattern(regexp="^[0-9+ ()-]{8,25}$") String telefonoTrabajo,@NotBlank @Email @Size(max=140) String email,@Email @Size(max=140) String emailTrabajo,@NotNull @Valid ClienteDtos.Direccion direccion,Long tallerId){}
 /** Fila del listado con alcance y estatus. */
 public record Resumen(Long id,String nombreCompleto,String telefonoPersonal,String email,String municipio,Long tallerId,String taller,EstatusCliente estatus){}
 /** Detalle editable sin rutas internas. */
 public record Detalle(Long id,String nombreCompleto,String contactoAlternativo,Integer edad,LocalDate fechaNacimiento,String telefonoPersonal,String telefonoTrabajo,String email,String emailTrabajo,String calle,String colonia,String municipio,String estado,String codigoPostal,Long tallerId,String taller,EstatusCliente estatus){}
 /** Página de administración. */
 public record Pagina(List<Resumen> clientes,int pagina,int tamanio,long total,int paginas){}
 /** Respuesta de edición/suspensión. */
 public record Respuesta(Long id,String mensaje,EstatusCliente estatus){}
}
