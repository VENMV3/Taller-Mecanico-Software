package com.taller.portal.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/** Entidad persistente de un vehículo asociado a un único cliente activo. */
@Entity @Table(name="vehiculo") public class Vehiculo {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
 @ManyToOne(optional=false) @JoinColumn(name="cliente_id",nullable=false) public Cliente cliente;
 @Column(nullable=false,unique=true,length=17) public String vin;
 @Column(nullable=false,unique=true,length=10) public String placa;
 @Column(nullable=false,length=120) public String marca;
 @Column(nullable=false,length=120) public String modelo;
 @Column(nullable=false) public Integer anio;
 @Column(nullable=false,length=120) public String version;
 @Column(nullable=false,length=80) public String color;
 public Long kilometraje;
 @ManyToOne(optional=false) @JoinColumn(name="status_id",nullable=false) public StatusCatalog status;
 @Column(name="creado_en",nullable=false) public LocalDateTime creadoEn=LocalDateTime.now();
 @ManyToOne @JoinColumn(name="creado_por") public User creadoPor;
}
