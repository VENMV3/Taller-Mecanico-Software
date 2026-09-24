package com.taller.portal.model;
import jakarta.persistence.*;
@Entity @Table(name="roles")
public class Role { @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id; @Enumerated(EnumType.STRING) @Column(nullable=false,unique=true,length=30) public RoleName nombre; @Column(nullable=false) public String descripcion; public Role(){} public Role(RoleName n,String d){nombre=n;descripcion=d;} }
