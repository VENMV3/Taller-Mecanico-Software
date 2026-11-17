package com.taller.portal.config;
import java.time.Instant; import java.util.*; import org.springframework.http.*; import org.springframework.web.bind.annotation.*;
/** Normaliza argumentos inválidos sin alterar contratos explícitos de conflictos o acceso. */
@RestControllerAdvice public class ApiExceptionHandler {
 @ExceptionHandler(IllegalArgumentException.class) ResponseEntity<Map<String,Object>> argumentoInvalido(IllegalArgumentException error){return ResponseEntity.badRequest().body(Map.of("status",400,"error","Bad Request","mensaje",error.getMessage()==null?"Solicitud inválida":error.getMessage(),"timestamp",Instant.now().toString()));}
}
