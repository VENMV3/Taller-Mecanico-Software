CREATE TABLE IF NOT EXISTS sepomex_asentamientos (
    id BIGINT NOT NULL AUTO_INCREMENT,
    codigo_postal VARCHAR(5) NOT NULL,
    colonia VARCHAR(160) NOT NULL,
    tipo_asentamiento VARCHAR(80) NOT NULL,
    municipio VARCHAR(120) NOT NULL,
    estado VARCHAR(120) NOT NULL,
    c_estado VARCHAR(2) NOT NULL,
    c_mnpio VARCHAR(4) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_sepomex_asentamiento UNIQUE (codigo_postal, colonia, tipo_asentamiento, municipio, estado, c_estado, c_mnpio),
    INDEX idx_sepomex_cp (codigo_postal),
    INDEX idx_sepomex_estado_municipio (estado, municipio)
);
