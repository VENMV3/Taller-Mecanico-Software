package com.taller.portal.repository;

import com.taller.portal.model.Vehiculo;
import org.springframework.data.jpa.repository.JpaRepository;

/** Acceso transaccional a vehículos; sus restricciones de BD preservan la unicidad global. */
public interface VehiculoRepository extends JpaRepository<Vehiculo,Long> {
 /** @param vin VIN ya normalizado. @return si existe en cualquier estatus. */ boolean existsByVin(String vin);
 /** @param placa placa ya normalizada. @return si existe en cualquier estatus. */ boolean existsByPlaca(String placa);
}
