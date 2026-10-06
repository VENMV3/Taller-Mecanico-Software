package com.taller.portal.repository;
import com.taller.portal.dto.TallerDtos.Resumen; import com.taller.portal.model.Taller; import java.util.*; import org.springframework.data.domain.*; import org.springframework.data.jpa.repository.*; import org.springframework.data.repository.query.Param;
/** Acceso parametrizado a talleres y sus duplicados normalizados. */
public interface TallerRepository extends JpaRepository<Taller,Long>{
 Optional<Taller> findByRfc(String rfc); Optional<Taller> findByIdempotencyKey(String key);
 boolean existsByNombreNormalizadoOrRazonSocialNormalizada(String nombre,String razon);
 @Query("select new com.taller.portal.dto.TallerDtos$Resumen(t.id,t.nombre,t.razonSocial,t.rfc,t.municipio) from Taller t") Page<Resumen> listar(Pageable pagina);
 @Query("select new com.taller.portal.dto.TallerDtos$Resumen(t.id,t.nombre,t.razonSocial,t.rfc,t.municipio) from Taller t where t.id=:id") Optional<Resumen> resumen(@Param("id") Long id);
}
