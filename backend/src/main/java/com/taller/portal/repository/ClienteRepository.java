package com.taller.portal.repository;
import com.taller.portal.model.Cliente; import org.springframework.data.jpa.repository.JpaRepository; import java.time.LocalDate; import java.util.Optional;
/** Repositorio de persistencia y detección de duplicados de clientes. */
public interface ClienteRepository extends JpaRepository<Cliente,Long>{ /** @param key clave idempotente @return cliente previo si existe. */ Optional<Cliente> findByIdempotencyKey(String key); /** @return verdadero ante campos únicos repetidos. */ boolean existsByEmailOrTelefonoPersonalOrTelefonoTrabajoOrNombreCompletoAndFechaNacimiento(String email,String personal,String trabajo,String nombre,LocalDate fecha); }
