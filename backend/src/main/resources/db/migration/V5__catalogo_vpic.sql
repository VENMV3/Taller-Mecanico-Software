CREATE TABLE IF NOT EXISTS vpic_marca (
    id BIGINT NOT NULL AUTO_INCREMENT,
    vpic_make_id BIGINT NOT NULL,
    nombre VARCHAR(160) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_vpic_marca_make_id UNIQUE (vpic_make_id),
    CONSTRAINT uk_vpic_marca_nombre UNIQUE (nombre),
    INDEX idx_vpic_marca_nombre (nombre)
);
CREATE TABLE IF NOT EXISTS vpic_modelo (
    id BIGINT NOT NULL AUTO_INCREMENT,
    marca_id BIGINT NOT NULL,
    vpic_model_id BIGINT NOT NULL,
    nombre VARCHAR(180) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_vpic_modelo_marca FOREIGN KEY (marca_id) REFERENCES vpic_marca(id),
    CONSTRAINT uk_vpic_modelo_nombre UNIQUE (marca_id, nombre),
    CONSTRAINT uk_vpic_modelo_make_model UNIQUE (marca_id, vpic_model_id),
    INDEX idx_vpic_modelo_nombre (nombre),
    INDEX idx_vpic_modelo_marca (marca_id)
);
