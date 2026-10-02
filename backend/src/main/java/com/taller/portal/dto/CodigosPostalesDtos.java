package com.taller.portal.dto;

import java.util.List;

/** Contratos de lectura del catálogo local SEPOMEX; no exponen entidades JPA. */
public final class CodigosPostalesDtos {
    /** Impide instanciar el contenedor de DTOs. */
    private CodigosPostalesDtos() {}
    /** Asentamiento normalizado. @param colonia nombre @param tipoAsentamiento clasificación @param codigoPostal CP oficial. */
    public record Colonia(String colonia, String tipoAsentamiento, String codigoPostal) {}
    /** Ubicación común de un código postal. @param estado entidad @param municipio municipio oficial. */
    public record Ubicacion(String estado, String municipio) {}
    /** Resultado de un CP. @param codigoPostal CP @param estado entidad @param municipio municipio @param colonias opciones. */
    public record PorCodigoPostal(String codigoPostal, String estado, String municipio, List<Colonia> colonias) {}
}
