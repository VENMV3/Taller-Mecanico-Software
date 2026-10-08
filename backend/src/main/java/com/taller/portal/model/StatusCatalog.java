package com.taller.portal.model;
import jakarta.persistence.*;
/** Entrada abierta del catálogo de estatus reutilizable por entidades operativas. */
@Entity @Table(name="status") public class StatusCatalog { @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id; @Column(name="str_valor",nullable=false,unique=true,length=12) public String strValor; @Column(name="str_descripcion",nullable=false,unique=true,length=80) public String strDescripcion; public StatusCatalog(){} }
