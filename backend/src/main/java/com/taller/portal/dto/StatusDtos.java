package com.taller.portal.dto;
import jakarta.validation.constraints.*; import java.util.*;
/** Contratos del catálogo de estatus. */ public final class StatusDtos { private StatusDtos(){} public record Entrada(@NotBlank @Pattern(regexp="^[A-Z0-9_]{1,12}$") String strValor,@NotBlank @Size(max=80) String strDescripcion){} public record Fila(Long id,String strValor,String strDescripcion){} public record Pagina(List<Fila> status,int pagina,int tamanio,long total,int paginas){} }
