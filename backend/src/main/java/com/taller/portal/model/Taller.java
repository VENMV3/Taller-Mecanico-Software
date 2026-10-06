package com.taller.portal.model;
import jakarta.persistence.*;
/** Taller aislado que agrupa clientes y personal de recepción. */
@Entity @Table(name="talleres") public class Taller {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
 @Column(nullable=false,length=120) public String nombre;
 @Column(name="nombre_normalizado",nullable=false,unique=true,length=120) public String nombreNormalizado;
 @Column(name="razon_social",nullable=false,length=160) public String razonSocial;
 @Column(name="razon_social_normalizada",nullable=false,unique=true,length=160) public String razonSocialNormalizada;
 @Column(nullable=false,unique=true,length=13) public String rfc;
 @Column(nullable=false,length=10) public String telefono;
 @Column(nullable=false,length=140) public String email;
 @Column(nullable=false,length=120) public String calle; @Column(nullable=false,length=100) public String colonia;
 @Column(nullable=false,length=100) public String municipio; @Column(nullable=false,length=100) public String estado;
 @Column(name="codigo_postal",nullable=false,length=5) public String codigoPostal;
 @Column(name="fotografia_ruta",length=255) public String fotografiaRuta;
 @Column(name="idempotency_key",nullable=false,unique=true,length=80) public String idempotencyKey;
}
