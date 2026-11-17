CREATE TABLE password_reset_token (
  id BIGINT NOT NULL AUTO_INCREMENT,
  usuario_id BIGINT NOT NULL,
  token_hash VARCHAR(64) NOT NULL,
  expira_en TIMESTAMP NOT NULL,
  usado_en TIMESTAMP NULL,
  intentos INTEGER NOT NULL DEFAULT 0,
  creado_en TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  CONSTRAINT fk_password_reset_token_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios(id),
  INDEX idx_password_reset_token_usuario (usuario_id),
  INDEX idx_password_reset_token_hash (token_hash)
);
