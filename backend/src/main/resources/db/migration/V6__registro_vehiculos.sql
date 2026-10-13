-- Catálogo operativo de vehículos; VIN y placa conservan unicidad aun si cambia su Status.
CREATE TABLE IF NOT EXISTS vehiculo (
  id BIGINT NOT NULL AUTO_INCREMENT,
  cliente_id BIGINT NOT NULL,
  vin VARCHAR(17) NOT NULL,
  placa VARCHAR(10) NOT NULL,
  marca VARCHAR(120) NOT NULL,
  modelo VARCHAR(120) NOT NULL,
  anio INT NOT NULL,
  version VARCHAR(120) NOT NULL,
  color VARCHAR(80) NOT NULL,
  kilometraje BIGINT NULL,
  status_id BIGINT NOT NULL,
  creado_en TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  creado_por BIGINT NULL,
  CONSTRAINT pk_vehiculo PRIMARY KEY (id),
  CONSTRAINT uk_vehiculo_vin UNIQUE (vin),
  CONSTRAINT uk_vehiculo_placa UNIQUE (placa),
  CONSTRAINT fk_vehiculo_cliente FOREIGN KEY (cliente_id) REFERENCES clientes(id),
  CONSTRAINT fk_vehiculo_status FOREIGN KEY (status_id) REFERENCES status(id),
  CONSTRAINT fk_vehiculo_creado_por FOREIGN KEY (creado_por) REFERENCES usuarios(id),
  INDEX idx_vehiculo_cliente (cliente_id),
  INDEX idx_vehiculo_status (status_id)
);
