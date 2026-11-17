package com.taller.portal.model;
import jakarta.persistence.*; import java.time.LocalDateTime;
/** Token de recuperación persistido exclusivamente como hash. */
@Entity @Table(name="password_reset_token") public class PasswordResetToken {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
 @ManyToOne(optional=false) @JoinColumn(name="usuario_id",nullable=false) public User usuario;
 @Column(name="token_hash",nullable=false,length=64) public String tokenHash;
 @Column(name="expira_en",nullable=false) public LocalDateTime expiraEn;
 @Column(name="usado_en") public LocalDateTime usadoEn;
 @Column(nullable=false) public int intentos;
 @Column(name="creado_en",nullable=false) public LocalDateTime creadoEn=LocalDateTime.now();
}
