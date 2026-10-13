# Registrar Vehículo

## Propósito

Registrar un vehículo activo asociado a un cliente activo, preservando la unicidad global de VIN y placa y usando únicamente el catálogo vPIC importado en la base local.

## Precondiciones y roles

- El usuario debe estar autenticado y activo.
- Pueden registrar **Recepcionista/Secretaría** y **Desarrollador**. Los demás roles reciben `403`.
- El cliente debe existir, tener estado `ACT`, no estar marcado con `is_deleted` y, para Recepción/Secretaría, pertenecer al taller asignado al usuario.
- El catálogo puede estar vacío: marca y modelo admiten captura manual validada.

## Flujo de datos

`Datos → normalizar → duplicados → guardar → relacionar`

La fachada normaliza primero VIN, placa y textos; verifica cliente, rol y taller; consulta duplicados; persiste el vehículo en una transacción con `Status=ACT`; y lo relaciona mediante `vehiculo.cliente_id`.

## Datos

| Campo | Obligatorio | Normalización | Validación |
| --- | --- | --- | --- |
| Cliente | Sí | ID numérico | Existe, activo, no eliminado y dentro del taller permitido. |
| VIN | Sí | Mayúsculas y sin espacios extremos | `^[A-HJ-NPR-Z0-9]{17}$`; no admite I, O ni Q. |
| Placa | Sí | Minúsculas y sin espacios internos | Letras, números o guiones; 5 a 10 caracteres. |
| Marca | Sí | Espacios únicos y mayúsculas para comparar | Máximo 120; usa forma canónica local si coincide. |
| Modelo | Sí | Espacios únicos y mayúsculas para comparar | Máximo 120; depende de la marca si existe en catálogo. |
| Año | Sí | Entero | De 1950 al año actual + 1. |
| Versión | Sí | Recorte de extremos | Máximo 120 caracteres. |
| Color | Sí | Recorte de extremos | Máximo 80 caracteres. |
| Kilometraje | No | Entero | De 0 a 3,000,000. |

## Reglas de duplicados y resultado

VIN y placa normalizada son únicos aun si un vehículo cambia de estatus. La Fachada los comprueba y también traduce la restricción única de BD a `409`, evitando dobles registros. Una alta correcta devuelve **“Vehículo Activo”** con estado `ACT`.

## Catálogo vPIC local

El catálogo se importa en `vpic_marca` y `vpic_modelo`; la interfaz consulta únicamente `GET /api/vehiculos/catalogo/*` contra la base local. Para descargar e importar en una máquina con internet:

```bash
cd /home/venmve/Taller_Mecanico
./scripts/importar_vpic
```

Para importar un JSON ya descargado:

```bash
./scripts/importar_vpic --archivo /ruta/al/vpic_catalogo.json
```

El script imprime los totales y el informe de cobertura. Fuente: datos públicos de [NHTSA vPIC](https://vpic.nhtsa.dot.gov/api/). NHTSA cubre principalmente el mercado de Estados Unidos; modelos recientes o exclusivos de México pueden faltar y deben capturarse manualmente cuando corresponda.
