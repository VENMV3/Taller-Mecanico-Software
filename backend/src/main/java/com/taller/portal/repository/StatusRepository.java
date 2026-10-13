package com.taller.portal.repository;
import com.taller.portal.model.StatusCatalog; import java.util.*; import org.springframework.data.jpa.repository.JpaRepository;
/** Acceso al catálogo de estatus con comparaciones normalizadas. */
public interface StatusRepository extends JpaRepository<StatusCatalog,Long>{ Optional<StatusCatalog> findByStrValorIgnoreCase(String valor); boolean existsByStrValorIgnoreCaseOrStrDescripcionIgnoreCase(String valor,String descripcion); @org.springframework.data.jpa.repository.Query("select count(c)>0 from Cliente c where c.status.id=:id") boolean referenciado(@org.springframework.data.repository.query.Param("id") Long id); }
