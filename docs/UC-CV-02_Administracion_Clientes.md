# UC-CV-02 Administración de Clientes

Administra talleres y expedientes de clientes con permisos por rol y aislamiento obligatorio por taller.

| Campo | Tabla | Tipo | Obligatorio | Validación/Regla |
|---|---|---|---|---|
| id | talleres | BIGINT | Sí | Clave primaria. |
| nombre / nombre_normalizado | talleres | VARCHAR(120) | Sí | Único sin distinguir mayúsculas ni acentos. |
| razon_social / razon_social_normalizada | talleres | VARCHAR(160) | Sí | Única normalizada. |
| rfc | talleres | VARCHAR(13) | Sí | Único; RFC mexicano y fecha real. |
| telefono | talleres | VARCHAR(10) | Sí | Diez dígitos. |
| email | talleres | VARCHAR(140) | Sí | E-mail válido. |
| calle, colonia, municipio, estado, codigo_postal | talleres | VARCHAR | Sí | Dirección; CP de cinco dígitos. |
| fotografia_ruta | talleres | VARCHAR(255) | No | Imagen real, máximo 15 MB. |
| taller_id | clientes | BIGINT | Sí | FK al taller; se obtiene del usuario para Recepcionista. |
| estatus | clientes | VARCHAR(12) | Sí | ACTIVO por defecto o SUSPENDIDO; no se elimina el registro. |
| taller_id | usuarios | BIGINT | No | Taller asignado para aislamiento de Recepcionista. |

| Acción | Administrador | Recepcionista |
|---|---|---|
| Crear y consultar talleres | Sí | No |
| Registrar cliente | Sí, selecciona taller | Sí, taller asignado por servidor |
| Consultar clientes | Sí, filtra por taller | Sí, solo su taller |
| Editar cliente | Sí, incluido cambio de taller | Sí, sin cambiar taller |
| Suspender cliente | Sí | No |
| Reactivar cliente | Sí | No |
