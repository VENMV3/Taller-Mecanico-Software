package com.taller.portal.controller;

import com.taller.portal.dto.CodigosPostalesDtos.Colonia;
import com.taller.portal.dto.CodigosPostalesDtos.PorCodigoPostal;
import com.taller.portal.service.CodigosPostalesFacade;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/** API autenticada de consulta del catálogo local; nunca llama proveedores externos. */
@RestController
@RequestMapping("/api/codigos-postales")
public class CodigosPostalesController {
    private final CodigosPostalesFacade facade;
    /** @param facade entrada única del catálogo. */
    public CodigosPostalesController(CodigosPostalesFacade facade) { this.facade = facade; }
    /** @param codigoPostal CP @param auth sesión @return datos del CP o 404 cuando no existe. */
    @GetMapping("/{codigoPostal}")
    public ResponseEntity<PorCodigoPostal> porCodigoPostal(@PathVariable String codigoPostal, Authentication auth) {
        PorCodigoPostal resultado = facade.porCodigoPostal(codigoPostal, auth);
        return resultado == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(resultado);
    }
    /** @param auth sesión @return entidades locales distintas. */
    @GetMapping("/estados") public List<String> estados(Authentication auth) { return facade.listarEstados(auth); }
    /** @param estado entidad @param auth sesión @return municipios locales. */
    @GetMapping("/municipios") public List<String> municipios(@RequestParam String estado, Authentication auth) { return facade.listarMunicipios(estado, auth); }
    /** @param estado entidad @param municipio municipio @param auth sesión @return colonias locales. */
    @GetMapping("/colonias") public List<Colonia> colonias(@RequestParam String estado, @RequestParam String municipio, Authentication auth) { return facade.listarColonias(estado, municipio, auth); }
    /** @param error solicitud inválida @return mensaje 400. */
    @ExceptionHandler(IllegalArgumentException.class) ResponseEntity<Map<String, String>> invalido(Exception error) { return ResponseEntity.badRequest().body(Map.of("message", error.getMessage())); }
    /** @param error rol no autorizado @return mensaje 403. */
    @ExceptionHandler(AccessDeniedException.class) ResponseEntity<Map<String, String>> prohibido(Exception error) { return ResponseEntity.status(403).body(Map.of("message", error.getMessage())); }
}
