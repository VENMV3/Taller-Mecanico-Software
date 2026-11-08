package com.taller.portal.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/** Registro append-only de un campo de vehículo modificado por una identidad autenticada. */
@Entity @Table(name="vehiculo_auditoria") public class VehiculoAuditoria {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
 @ManyToOne(optional=false) @JoinColumn(name="vehiculo_id",nullable=false) public Vehiculo vehiculo;
 @ManyToOne(optional=false) @JoinColumn(name="usuario_id",nullable=false) public User usuario;
 @Column(name="fecha_hora",nullable=false) public LocalDateTime fechaHora=LocalDateTime.now();
 @Column(nullable=false,length=40) public String campo;
 @Column(name="valor_anterior",length=255) public String valorAnterior;
 @Column(name="valor_nuevo",length=255) public String valorNuevo;
}
