CREATE TABLE IF NOT EXISTS talleres (
    id BIGINT NOT NULL AUTO_INCREMENT,
    nombre VARCHAR(120) NOT NULL,
    nombre_normalizado VARCHAR(120) NOT NULL,
    razon_social VARCHAR(160) NOT NULL,
    razon_social_normalizada VARCHAR(160) NOT NULL,
    rfc VARCHAR(13) NOT NULL,
    telefono VARCHAR(10) NOT NULL,
    email VARCHAR(140) NOT NULL,
    calle VARCHAR(120) NOT NULL,
    colonia VARCHAR(100) NOT NULL,
    municipio VARCHAR(100) NOT NULL,
    estado VARCHAR(100) NOT NULL,
    codigo_postal VARCHAR(5) NOT NULL,
    fotografia_ruta VARCHAR(255),
    idempotency_key VARCHAR(80) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_taller_rfc UNIQUE (rfc),
    CONSTRAINT uk_taller_nombre_normalizado UNIQUE (nombre_normalizado),
    CONSTRAINT uk_taller_razon_normalizada UNIQUE (razon_social_normalizada),
    CONSTRAINT uk_taller_idempotency UNIQUE (idempotency_key)
);

INSERT INTO talleres (nombre,nombre_normalizado,razon_social,razon_social_normalizada,rfc,telefono,email,calle,colonia,municipio,estado,codigo_postal,fotografia_ruta,idempotency_key)
SELECT 'Motor Nébula','MOTOR NEBULA','Motor Nébula Servicios Automotrices, S.A. de C.V.','MOTOR NEBULA SERVICIOS AUTOMOTRICES, S.A. DE C.V.','MNS260101AB1','5555012048','contacto@motornebula.local','Avenida de la Paz 80','San Ángel','Álvaro Obregón','Ciudad de México','01000',NULL,'semilla-motor-nebula-v1'
WHERE NOT EXISTS (SELECT 1 FROM talleres WHERE idempotency_key = 'semilla-motor-nebula-v1');

-- Hibernate crea estas tablas en instalaciones antiguas. Las definiciones mínimas
-- permiten que Flyway también ejecute la migración sobre una base nueva (pruebas H2).
CREATE TABLE IF NOT EXISTS roles (id BIGINT NOT NULL AUTO_INCREMENT, nombre VARCHAR(30) NOT NULL, descripcion VARCHAR(255) NOT NULL, PRIMARY KEY (id), CONSTRAINT uk_roles_nombre UNIQUE (nombre));
CREATE TABLE IF NOT EXISTS usuarios (id BIGINT NOT NULL AUTO_INCREMENT, nombre VARCHAR(80) NOT NULL, email VARCHAR(140) NOT NULL, celular VARCHAR(25) NOT NULL, password_hash VARCHAR(255) NOT NULL, rol_id BIGINT NOT NULL, activo BOOLEAN NOT NULL, email_verificado BOOLEAN NOT NULL, creado_en TIMESTAMP, ultimo_acceso TIMESTAMP, PRIMARY KEY (id), CONSTRAINT uk_usuarios_email UNIQUE (email), CONSTRAINT uk_usuarios_celular UNIQUE (celular));
CREATE TABLE IF NOT EXISTS clientes (id BIGINT NOT NULL AUTO_INCREMENT, nombre_completo VARCHAR(120) NOT NULL, contacto_alternativo VARCHAR(120) NOT NULL, edad INTEGER NOT NULL, fecha_nacimiento DATE NOT NULL, telefono_personal VARCHAR(25) NOT NULL, telefono_trabajo VARCHAR(25) NOT NULL, email VARCHAR(140) NOT NULL, email_trabajo VARCHAR(140), fotografia_ruta VARCHAR(255), calle VARCHAR(120) NOT NULL, colonia VARCHAR(100) NOT NULL, municipio VARCHAR(100) NOT NULL, estado VARCHAR(100) NOT NULL, codigo_postal VARCHAR(5) NOT NULL, idempotency_key VARCHAR(80) NOT NULL, PRIMARY KEY (id), CONSTRAINT uk_cliente_idempotency UNIQUE (idempotency_key));

ALTER TABLE clientes ADD COLUMN taller_id BIGINT NULL;
ALTER TABLE clientes ADD COLUMN estatus VARCHAR(12) NOT NULL DEFAULT 'ACTIVO';
UPDATE clientes SET taller_id = (SELECT id FROM talleres WHERE idempotency_key = 'semilla-motor-nebula-v1') WHERE taller_id IS NULL;
ALTER TABLE clientes MODIFY COLUMN taller_id BIGINT NOT NULL;
ALTER TABLE clientes ADD CONSTRAINT fk_cliente_taller FOREIGN KEY (taller_id) REFERENCES talleres(id);

ALTER TABLE usuarios ADD COLUMN taller_id BIGINT NULL;
UPDATE usuarios SET taller_id = (SELECT id FROM talleres WHERE idempotency_key = 'semilla-motor-nebula-v1') WHERE taller_id IS NULL;
ALTER TABLE usuarios ADD CONSTRAINT fk_usuario_taller FOREIGN KEY (taller_id) REFERENCES talleres(id);
