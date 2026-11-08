---
name: spring-boot-rest-spec
description: Guía técnica para analizar, diseñar, implementar y validar el backend, los datos y la seguridad del portal de taller (Spring Boot 3.5, Java 21, JPA, Flyway) mediante especificaciones verificables y alcance controlado. La interfaz se rige por ../frontend/frontend.md.
version: 2.0.0
language: es
related: ../orquestador/orquestador.md, ../frontend/frontend.md, ../Agents/AGENTS.md
---

# Skill profesional — Portal de taller (Spring Boot REST)

## 0. Perfil del proyecto

Hechos tomados de `AGENTS.md`. Verifícalos con `rg`/lectura antes de apoyarte en ellos; lo que no esté aquí es **por verificar**, nunca se asume.

| Aspecto | Valor |
| --- | --- |
| Backend | `backend/`: Spring Boot 3.5, Java 21, JPA, Spring Security, Flyway, Maven |
| Frontend | `frontend/src/`: React + Vite; guía completa en `../frontend/frontend.md` |
| Arquitectura | **Controller → Facade → Repository** en `com.taller.portal`. La Facade concentra autorización, validación, orden del flujo, transacciones y manejo de errores |
| BD | MySQL en desarrollo (`./iniciar-taller.sh`); H2 + Flyway en pruebas |
| Migraciones | `backend/src/main/resources/db/migration/V<n>__*.sql`. **Nunca se edita una migración aplicada**; se crea una nueva |
| Catálogos | SEPOMEX y vPIC importados localmente (`scripts/`, `data/` ignorado por Git); en ejecución no requieren red |
| Roles conocidos | Administrador del Sistema, Recepcionista (Secretaria). Desarrollador: **verificar que exista; no inventarlo** |
| Convenciones de datos | Soft delete con `is_deleted`, `deleted_at`, `deleted_by`; catálogo `status` con códigos `ACT`/`SUS`/`CAN` identificados por constante central, no por ids dispersos |
| Pruebas | `cd backend && mvn test`; `cd frontend && npm run build`. No cambiar la configuración de Surefire/Byte Buddy sin una prueba reproducible |
| Idioma | Respuestas, mensajes y documentación en español |

## 1. Propósito y contrato de aplicación

Actúa como ingeniero senior de Java y Spring Boot. Desarrolla software mantenible, seguro y eficiente con Specification-Driven Development: especificar, diseñar la solución mínima suficiente, implementar y verificar contra criterios de aceptación. `../orquestador/orquestador.md` gobierna la ejecución; `../Agents/AGENTS.md` es el contrato del repositorio y prevalece sobre las recomendaciones genéricas de este archivo; `../frontend/frontend.md` rige la interfaz. Las rutas de este archivo son relativas a su ubicación (`spec/skills/`).

**DEBE** indica obligación; **DEBERÍA**, recomendación con excepción justificada; **PUEDE**, opción condicionada al alcance. La seguridad y la corrección necesarias para entregar lo solicitado son obligatorias. No se crean funcionalidades adicionales bajo el argumento de «buenas prácticas».

**Activación:** al crear o modificar APIs, casos de uso, persistencia, migraciones, pruebas, seguridad, documentación, importadores o scripts de siembra. Las pantallas del portal se rigen por `frontend.md`. Para una consulta conceptual, responde sin generar código ni archivos.

**Entradas mínimas:** solicitud y resultado observable; rama y estado de Git; convenciones y patrones existentes; esquema real afectado; contratos y consumidores; restricciones de seguridad. Si falta un dato, separa supuesto reversible de decisión bloqueante. Nunca inventes acceso, reglas de negocio, resultados de pruebas ni capacidad de rendimiento.

## 2. Protocolo SPEC y trazabilidad

SPEC es una convención operativa de SDD, no un estándar oficial.

| Elemento | Contenido verificable |
| --- | --- |
| S — Solicitud y alcance | Problema, actores, comportamiento pedido, exclusiones, restricciones |
| P — Precisión del contrato | Requisitos, entradas/salidas, reglas, errores, permisos y criterios de aceptación |
| E — Ejecución diseñada | Decisiones, componentes, tareas, dependencias y riesgos |
| C — Comprobación | Pruebas, evidencia, diferencias contra la SPEC y estado final |

Toda modificación DEBE rastrearse: `REQ → AC → TASK → cambio → TEST/evidencia`. Un hallazgo ajeno al alcance se registra como propuesta (CR); no se implementa.

```markdown
# SPEC: <id y título>
Estado: borrador | lista | en ejecución | validada | bloqueada
Solicitud de origen / Objetivo observable:
Incluido: ... | Excluido: ...
Restricciones y supuestos:
REQ-001: <comportamiento>  BR-001: <regla confirmada>
AC-001: Dado <contexto>, cuando <acción>, entonces <resultado observable>.
Contrato: <HTTP, DTO, validaciones, errores, permisos por rol, alcance por taller>
Datos: <migración, integridad, unicidad, índices, transacciones>
TASK-001 → REQ-001/AC-001: <cambio>   TEST-001 → AC-001: <caso y evidencia>
Riesgos y pendientes / Definition of Done:
```

Para un cambio pequeño, la SPEC puede ser una lista de AC en la respuesta. No dupliques documentación.

## 3. Base tecnológica y dependencias

1. Inspecciona `backend/pom.xml` y `frontend/package.json` antes de decidir. Conserva las versiones del proyecto; no actualices versiones mayores sin alcance.
2. Usa la gestión de dependencias de Spring Boot; no mezcles `javax.*` y `jakarta.*`.
3. **No añadas dependencias** salvo necesidad demostrada para un requisito, con una línea de justificación (licencia, mantenimiento, alternativa existente). En este entorno puede no haber red ni Docker: no propongas herramientas que los requieran como paso obligatorio.
4. Inyección por constructor; configuración externa con `@ConfigurationProperties`; ningún secreto en Git, logs ni ejemplos.

## 4. Arquitectura proporcional

- Respeta **Controller → Facade → Repository**. No reorganices a hexagonal/Clean ni a otra estructura de paquetes salvo orden expresa.
- **Controller:** transporte HTTP, DTO, códigos de estado; sin lógica de negocio ni consultas.
- **Facade:** única entrada de la UI/API al caso de uso: (1) autorizar, (2) validar, (3) normalizar, (4) comprobar duplicados/estado, (5) escribir, (6) mapear errores. Define los límites transaccionales.
- **Repository:** consultas parametrizadas; sin reglas de negocio.
- Reutiliza validadores y normalizadores existentes; **no los dupliques**. Las reglas de edición deben ser las mismas que las del registro.
- Evita abstracciones sin variación real, repositorios genéricos que solo repiten Spring Data, y patrones nuevos (CQRS, mensajería, caché, microservicios) sin alcance autorizado.
- Registra decisiones significativas como DEC; ADR solo para decisiones estructurales.

## 5. Contratos REST

### Semántica y códigos

| Código | Uso en este proyecto |
| --- | --- |
| 200/201 | Éxito; 201 con el recurso creado cuando se crea |
| 400 | Validación fallida, con errores por campo |
| 401 | Sin autenticación válida |
| 403 | Rol o alcance insuficiente (rol no permitido, intento de cambiar taller sin ser Administrador) |
| 404 | Recurso inexistente o **no visible** para ese usuario/taller/estado (p. ej. cliente soft-deleted en consultas operativas); documenta la decisión en una línea |
| 409 | Conflicto: dato duplicado (indicando cuál: VIN, placa, teléfono, e-mail, etc.), estatus base protegido, estatus en uso, cliente ya suspendido/activo, restricción única violada en BD |

No devuelvas 200 para errores ni filtres SQL, stack traces o rutas internas. Usa el formato de error que ya emplea el proyecto (o `ProblemDetail` si ya está adoptado); centraliza con `@RestControllerAdvice`.

### DTO, validación y compatibilidad
- DTO de entrada/salida; no expongas entidades JPA ni campos internos (`is_deleted`, auditoría) si el contrato no los pide. Evita mass assignment: mapea campos permitidos explícitamente (en edición de cliente, **no** se acepta fotografía ni estatus).
- Validación en el borde (Bean Validation) y reglas de negocio en la Facade; los constraints de BD son la defensa final. Define formato de fechas, longitudes máximas y nulabilidad; la edad debe ser coherente con la fecha de nacimiento.
- Normaliza igual que el registro antes de validar duplicados (VIN en mayúsculas, placa en minúsculas con espacios colapsados, textos recortados).
- No alteres contratos existentes en silencio; documenta cualquier cambio.

### Colecciones y paginación
- Paginación en **servidor**, tamaño máximo impuesto por el servidor (10 en los listados de gestión); ignora tamaños mayores del cliente.
- Orden solo por columnas de **lista blanca**; columna inválida → rechazo (400). Orden estable con desempate único (p. ej. `id`).
- El conteo total usa los mismos filtros que la página. Usa `Slice` si no se necesita total.
- Identifica la acción de «doble envío»: las escrituras que puedan repetirse deben ser idempotentes (restricción única + transacción) y la UI debe bloquear el reenvío.

## 6. Seguridad desde el diseño

1. **La autorización vive en el backend.** Ocultar una opción en la UI no es un control. Verifica rol por operación (Facade/Controller) y alcance por objeto.
2. **Alcance por taller:** el taller permitido se deduce del usuario autenticado **en el servidor**; ignora cualquier taller enviado por el cliente. Solo el Administrador puede cambiar el taller de un cliente, y el taller destino debe existir.
3. Matriz de permisos vigente (verifícala en el código antes de usarla):

| Acción | Administrador | Recepcionista |
| --- | --- | --- |
| Ingresar / editar cliente | Sí | Sí (solo su taller) |
| Suspender / reactivar cliente | Sí | No |
| Gestionar catálogo de estatus | Sí | No |
| Registrar vehículo | Sí, si el proyecto lo permite (verificar) | Sí |

4. Denegar por defecto; no desactives ni relajes controles para que pase una prueba.
5. JWT/sesión/CSRF/CORS: usa la infraestructura existente y no cambies la autenticación salvo alcance expreso. No expongas tokens, contraseñas ni rutas de carga en respuestas ni logs.
6. Consultas siempre parametrizadas; nunca construyas SQL ni columnas de orden desde entrada libre.
7. Secretos fuera de Git (`.env`, credenciales, `target/`, logs, catálogos descargados).
8. Los datos de prueba no deben copiar datos personales reales del entorno de desarrollo.

Pruebas mínimas cuando aplique: anónimo, rol insuficiente (403), objeto de otro taller, cambio de taller no autorizado, entrada inválida/maliciosa, y que el cliente no pueda forzar el taller por parámetro.

## 7. Persistencia, estados y concurrencia

### Integridad
- Las migraciones deben ejecutarse tanto en MySQL 8 como en H2: no uses `IF NOT EXISTS` en `ALTER` ni en `CREATE INDEX`, pues H2 acepta sintaxis que MySQL rechaza.
- Define PK, FK, unicidad, nulabilidad e índices con base en filtros, joins y orden reales. No indexes por reflejo.
- **Migraciones Flyway versionadas e idempotentes en su efecto**; una migración nueva por cambio; datos migrados sin pérdida (p. ej. estatus antiguo → FK). No uses `ddl-auto=update/create`.
- Una comprobación previa en Java no evita carreras: respáldala con restricción única en BD y **mapea la violación (`DataIntegrityViolationException` o equivalente) al mismo 409** que la comprobación previa, nunca a 500.
- **Soft delete:** las consultas operativas excluyen `is_deleted = true`; solo los módulos que lo requieren (filtro de suspendidos para el Administrador) los incluyen. **Unicidad y detección de duplicados siempre consideran también los soft-deleted.**
- Los estatus base (`ACT`, `SUS`, `CAN`) se referencian por constante central de `StrValor`, no por ids fijos.

### Transiciones de estado (suspender / reactivar)
Orden obligatorio: (1) permiso, (2) comprobar estado actual y, si ya está en el estado destino, informar sin error y sin continuar, (3) `UPDATE` atómico condicional (p. ej. `WHERE status_id <> SUS AND is_deleted = false` para suspender; `WHERE is_deleted = true AND status_id = SUS` para reactivar). Dos solicitudes simultáneas deben aplicarse **una sola vez**; se prueba. Suspender registra `status_id`, `is_deleted`, `deleted_at`, `deleted_by`; reactivar los restablece. Nada se elimina físicamente.

### Transacciones
- Transacciones cortas y explícitas en la Facade; validación + duplicados + escritura en la misma transacción. Cuidado con la auto-invocación y los proxies de `@Transactional`: verifica con una prueba.
- Rollback para excepciones no comprobadas; declara rollback explícito para comprobadas si aplica.
- Operaciones masivas: considera la desincronización del contexto de persistencia.

### Consultas y eficiencia
| Necesidad | Alternativa | Evidencia |
| --- | --- | --- |
| Listado paginado con estatus | Una consulta con `JOIN` a `status` y proyección de solo las columnas necesarias | Sin N+1; conteo con los mismos filtros |
| Filtros opcionales | Specifications o la solución ya adoptada | Predicados y parámetros seguros |
| Búsqueda por prefijo (catálogos) | Índice sobre el nombre normalizado + `LIKE 'prefijo%'` con límite | Plan sin escaneo completo |
| Agregaciones complejas | SQL nativo solo si ya se usa | Plan y mapeo probado |

Protocolo para optimizar: especificar resultado, filtros, permisos, orden y tamaño → obtener SQL representativo → medir/inspeccionar con `EXPLAIN` (solo lectura, en MySQL de desarrollo) → ajustar índices → comparar antes/después sin cambiar resultados → regresión. Evita N+1, `SELECT *` innecesario, filtrar en Java y colecciones sin límite. No pagines un fetch join de colección.

### Importadores y siembras (SEPOMEX, vPIC, datos demo)
- **Idempotentes**: transacción, restricciones únicas, `ON CONFLICT`/equivalente; repetir no duplica. Imprimen totales (nuevos/existentes/omitidos).
- Importadores de red (vPIC): pausa ≥200 ms entre llamadas, reintentos con espera creciente y tiempo máximo por petición; opción de importar desde archivo para probar sin internet; informe de cobertura.
- Tablas de catálogo de **solo lectura** para la aplicación; la UI consulta solo el backend local.
- **Siembra de datos demo:** entra por la Facade (mismas validaciones que la UI), con datos realistas y parecidos a los existentes (formato de nombres, teléfonos de 10 dígitos válidos, e-mails plausibles y únicos, CP/colonia/municipio/estado de SEPOMEX, talleres existentes, edad coherente), sin copiar datos reales y sin términos de relleno. No corre sola ni dentro de las pruebas; se ejecuta y se verifica con una consulta cuando la tarea lo ordena.

## 8. Frontend

La interfaz (React + Vite) se rige por `../frontend/frontend.md`, que prevalece en todo lo visual, de accesibilidad y de formularios. Desde este archivo solo se fijan los contratos que la UI consume:

- Los códigos 400/403/404/409 y la paginación del servidor son los de la sección 5; la UI no los reinterpreta.
- La UI refleja los permisos pero **no los sustituye** (sección 6): la autorización vive en el backend.
- Si la tarea incluye interfaz y cambia un contrato HTTP, el servicio de API correspondiente del frontend se actualiza en la misma tarea; si no incluye interfaz, el cambio de contrato se declara como pendiente para el frontend.
- La validación del frontend usa las mismas reglas que el backend, pero nunca es la única.

## 9. Errores, logs y mantenibilidad

- **Nunca ocultes la causa real.** Un error inesperado se registra en el log con su causa completa y correlación; al usuario se le muestra un mensaje genérico. Los errores de negocio devuelven códigos y mensajes claros (qué dato está duplicado, por qué no se puede eliminar un estatus).
- No registres tokens, contraseñas ni cuerpos completos con datos personales.
- Nombres del dominio, métodos pequeños, errores específicos; no captures `Exception` para silenciar; no devuelvas `null` ambiguo.
- Evita `@Data` indiscriminado en entidades JPA; define identidad y excluye campos sensibles del `toString`.
- **Documenta cada clase y método creado o modificado** (propósito, parámetros, retorno, excepciones) con el formato del proyecto; comentarios para decisiones, no para repetir instrucciones.
- Documentación del módulo: actualiza solo las secciones afectadas (p. ej. `Planeacion_FaseCreacionCuenta.md`, `docs/`, diagramas Archify en `docs/diagramas/`).

## 10. Estrategia de validación

| Nivel | Qué verificar | Cómo en este proyecto |
| --- | --- | --- |
| Unitario | Validadores, normalización, reglas | JUnit; Mockito solo si aporta aislamiento |
| Facade/servicio | Permisos, orden del flujo, duplicados, transiciones, concurrencia | `@SpringBootTest` con H2 y transacciones; modelo: `SuspensionFacadeTest` |
| Web | Contrato HTTP y códigos 400/403/404/409 | MockMvc con usuario autenticado por rol |
| Persistencia | Migraciones desde cero, constraints, paginación | H2 + Flyway; todas las migraciones corren desde cero |
| Concurrencia | Dos solicitudes simultáneas se aplican una sola vez | Prueba con hilos sobre el `UPDATE` condicional |
| Esquema real | La BD de desarrollo coincide con lo esperado | Comprobación **manual** contra MySQL de desarrollo (esquema, `EXPLAIN` de solo lectura); se reporta aparte |
| Frontend | Compila y respeta contratos | `npm run build` + lista de verificación manual |

Reglas:
- Las pruebas automáticas **no se conectan a la base de desarrollo**: usan H2, fixtures y transacciones de prueba. Los fixtures (p. ej. vPIC) viven en `backend/src/test/resources/`.
- Donde H2 no pueda validar algo específico de MySQL (plan de ejecución, collation, comportamiento de índices), decláralo como **no verificado por pruebas automáticas** y verifícalo manualmente en MySQL de desarrollo; no lo des por aprobado.
- Una prueba debe haber fallado sin la corrección y pasar con ella cuando se trata de un bug.
- Ejecuta pruebas dirigidas (`mvn -q -Dtest='*Clase*' test`) y luego `mvn test` completo. Guarda comando, resultado y limitación. Lo no ejecutado se reporta como pendiente.
- Evita mocks que solo reproducen la implementación; no exijas pruebas de carga para cambios menores.

## 11. Definition of Done

- [ ] Cada requisito incluido tiene criterios de aceptación y evidencia.
- [ ] El diff contiene solo cambios solicitados o indispensables y justificados.
- [ ] `mvn test` completo en verde (y `npm run build` si hubo frontend), o bloqueo declarado.
- [ ] Autorización en backend, alcance por taller, duplicados (incluidos soft-deleted) y mapeo a 409 verificados donde aplican.
- [ ] Migraciones nuevas (no se editó ninguna aplicada) probadas desde cero; esquema real comprobado o declarado no verificado.
- [ ] Errores inesperados se registran con su causa; no hay secretos en el diff.
- [ ] Clases y métodos documentados; documentación afectada actualizada.
- [ ] Commit local con la convención del historial; sin `push`; comandos entregados al usuario.
- [ ] Diagrama de la tarea creado con Archify (o excepción declarada), según `../orquestador/orquestador.md`.
- [ ] Tabla de fases y resumen conforme a `../orquestador/orquestador.md`.

No declares terminado un conjunto con criterios obligatorios fallidos o sin verificar.

## 12. Fuentes técnicas oficiales

Referencias consultadas el 11 de octubre de 2026. Selecciona siempre la documentación de la versión del proyecto antes de copiar APIs o configuración. Las reglas SPEC y de alcance son convenciones de estos documentos.

- Spring Security, autenticación: https://docs.spring.io/spring-security/reference/servlet/authentication/architecture.html
- Spring Security, CSRF: https://docs.spring.io/spring-security/reference/servlet/exploits/csrf.html
- Spring Framework, respuestas de error y ProblemDetail: https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-ann-rest-exceptions.html
- Spring Data JPA, proyecciones: https://docs.spring.io/spring-data/jpa/reference/repositories/projections.html
- Spring Data JPA, métodos de consulta y EntityGraph: https://docs.spring.io/spring-data/jpa/reference/jpa/query-methods.html
- Spring Boot, funciones de producción: https://docs.spring.io/spring-boot/reference/actuator/index.html
- Spring Boot, observabilidad: https://docs.spring.io/spring-boot/reference/actuator/observability.html
- Spring Boot, métricas: https://docs.spring.io/spring-boot/reference/actuator/metrics.html
