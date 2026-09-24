package com.taller.portal.service;
import io.jsonwebtoken.*; import io.jsonwebtoken.security.Keys; import org.springframework.beans.factory.annotation.Value; import org.springframework.stereotype.Service; import javax.crypto.SecretKey; import java.nio.charset.StandardCharsets; import java.util.*;
@Service public class JwtService { private final SecretKey key; private final long expiration;
 public JwtService(@Value("${app.jwt-secret}") String s,@Value("${app.jwt-expiration-ms}") long e){ key=Keys.hmacShaKeyFor(s.getBytes(StandardCharsets.UTF_8));expiration=e; }
 public String generate(String email,String role){return Jwts.builder().subject(email).claim("role",role).issuedAt(new Date()).expiration(new Date(System.currentTimeMillis()+expiration)).signWith(key).compact();}
 public String email(String token){return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload().getSubject();}}
