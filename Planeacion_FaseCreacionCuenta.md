# Documentación de Fase Creación

## 1. Desglose de Módulos por Fase

| Módulo | Estado | Datos asociados | Descripción técnica |
| --- | --- | --- | --- |
| Identidad, roles y login | En desarrollo | `usuarios`, `roles`, JWT | Login con BCrypt y JWT; se añadió filtro Bearer y roles `ADMINISTRADOR_SISTEMA` y `RECEPCIONISTA` para proteger el registro. |
| Registro de cliente | **100% terminado** | `clientes`, fotografía local | Captura los datos solicitados, valida frontend/backend, persiste en transacción y devuelve `Cliente Registrado`. |
| Duplicados e idempotencia | **100% terminado** | correo, teléfonos, nombre+fecha, `idempotencyKey` | Consulta preventiva, restricciones únicas de MySQL y reuso de resultado para la misma clave idempotente. |
| Autorización de registro | **100% terminado** | JWT y rol | La UI oculta la acción a otros roles; la fachada rechaza con 403 a quien no sea Administrador del Sistema o Recepcionista. |
| Fotografía | **100% terminado para almacenamiento local** | `uploads/clientes` | Límite de 15 MB, MIME de imagen y lectura mediante `ImageIO` para comprobar contenido binario. |
| Operación de taller | Pendiente | vehículos, citas, órdenes | El dashboard muestra espacios visuales; no hay CRUD de estos datos. |

## 2. Documentación del Código Generado

- `Cliente`: entidad JPA con restricciones únicas para correo, teléfonos, combinación nombre/fecha y clave idempotente.
- `ClienteRepository`: patrón Repository para persistencia y detección de duplicados.
- `ClienteRegistrationFacade`: patrón Facade y única entrada del caso de uso; autoriza, valida, detecta duplicados, guarda la foto y persiste.
- `ClienteController`: endpoint multipart `POST /api/clientes`; delega en la fachada y devuelve 400/403 de forma visible.
- `JwtAuthenticationFilter` y `SecurityConfig`: construyen el contexto autenticado desde `Authorization: Bearer`.
- `frontend/src/main.jsx`: botón visible solo para roles permitidos y formulario con bloqueo durante el envío, validación local, `FormData` e `Idempotency-Key`.
- Diagrama de componentes: [JSON](docs/diagramas/componentes-registro-cliente.json) y [HTML Archify](docs/diagramas/componentes-registro-cliente.html).

## 3. Instrucciones de Publicación

1. Configurar `DB_URL`, `DB_USER`, `DB_PASSWORD` y `JWT_SECRET` como secretos del entorno; no publicar valores locales.
2. Montar almacenamiento persistente para `uploads/clientes`; el directorio local no es suficiente en instancias efímeras.
3. Configurar el origen permitido de CORS para el frontend productivo.
4. Ejecutar `npm run build` en `frontend` y `mvn -DskipTests package` en `backend` antes del despliegue.
5. Mantener MySQL con respaldo y conservar las restricciones únicas creadas por JPA/migraciones posteriores.

## 4. Mejoras de Interfaz

Se conservó la paleta existente: negro `#050505`, panel `#101010`, azul eléctrico `#1677ff`, zinc y texto claro. Se centralizaron tokens de estado para hover, foco, error, éxito y deshabilitado. `frontend/src/main.jsx` incorpora iconos de campo, spinner, botones con degradado azul derivado, panel visual de marca, secciones con iconos, progreso visual y fotografía circular. `frontend/src/index.css` concentra glassmorphism sutil, sombras en capas, layout responsive, transiciones menores de 250 ms y `prefers-reduced-motion`.

## 5. Resultados de Pruebas

| Prueba | Fallo detectado | Corrección | Estado |
| --- | --- | --- | --- |
| Build de producción frontend | Ninguno | `npm run build` completó el empaquetado. | Aprobado |
| Compilación backend | Ninguno | `mvn -DskipTests package` completó correctamente. | Aprobado |
| Login válido e inválido | API inaccesible en `127.0.0.1:8080` tras reinicio | El backend compila; el proceso local no expuso el puerto durante la sesión de QA. | Bloqueado por entorno |
| 403 y opción oculta por rol | API inaccesible en `127.0.0.1:8080` tras reinicio | No se alteró la condición de rol ni la API. | Bloqueado por entorno |
| Registro, duplicados e idempotencia | API inaccesible en `127.0.0.1:8080` tras reinicio | No se alteró `FormData`, clave idempotente ni backend. | Bloqueado por entorno |
| Archivo inválido o mayor de 15 MB | Ninguno en la validación visual | Se conserva rechazo antes del envío y mensaje visible. | Aprobado por revisión |
| Responsividad 375/768/1280 px | El navegador integrado no adjuntó una pestaña de QA | CSS incluye puntos de ruptura y grids de una/dos columnas; requiere revisión manual local. | Bloqueado por entorno |

## 6. Consulta de Clientes

**Estado: implementado parcialmente; pendiente de aceptación y cierre de QA. No publicado en GitHub.**

- Listado y detalle de solo lectura para `ADMINISTRADOR_SISTEMA` y `RECEPCIONISTA`, igual que registro; los demás roles reciben 403. La navegación reutiliza la misma condición de visibilidad, sin cambiar Login.
- `GET /api/clientes?pagina=0&tamanio=10&busqueda=`: página base cero, tamaño de 1 a 50, búsqueda de hasta 120 caracteres por nombre, teléfono personal o e-mail. Consultas parametrizadas y comodines SQL escapados; proyecciones seleccionan solo las columnas necesarias.
- `GET /api/clientes/{id}`: todos los datos del cliente, sin ruta de archivo ni clave idempotente; 404 si no existe. El e-mail laboral solo se presenta cuando tiene valor.
- `GET /api/clientes/{id}/fotografia`: autorización en fachada, comprobación de ruta dentro de `uploads/clientes`, límite de bytes/píxeles y recodificación PNG; `Cache-Control: no-store` y `X-Content-Type-Options: nosniff`. La UI solicita bytes con Bearer y usa URL temporal revocable, con iniciales como alternativa.
- **Limitación del modelo:** `Cliente` no contiene fecha de registro. Respetando la prohibición de modificar el esquema, se muestra «No disponible» y se ordena por ID descendente. Este orden no equivale a una fecha histórica verificable. Falta decidir si se acepta esta limitación o se autoriza una migración; no se inventan fechas.
- Paleta conservada: fondo `#050505`, panel `#101010`, superficie `#181818`, azul `#1677ff`, hover `#0e63d8`, texto `#f5f7fa` y secundario `#a1a1aa`. Tabla en escritorio, tarjetas móviles, foco visible, carga, vacío, error y reintento; respeta movimiento reducido.
- Archivos: `ClienteConsultaDtos.java`, `ClienteRepository.java`, `ClienteRegistrationFacade.java`, `ClienteController.java`, `frontend/src/ClientesConsulta.jsx`, `frontend/src/clientes-consulta.css`, navegación en `frontend/src/main.jsx`, `backend/pom.xml` y `backend/src/test/java/com/taller/portal/ClienteConsultaTest.java`.
- Solo se añadieron dependencias de pruebas: Spring Boot Test, Spring Security Test y H2. Sin cambios al modelo, validaciones de registro, autenticación, tablas o campos de taller.
- El [diagrama de componentes Archify](docs/diagramas/componentes-registro-cliente.html) fue actualizado para incluir registro, consulta, control JWT/roles, fotografía y catálogo SEPOMEX local. Sus referencias se validaron contra la revisión `62a2d8e`; la evidencia automatizada de navegador queda condicionada a disponer de Chromium en el entorno de Archify.
- Publicación: ejecutar `npm run build` en `frontend` y `mvn verify` en `backend`; reiniciar la API para cargar las nuevas rutas. Mantener API y volumen de fotografías privados, con origen/proxy productivo configurado. No publicar hasta resolver los pendientes de la sección 7.

## 7. Resultados de Pruebas de Consulta

Verificación realizada el 29 de septiembre de 2026. La suite usa H2 aislado y rollback; elimina exclusivamente la fotografía temporal creada por ella. No se agregaron clientes de prueba a MySQL.

| Prueba | Fallo / causa | Corrección | Estado |
| --- | --- | --- | --- |
| Frontend `npm run build` | Ninguno | No requerida | Aprobado |
| Backend `mvn verify`: 5 pruebas de integración | Mockito no pudo adjuntarse dentro del sandbox | Ejecución autorizada fuera del sandbox | Aprobado: 0 fallos, 0 errores |
| Repository: páginas, orden, búsqueda por nombre/teléfono/e-mail e ID inexistente | Ninguno | No requerida | Aprobado en H2 |
| Facade: dos roles permitidos, cinco denegados, sesión ausente, límites y comodines | Ninguno | No requerida | Aprobado |
| HTTP: 403 en listado/detalle/fotografía, 400 en tamaño inválido, 404 inexistente | Ninguno | No requerida | Aprobado con MockMvc y cadena de seguridad real |
| Registro → consulta → dos páginas (11 clientes) → fotografía privada | Ninguno | Edad de fixture calculada con fecha actual | Aprobado con MockMvc/H2; no es E2E de navegador |
| Listado/detalle con MySQL, foto y e-mail laboral ausente | API anterior seguía activa | Reinicio del backend local actualizado | Aprobado en navegador |
| Búsqueda sin coincidencias | Ninguno | No requerida | Aprobado en navegador |
| Listado y detalle a 375, 768 y 1280 px | Ningún desbordamiento horizontal observado | No requerida | Inspección visual realizada; no certifica auditoría WCAG completa |
| Linter existente | No hay script/configuración de linter en el proyecto | No se añadieron herramientas ajenas | No disponible |
| E2E completo: registrar, paginar, tabla totalmente vacía y menú oculto por rol | La sesión de navegador usó administrador y datos existentes; estos casos no se ejecutaron completos | Cubiertos parcialmente por integración; falta recorrido E2E | Pendiente |
| Fecha real de registro | Campo inexistente; requisito incompatible con no cambiar esquema | «No disponible» provisional, ID descendente | Pendiente de decisión |
| Diagrama Archify actualizado y publicación GitHub | Chromium no está disponible para la puerta de navegador de Archify; queda una prueba física de red | El JSON/HTML pasó validación, entrega y comprobación estricta; commit local creado, sin push | Pendiente de cierre |

## 8. Autocompletado de Dirección (catálogo SEPOMEX local)

**Estado: implementado; pendiente únicamente la prueba física de aislamiento de red.** El Registro de Cliente conserva los campos Calle, Colonia, Municipio, Estado y Código Postal. `DireccionAutocompletada` consume únicamente la API autenticada de esta aplicación: al capturar cinco dígitos completa estado/municipio y ofrece colonias; al cambiar Estado limpia los dependientes y habilita la cascada Estado → Municipios → Colonias → CP. Calle, Estado, Municipio y Colonia siguen siendo editables. Las solicitudes se retrasan 300 ms, se cancelan si quedan obsoletas y no bloquean el registro ante catálogo vacío o error.

- `sepomex_asentamientos` es una tabla de catálogo de solo lectura creada por `backend/src/main/resources/db/migration/V1__crear_catalogo_sepomex.sql`. Tiene CP, colonia, tipo, municipio, estado, `c_estado`, `c_mnpio`, restricción única e índices por CP y por `(estado, municipio)`.
- `CodigosPostalesRepository` contiene el único acceso de datos con consultas parametrizadas: CP, estados distintos, municipios y colonias. `CodigosPostalesFacade` autoriza exclusivamente `ADMINISTRADOR_SISTEMA` y `RECEPCIONISTA`, valida formato/longitud y normaliza respuestas. `CodigosPostalesController` expone `GET /api/codigos-postales/*`; otros roles reciben 403.
- La migración se ejecuta con Flyway. Se añadió `flyway-mysql` porque Flyway 11 separa el soporte MySQL de `flyway-core`; no se modificó la tabla `clientes`, sus validaciones ni su registro.
- `scripts/importar_sepomex` acepta TXT o ZIP oficial, detecta ZIP, lee ISO-8859-1, normaliza espacios, procesa el encabezado por nombre y realiza inserciones idempotentes dentro de una transacción. Imprime filas leídas, nuevas y total. El archivo no se versiona (`data/sepomex/*.txt` y `*.zip` están ignorados).

### Descarga e importación

1. Abrir la [Descarga de Códigos Postales de Correos de México](https://www.correosdemexico.gob.mx/SSLServicios/ConsultaCP/CodigoPostal_Exportar.aspx).
2. Elegir **Todos** y formato **TXT**, descargar el archivo y colocarlo como `data/sepomex/CPdescarga.txt`. Aunque el portal use esa extensión, actualmente entrega un ZIP que contiene el TXT; el script admite ambos formatos.
3. Desde la raíz del repositorio ejecutar `./scripts/importar_sepomex data/sepomex/CPdescarga.txt`.
4. Verificar la salida: la primera carga local del 2 de octubre de 2026 leyó 159,342 filas e insertó 159,309 asentamientos únicos; la segunda carga insertó 0 y mantuvo 159,309.
5. Reiniciar la API si estaba ejecutándose para que Flyway aplique la migración. No hace falta internet durante el uso del formulario ni durante la consulta del catálogo ya importado.

Fuente y atribución: Catálogo Nacional de Códigos Postales elaborado por Correos de México / Servicio Postal Mexicano. La página oficial indica que se proporciona gratuitamente para uso particular y prohíbe comercialización total o parcial y distribución a terceros; revisar los términos vigentes en el enlace oficial antes de redistribuirlo.

Diagrama de componentes: [JSON fuente](docs/diagramas/componentes-registro-cliente.json) y [HTML Archify](docs/diagramas/componentes-registro-cliente.html). Representa React, JWT/roles, controladores, fachadas, repositorios, MySQL, fotografía e importación desde archivo local; no incorpora servicio externo en ejecución. Archify aprobó validación, entrega y comprobación estricta contra la revisión `62a2d8e`; su puerta de navegador fue omitida porque el entorno no dispone de Chromium.

## 9. Resultados de Pruebas de Autocompletado

| Prueba | Fallo / causa | Corrección | Estado |
| --- | --- | --- | --- |
| Descarga e inspección oficial | El portal entrega ZIP con extensión `.txt` | El importador detecta la firma ZIP y lee su entrada TXT ISO-8859-1 | Aprobado: 159,344 líneas, 15 columnas |
| Primera importación MySQL | Incompatibilidad `int[][]` de `JdbcTemplate.batchUpdate` | Se aplanaron los resultados por lote | Aprobado: 159,342 leídas, 159,309 nuevas |
| Migración MySQL | Flyway 11 no reconoce MySQL sin módulo específico | Se añadió `flyway-mysql` | Aprobado |
| Segunda importación | Ninguno | No requerida | Aprobado: 0 nuevas, 159,309 total |
| Caracteres especiales | La prueba asumía un orden no garantizado | Valida presencia de «San Ángel» y «Álvaro Obregón» | Aprobado en H2 |
| Repository y Facade | CP válido/inexistente, estado sin municipios, entradas inválidas y rol cliente | Cobertura en `SepomexCatalogoTest` | Aprobado |
| Ruta 403 | Sesión ausente y rol `CLIENTE` | Cobertura MockMvc | Aprobado |
| Build frontend | Ninguno | No requerida | Aprobado con `npm run build` |
| `mvn verify` (8 pruebas) | El sandbox impide a Mockito adjuntar su agente en Java 27 | Se ejecutó fuera del sandbox; no requirió cambio de código | Aprobado: 0 fallos, 0 errores |
| Flujo A navegador | Ninguno | `01000` completó Ciudad de México, Álvaro Obregón y colonia local | Aprobado |
| Flujo B y limpieza | Claves React repetidas en colonias homónimas | Clave visual compuesta por valor e índice | Aprobado: Puebla cargó 217 municipios y 982 colonias; al cambiar Estado limpió dependientes |
| Catálogo vacío/manual, E2E de registro y aislamiento físico de internet | No se vació MySQL de usuario ni se desconectó la red compartida | El código y las consultas usan solo rutas locales; captura manual permanece disponible | Pendiente de prueba aislada no destructiva |
| Diagrama Archify | Primera composición sin espacio para dos etiquetas; Chromium ausente para la puerta de navegador | Se reubicaron componentes; validación, entrega y comprobación estricta aprobadas. La evidencia de navegador queda pendiente por entorno | Parcial: artefactos generados y validados |
| Push a GitHub | Prueba física sin red pendiente | Se creó solo el commit local `62a2d8e`; no se publicó | Pendiente de autorización/cierre |
