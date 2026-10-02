package com.taller.portal.repository;

import com.taller.portal.dto.CodigosPostalesDtos.Colonia;
import com.taller.portal.dto.CodigosPostalesDtos.Ubicacion;
import com.taller.portal.model.SepomexAsentamiento;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Único acceso de lectura al catálogo local SEPOMEX mediante consultas parametrizadas. */
public interface CodigosPostalesRepository extends JpaRepository<SepomexAsentamiento, Long> {
    /** @param codigoPostal CP de cinco dígitos @return asentamientos ordenados del CP. */
    @Query("select new com.taller.portal.dto.CodigosPostalesDtos$Colonia(s.colonia, s.tipoAsentamiento, s.codigoPostal) from SepomexAsentamiento s where s.codigoPostal = :codigoPostal order by s.colonia")
    List<Colonia> porCodigoPostal(@Param("codigoPostal") String codigoPostal);

    /** @param codigoPostal CP de cinco dígitos @return ubicación distinta del CP, sin cargar la entidad completa. */
    @Query("select distinct new com.taller.portal.dto.CodigosPostalesDtos$Ubicacion(s.estado, s.municipio) from SepomexAsentamiento s where s.codigoPostal = :codigoPostal")
    List<Ubicacion> ubicacionPorCodigoPostal(@Param("codigoPostal") String codigoPostal);

    /** @return entidades federativas distintas, en orden alfabético. */
    @Query("select distinct s.estado from SepomexAsentamiento s order by s.estado")
    List<String> listarEstados();

    /** @param estado entidad exacta @return municipios distintos de la entidad. */
    @Query("select distinct s.municipio from SepomexAsentamiento s where s.estado = :estado order by s.municipio")
    List<String> listarMunicipios(@Param("estado") String estado);

    /** @param estado entidad exacta @param municipio municipio exacto @return colonias y CP correspondientes. */
    @Query("select new com.taller.portal.dto.CodigosPostalesDtos$Colonia(s.colonia, s.tipoAsentamiento, s.codigoPostal) from SepomexAsentamiento s where s.estado = :estado and s.municipio = :municipio order by s.colonia")
    List<Colonia> listarColonias(@Param("estado") String estado, @Param("municipio") String municipio);
}
