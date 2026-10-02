package com.taller.portal.controller;
import com.taller.portal.dto.ClienteDtos.*; import com.taller.portal.service.ClienteRegistrationFacade; import jakarta.validation.Valid; import org.springframework.http.*; import org.springframework.security.access.AccessDeniedException; import org.springframework.security.core.Authentication; import org.springframework.web.bind.annotation.*; import org.springframework.web.multipart.MultipartFile; import java.io.IOException; import java.util.Map;
/** API de presentación; delega todo el registro a la fachada. */
@RestController @RequestMapping("/api/clientes") public class ClienteController { private final ClienteRegistrationFacade facade; /** @param facade único caso de uso permitido. */ public ClienteController(ClienteRegistrationFacade facade){this.facade=facade;} /** @param datos JSON multipart validado @param foto imagen adjunta @param key clave idempotente @param auth usuario JWT @return Cliente Registrado @throws IOException si falla foto. */ @PostMapping(consumes=MediaType.MULTIPART_FORM_DATA_VALUE) public ResponseEntity<Respuesta> registrar(@Valid @RequestPart("datos") Registro datos,@RequestPart("fotografia") MultipartFile foto,@RequestHeader("Idempotency-Key") String key,Authentication auth)throws IOException{return ResponseEntity.status(HttpStatus.CREATED).body(facade.registrar(datos,foto,key,auth));} /** @param e error de validación/duplicado @return mensaje 400. */ @ExceptionHandler({IllegalArgumentException.class}) ResponseEntity<Map<String,String>> bad(Exception e){return ResponseEntity.badRequest().body(Map.of("message",e.getMessage()));} /** @param e denegación de rol @return mensaje 403. */ @ExceptionHandler(AccessDeniedException.class) ResponseEntity<Map<String,String>> denied(Exception e){return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message",e.getMessage()));}
    /** @param pagina base cero @param tamanio 1..50 @param busqueda texto
     * @param auth sesión @return página autorizada; propaga validación y denegación de fachada. */
    @GetMapping
    public com.taller.portal.dto.ClienteConsultaDtos.Pagina consultar(
        @RequestParam(defaultValue="0") int pagina, @RequestParam(defaultValue="10") int tamanio,
        @RequestParam(defaultValue="") String busqueda, Authentication auth) {
        return facade.consultar(pagina, tamanio, busqueda, auth);
    }
    /** @param id cliente @param auth sesión @return detalle; 404 si no existe, 403 por rol. */
    @GetMapping("/{id}")
    public com.taller.portal.dto.ClienteConsultaDtos.Detalle detalle(@PathVariable Long id, Authentication auth) {
        return facade.detalle(id, auth);
    }
    /** @param id cliente @param auth sesión @return PNG privado @throws IOException por lectura fallida. */
    @GetMapping("/{id}/fotografia")
    public ResponseEntity<byte[]> fotografia(@PathVariable Long id, Authentication auth) throws IOException {
        var foto = facade.fotografia(id, auth);
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(foto.mime()))
            .cacheControl(CacheControl.noStore()).header("X-Content-Type-Options", "nosniff").body(foto.contenido());
    }
}
