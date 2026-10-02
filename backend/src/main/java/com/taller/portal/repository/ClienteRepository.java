package com.taller.portal.repository;
import com.taller.portal.model.Cliente; import org.springframework.data.jpa.repository.JpaRepository; import java.time.LocalDate; import java.util.Optional;
/** Repositorio de persistencia y detección de duplicados de clientes. */
public interface ClienteRepository extends JpaRepository<Cliente,Long>{ /** @param key clave idempotente @return cliente previo si existe. */ Optional<Cliente> findByIdempotencyKey(String key); /** @return verdadero ante campos únicos repetidos. */ boolean existsByEmailOrTelefonoPersonalOrTelefonoTrabajoOrNombreCompletoAndFechaNacimiento(String email,String personal,String trabajo,String nombre,LocalDate fecha);
    /** Busca campos públicos con parámetros enlazados y paginación SQL.
     * @param patron búsqueda literal escapada @param pagina límites y orden
     * @return página con las cinco columnas visibles e ID. */
    @org.springframework.data.jpa.repository.Query("""
        select new com.taller.portal.dto.ClienteConsultaDtos$Resumen(
            c.id, c.nombreCompleto, c.telefonoPersonal, c.email, c.municipio)
        from Cliente c where lower(c.nombreCompleto) like :patron escape '!'
          or lower(c.telefonoPersonal) like :patron escape '!'
          or lower(c.email) like :patron escape '!'
        """)
    org.springframework.data.domain.Page<com.taller.portal.dto.ClienteConsultaDtos.Resumen> consultar(
        @org.springframework.data.repository.query.Param("patron") String patron,
        org.springframework.data.domain.Pageable pagina);

    /** @param id identificador @return detalle sin datos internos; vacío si no existe. */
    @org.springframework.data.jpa.repository.Query("""
        select new com.taller.portal.dto.ClienteConsultaDtos$Detalle(c.id, c.nombreCompleto,
        c.contactoAlternativo, c.edad, c.fechaNacimiento, c.telefonoPersonal, c.telefonoTrabajo,
        c.email, c.emailTrabajo, c.calle, c.colonia, c.municipio, c.estado, c.codigoPostal)
        from Cliente c where c.id = :id
        """)
    Optional<com.taller.portal.dto.ClienteConsultaDtos.Detalle> consultarDetalle(
        @org.springframework.data.repository.query.Param("id") Long id);

    /** @param id identificador @return ruta interna usada solo por la fachada. */
    @org.springframework.data.jpa.repository.Query("select c.fotografiaRuta from Cliente c where c.id = :id")
    Optional<String> consultarRutaFoto(@org.springframework.data.repository.query.Param("id") Long id);
}
