package com.taller.portal.service;

import com.taller.portal.dto.CodigosPostalesDtos.Colonia;
import com.taller.portal.dto.CodigosPostalesDtos.PorCodigoPostal;
import com.taller.portal.model.RoleName;
import com.taller.portal.repository.CodigosPostalesRepository;
import java.util.List;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Fachada de lectura del catálogo: autoriza, valida entradas y normaliza respuestas. */
@Service
public class CodigosPostalesFacade {
    private final CodigosPostalesRepository repository;

    /** @param repository acceso exclusivo al catálogo local. */
    public CodigosPostalesFacade(CodigosPostalesRepository repository) { this.repository = repository; }

    /** @param codigoPostal CP de cinco dígitos @param auth sesión actual @return resultado o null si no hay CP.
     * @throws IllegalArgumentException si el CP no tiene formato válido @throws AccessDeniedException si el rol no puede consultar. */
    @Transactional(readOnly = true)
    public PorCodigoPostal porCodigoPostal(String codigoPostal, Authentication auth) {
        autorizar(auth); validarCodigoPostal(codigoPostal);
        List<Colonia> colonias = repository.porCodigoPostal(codigoPostal);
        if (colonias.isEmpty()) return null;
        /* SEPOMEX relaciona un CP con la misma entidad y municipio; la UI recibe una respuesta compacta. */
        var ubicacion = repository.ubicacionPorCodigoPostal(codigoPostal).getFirst();
        return new PorCodigoPostal(codigoPostal, ubicacion.estado(), ubicacion.municipio(), colonias);
    }

    /** @param auth sesión actual @return entidades disponibles; lista vacía si el catálogo no está cargado.
     * @throws AccessDeniedException si el rol no puede consultar. */
    @Transactional(readOnly = true)
    public List<String> listarEstados(Authentication auth) { autorizar(auth); return repository.listarEstados(); }

    /** @param estado entidad de máximo 120 caracteres @param auth sesión @return municipios disponibles.
     * @throws IllegalArgumentException si el texto es inválido @throws AccessDeniedException por rol. */
    @Transactional(readOnly = true)
    public List<String> listarMunicipios(String estado, Authentication auth) { autorizar(auth); return repository.listarMunicipios(validarTexto(estado, "Estado")); }

    /** @param estado entidad @param municipio municipio @param auth sesión @return colonias del municipio.
     * @throws IllegalArgumentException si los textos son inválidos @throws AccessDeniedException por rol. */
    @Transactional(readOnly = true)
    public List<Colonia> listarColonias(String estado, String municipio, Authentication auth) {
        autorizar(auth); return repository.listarColonias(validarTexto(estado, "Estado"), validarTexto(municipio, "Municipio"));
    }

    /** @param auth identidad autenticada @throws AccessDeniedException si no pertenece a los roles autorizados. */
    private void autorizar(Authentication auth) {
        if (auth == null || !auth.isAuthenticated() || auth.getAuthorities().stream().noneMatch(authority ->
            authority.getAuthority().equals("ROLE_" + RoleName.ADMINISTRADOR_SISTEMA) || authority.getAuthority().equals("ROLE_" + RoleName.RECEPCIONISTA)))
            throw new AccessDeniedException("No tiene autorización para consultar el catálogo postal");
    }
    /** @param codigoPostal CP @throws IllegalArgumentException si no son cinco dígitos. */
    private void validarCodigoPostal(String codigoPostal) { if (codigoPostal == null || !codigoPostal.matches("\\d{5}")) throw new IllegalArgumentException("El código postal debe tener cinco dígitos"); }
    /** @param texto entrada del usuario @param campo etiqueta visible @return texto sin espacios externos.
     * @throws IllegalArgumentException si está vacío, excede 120 o contiene control. */
    private String validarTexto(String texto, String campo) {
        String limpio = texto == null ? "" : texto.strip();
        if (limpio.isEmpty() || limpio.length() > 120 || limpio.chars().anyMatch(Character::isISOControl)) throw new IllegalArgumentException(campo + " inválido");
        return limpio;
    }
}
