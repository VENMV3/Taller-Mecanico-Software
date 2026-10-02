package com.taller.portal.model;

import jakarta.persistence.*;

/** Asentamiento del Catálogo Nacional de Códigos Postales de Correos de México. */
@Entity
@Table(name = "sepomex_asentamientos", uniqueConstraints = @UniqueConstraint(name = "uk_sepomex_asentamiento", columnNames = {
    "codigo_postal", "colonia", "tipo_asentamiento", "municipio", "estado", "c_estado", "c_mnpio"}))
public class SepomexAsentamiento {
    /** Identificador técnico del catálogo. */
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    /** Código postal oficial de cinco dígitos. */
    @Column(name = "codigo_postal", nullable = false, length = 5) public String codigoPostal;
    /** Nombre del asentamiento o colonia. */
    @Column(nullable = false, length = 160) public String colonia;
    /** Clasificación oficial del asentamiento. */
    @Column(name = "tipo_asentamiento", nullable = false, length = 80) public String tipoAsentamiento;
    /** Municipio o alcaldía oficial. */
    @Column(nullable = false, length = 120) public String municipio;
    /** Entidad federativa oficial. */
    @Column(nullable = false, length = 120) public String estado;
    /** Clave SEPOMEX de entidad federativa. */
    @Column(name = "c_estado", nullable = false, length = 2) public String cEstado;
    /** Clave SEPOMEX de municipio. */
    @Column(name = "c_mnpio", nullable = false, length = 4) public String cMnpio;
}
