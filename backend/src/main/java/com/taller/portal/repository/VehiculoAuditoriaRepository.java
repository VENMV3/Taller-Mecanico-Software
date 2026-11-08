package com.taller.portal.repository;

import com.taller.portal.model.VehiculoAuditoria;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Persistencia append-only de los cambios auditados de vehículos. */
public interface VehiculoAuditoriaRepository extends JpaRepository<VehiculoAuditoria,Long> {
 /** @param vehiculoId vehículo auditado. @return cambios ordenados por inserción. */
 List<VehiculoAuditoria> findByVehiculoIdOrderById(Long vehiculoId);
}
