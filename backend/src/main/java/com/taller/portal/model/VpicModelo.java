package com.taller.portal.model;
import jakarta.persistence.*;
/** Modelo canónico de solo lectura asociado a una marca vPIC local. */
@Entity @Table(name="vpic_modelo") public class VpicModelo { @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id; @ManyToOne(optional=false) @JoinColumn(name="marca_id",nullable=false) public VpicMarca marca; @Column(name="vpic_model_id",nullable=false) public Long vpicModelId; @Column(nullable=false,length=180) public String nombre; }
