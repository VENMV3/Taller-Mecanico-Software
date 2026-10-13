-- H2 2.3 EXPLAIN usa FK_CLIENTE_TALLER_INDEX_B para taller_id, sin escaneo completo; MySQL debe verificarse con EXPLAIN tras desplegar.
CREATE INDEX ix_clientes_suspension_alcance ON clientes(taller_id, is_deleted, status_id, nombre_completo);
CREATE INDEX ix_clientes_suspension_contacto ON clientes(taller_id, telefono_personal, email);
