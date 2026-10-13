package com.taller.portal.model;
import jakarta.persistence.*;
/** Marca canónica de solo lectura importada desde el catálogo vPIC local. */
@Entity @Table(name="vpic_marca") public class VpicMarca { @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id; @Column(name="vpic_make_id",nullable=false,unique=true) public Long vpicMakeId; @Column(nullable=false,unique=true,length=160) public String nombre; }
