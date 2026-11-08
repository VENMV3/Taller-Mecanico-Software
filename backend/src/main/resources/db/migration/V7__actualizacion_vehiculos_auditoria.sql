ALTER TABLE vehiculo ADD COLUMN IF NOT EXISTS control_version BIGINT NOT NULL DEFAULT 0;

CREATE TABLE IF NOT EXISTS vehiculo_auditoria (
  id BIGINT NOT NULL AUTO_INCREMENT,
  vehiculo_id BIGINT NOT NULL,
  usuario_id BIGINT NOT NULL,
  fecha_hora TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  campo VARCHAR(40) NOT NULL,
  valor_anterior VARCHAR(255) NULL,
  valor_nuevo VARCHAR(255) NULL,
  CONSTRAINT pk_vehiculo_auditoria PRIMARY KEY (id),
  CONSTRAINT fk_vehiculo_auditoria_vehiculo FOREIGN KEY (vehiculo_id) REFERENCES vehiculo(id),
  CONSTRAINT fk_vehiculo_auditoria_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios(id),
  INDEX idx_vehiculo_auditoria_vehiculo_fecha (vehiculo_id, fecha_hora)
);
