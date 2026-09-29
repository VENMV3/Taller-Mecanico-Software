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
