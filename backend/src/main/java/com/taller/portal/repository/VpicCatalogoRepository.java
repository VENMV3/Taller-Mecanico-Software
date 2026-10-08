package com.taller.portal.repository;
import com.taller.portal.model.*; import java.util.*; import org.springframework.data.domain.PageRequest; import org.springframework.data.jpa.repository.*; import org.springframework.data.repository.query.Param;
/** Único acceso de solo lectura al catálogo vPIC local mediante consultas parametrizadas. */
public interface VpicCatalogoRepository extends JpaRepository<VpicMarca,Long> {
 /** @param prefijo texto canónico inicial @return hasta veinte marcas ordenadas. */
 @Query("select m from VpicMarca m where m.nombre like concat(:prefijo,'%') order by m.nombre") List<VpicMarca> consultarMarcas(@Param("prefijo") String prefijo,PageRequest limite);
 /** @param prefijo texto canónico inicial @return hasta veinte marcas ordenadas. */ default List<VpicMarca> buscarMarcas(String prefijo){return consultarMarcas(prefijo,PageRequest.of(0,20));}
 /** @param marcaId marca local @param prefijo texto canónico inicial @return hasta veinte modelos ordenados. */
 @Query("select m from VpicModelo m where m.marca.id=:marcaId and m.nombre like concat(:prefijo,'%') order by m.nombre") List<VpicModelo> consultarModelos(@Param("marcaId") Long marcaId,@Param("prefijo") String prefijo,PageRequest limite);
 /** @param marcaId marca local @param prefijo texto canónico inicial @return hasta veinte modelos ordenados. */ default List<VpicModelo> buscarModelos(Long marcaId,String prefijo){return consultarModelos(marcaId,prefijo,PageRequest.of(0,20));}
}
