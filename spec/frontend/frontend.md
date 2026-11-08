---
name: frontend-react-vite-spec
description: Guía técnica para analizar, diseñar, implementar y validar la interfaz del portal de taller (React + Vite) mediante especificaciones verificables, accesibilidad y alcance controlado.
version: 2.0.0
language: es
related: ../orquestador/orquestador.md, ../skills/skills.md, ../Agents/AGENTS.md
---

# Skill profesional — Interfaz del portal de taller (React + Vite)

## 0. Perfil del frontend

Hechos tomados de `../Agents/AGENTS.md`. Verifícalos con `rg` y lectura de `frontend/package.json` antes de apoyarte en ellos; lo que no esté aquí es **por verificar**, nunca se asume (versiones de React y Vite, uso de TypeScript, enrutador, librería de pruebas, linter).

| Aspecto | Valor |
| --- | --- |
| Ubicación | `frontend/src/` (portal React/Vite en la raíz del repositorio, distinto de este archivo en `spec/frontend/`) |
| Vistas | PascalCase, p. ej. `ClientesConsulta.jsx` |
| Servicios de API | camelCase, p. ej. `statusApi.js` |
| Estilos | Tokens CSS en `frontend/src/index.css`; paleta **oscura/azul**; no se cambia el diseño general |
| Comandos | `cd frontend && npm run dev` (desarrollo), `cd frontend && npm run build` (compilación de producción) |
| Arranque completo | `./iniciar-taller.sh` (MySQL, API y portal en `http://localhost:5173`); `./detener-taller.sh` |
| Contratos | No se renombran `id`, `name`, `data-*` ni clases ya usadas por pruebas o scripts |
| Accesibilidad | `prefers-reduced-motion` respetado y foco visible en todo control |
| Catálogos | SEPOMEX y vPIC se consultan a través del backend local; el navegador no llama a servicios externos |
| Idioma | Textos de interfaz, mensajes y documentación en español |

## 1. Propósito y contrato de aplicación

Actúa como desarrollador frontend senior de React. Construye interfaces mantenibles, accesibles y seguras con Specification-Driven Development: especificar el comportamiento, diseñar la solución mínima suficiente, implementar y verificar contra criterios de aceptación. `../orquestador/orquestador.md` gobierna la ejecución; `../Agents/AGENTS.md` es el contrato del repositorio y prevalece sobre lo genérico; `../skills/skills.md` define el backend, los datos y la seguridad que la interfaz consume. Las rutas de este archivo son relativas a su ubicación (`spec/frontend/`).

**DEBE** indica obligación; **DEBERÍA**, recomendación con excepción justificada; **PUEDE**, opción condicionada al alcance. No se crean funcionalidades ni pantallas adicionales bajo el argumento de «buenas prácticas».

**Activación:** al crear o modificar vistas, componentes, modales, formularios, servicios de API, estilos o textos de interfaz. Un cambio puramente visual no toca lógica de negocio, endpoints ni base de datos. Para una consulta conceptual, responde sin generar código.

**Entradas mínimas:** resultado observable esperado; rama y estado de Git; vista y componentes existentes a reutilizar; contrato HTTP real consumido (rutas, DTO, códigos); roles que ven y usan la pantalla; estados a cubrir (carga, vacío, error). Si falta un dato, separa supuesto reversible de decisión bloqueante. Nunca inventes endpoints, campos, roles ni resultados de pruebas.

## 2. SPEC para interfaz

Usa el protocolo SPEC y la plantilla de `../skills/skills.md` (sección 2). Para interfaz, los criterios de aceptación deben incluir, cuando apliquen:

- **Por rol:** qué ve y qué puede hacer cada rol (Administrador, Recepcionista) y qué ocurre si el backend responde 403.
- **Por estado:** comportamiento en carga, vacío, error de red, 400, 404 y 409.
- **Por dispositivo:** 375, 768 y 1280 px.
- **Accesibilidad:** teclado, foco, ARIA y contraste.

Para un cambio pequeño, la SPEC puede ser una lista de AC en la respuesta.

## 3. Base tecnológica y dependencias

1. Inspecciona `frontend/package.json` y la configuración de Vite antes de decidir; conserva las versiones del proyecto y no migres a TypeScript, a otro enrutador ni a otra librería de estado sin alcance autorizado.
2. **No añadas dependencias** (librerías de UI, de formularios, de iconos, de estado, de fechas) salvo necesidad demostrada para un requisito, con una línea de justificación. Primero reutiliza lo que ya hay en `frontend/src/`.
3. No incluyas secretos ni URLs privadas en el código del cliente ni en variables `VITE_*`: todo lo que entra en el bundle es público.
4. Compila con `npm run build` antes de entregar; un build en rojo no se entrega.

## 4. Arquitectura de la interfaz

- **Vistas** (`PascalCase.jsx`): componen la pantalla, gestionan el estado local y coordinan servicios; no contienen reglas de negocio autoritativas.
- **Servicios de API** (`camelCaseApi.js`): único lugar donde se llama al backend (rutas, encabezados, manejo de la respuesta). Las vistas no usan `fetch` directamente.
- **Componentes reutilizables:** extrae un componente solo cuando se usa en más de un lugar o cuando aísla una responsabilidad clara (modal, insignia de estatus, tabla paginada). No crees capas ni abstracciones sin variación real.
- **Estado:** el estado derivado se calcula, no se duplica; el estado de servidor (listas, página actual, filtros) se recarga tras escribir en lugar de mantenerse a mano cuando sea más simple y seguro.
- **Reutilización obligatoria:** validadores, normalizadores y autocompletado SEPOMEX del registro de cliente se reutilizan en edición y en vehículos; **no los dupliques**.
- Una sola página de gestión de clientes con filtro de estatus (Activos / Suspendidos / Todos, este último solo para el Administrador); no crees pantallas paralelas para lo que el listado ya resuelve.

## 5. Integración con la API

### Contrato y errores
Consume el contrato definido en `../skills/skills.md` (sección 5). La interfaz traduce cada respuesta:

| Respuesta | Comportamiento de la UI |
| --- | --- |
| 200/201 | Confirmación visual breve; actualiza la fila o recarga la lista sin recargar la página |
| 400 | Errores **por campo**, junto al campo correspondiente y asociados con `aria-describedby` |
| 401 | Redirige al inicio de sesión usando el mecanismo existente; no cambies la autenticación |
| 403 | Mensaje claro («no tienes permiso para esta acción»); cierra el modal si aplica |
| 404 | Mensaje claro; cierra el modal y recarga la lista |
| 409 | Muestra **qué dato está duplicado** o por qué la operación no procede (ya suspendido, estatus en uso, estatus base protegido), junto al campo cuando corresponde |
| Red / 5xx | Mensaje genérico con opción de reintentar; no muestres detalles técnicos ni stack traces |

No muestres mensajes del servidor sin revisar que son seguros; no expongas tokens, rutas internas ni datos personales en la consola ni en el DOM.

### Listados
- La paginación y el orden son **del servidor**; la UI solicita página y orden y no ordena ni filtra todo en el cliente. El tamaño máximo es 10 por página y lo impone el servidor.
- El orden se ofrece solo por las columnas permitidas; el indicador de flecha refleja el orden vigente.
- La búsqueda usa *debounce* y cancela la petición anterior (`AbortController`) para evitar respuestas fuera de orden.
- Muestra el total de resultados y el estado de carga durante la consulta.

### Escrituras
- **Doble envío:** el botón se deshabilita y muestra estado de carga mientras la petición está en vuelo; un segundo clic no genera una segunda solicitud.
- Tras un 409 o 403, el formulario conserva lo que el usuario escribió.
- Los permisos que la UI usa para mostrar u ocultar acciones provienen del usuario autenticado, no de datos editables del cliente.

## 6. Formularios y validación

- **Mismas reglas que el registro:** teléfono (10 dígitos con el formato del validador existente), e-mail, e-mail de trabajo opcional, fecha de nacimiento y edad coherentes, código postal, longitudes máximas. Reutiliza las funciones existentes.
- La edad se calcula a partir de la fecha de nacimiento y debe ser coherente; el formulario de edición no permite editar fotografía ni estatus.
- Normaliza a la vista igual que el backend (recortar espacios, VIN en mayúsculas, placa en minúsculas con espacios colapsados), pero **el backend siempre revalida**: la validación del cliente mejora la experiencia, nunca sustituye a la del servidor.
- Dirección con autocompletado SEPOMEX existente (CP → colonia, municipio, estado).
- **Vehículo:** marca y modelo con autocompletado en cascada contra el backend local; si el valor no aparece, se permite captura manual con validación y longitud máxima. VIN con `^[A-HJ-NPR-Z0-9]{17}$` (sin I, O, Q), año entre 1950 y el año actual + 1, kilometraje opcional entero ≥ 0.
- Campo de taller visible **solo para el Administrador**; para el Recepcionista no se renderiza.
- Etiquetas siempre visibles, ayudas cortas, obligatorios señalados, rejilla de dos columnas en escritorio y una en móvil.

## 7. Modales y accesibilidad

- Modales con `role="dialog"`, `aria-modal="true"`, `aria-labelledby` (y `aria-describedby` cuando haya texto de apoyo).
- **Foco atrapado** dentro del modal; `Esc` lo cierra; al cerrarse, el foco **vuelve al elemento que lo abrió**; el fondo no recibe foco.
- Navegación completa por teclado; foco visible en todos los controles.
- Contraste mínimo AA; el estado (Activo, Suspendido, Cancelado) nunca se comunica **solo por color**: la insignia lleva texto.
- Botones de icono con nombre accesible (`aria-label`) y tooltip; acciones peligrosas (Suspender) visualmente distinguibles; confirmaciones en modal para suspender y reactivar.
- Avisos de resultado con `role="status"` o `aria-live` para que lectores de pantalla los anuncien.
- Textos de interfaz en español, claros y consistentes en todo el sitio.

## 8. Diseño visual y responsivo

- Conserva la paleta oscura/azul y los tokens de `frontend/src/index.css`; añade tokens o clases nuevas en lugar de reemplazar las existentes.
- Responsivo en **375, 768 y 1280 px**. En 375 px, la tabla pasa a tarjetas apiladas o a desplazamiento horizontal contenido, sin romper la página.
- Transiciones ≤ 250 ms y desactivadas o reducidas con `prefers-reduced-motion`.
- Todo listado y formulario cubre los estados de **carga** (esqueleto o indicador), **vacío** (mensaje y sugerencia) y **error** (mensaje y reintento).
- Tablas legibles: espaciado, hover y cebra sutiles, columnas ordenables con indicador, paginación clara con el total.
- Un rediseño visual no toca lógica, endpoints ni base de datos.

## 9. Seguridad en el cliente

1. **Ocultar una opción no es autorización.** Cada acción protegida se valida en el backend (403); la UI solo oculta lo que el rol no puede usar.
2. No uses `dangerouslySetInnerHTML` con datos del usuario o del servidor; escapa por defecto (React lo hace en JSX).
3. No registres en consola tokens JWT, contraseñas, rutas de carga ni datos personales; no los pongas en URLs, en `data-*` ni en el DOM visible.
4. No almacenes secretos en el bundle; todo `VITE_*` es público.
5. Los catálogos (SEPOMEX, vPIC) se consultan siempre a través del backend local; el navegador no depende de la red externa en ejecución.
6. Conserva el mecanismo de autenticación existente y sus protecciones (CSRF, CORS) sin cambiarlos salvo alcance expreso.
7. Los datos de demostración y las capturas para documentación no deben contener datos personales reales.

## 10. Rendimiento proporcional

- Mide antes de optimizar; no introduzcas memoización ni librerías de rendimiento por reflejo.
- Listados paginados por el servidor; evita renderizar cientos de filas.
- *Debounce* en búsqueda y autocompletado (vPIC, SEPOMEX) y límite de resultados en el cliente de autocompletado.
- Evita re-renderizados evidentes (estado elevado sin necesidad, objetos recreados que rompen comparaciones) solo cuando haya un problema observable.
- Carga diferida de vistas pesadas solo si existe una necesidad medida y ya hay enrutador.

## 11. Estrategia de validación

| Nivel | Qué verificar | Cómo en este proyecto |
| --- | --- | --- |
| Compilación | La interfaz compila | `cd frontend && npm run build` |
| Linter | Estilo y errores estáticos | El script del proyecto, **si existe** en `package.json`; no instales uno nuevo |
| Pruebas de UI | Render por rol, selector de taller solo para Administrador, estados | La librería que ya use el proyecto, **si existe**; si no hay infraestructura, no instales nada |
| Contrato | La UI maneja 400/403/404/409 como en la sección 5 | Revisión del servicio de API y la vista; prueba manual contra la API en ejecución |
| Accesibilidad y responsivo | Teclado, foco, ARIA, contraste AA, 375/768/1280 px | Lista de verificación manual |

Cuando no haya infraestructura de pruebas de UI, entrega una **lista de verificación manual** (8 a 10 puntos) con: los tres anchos, navegación por teclado y foco en modales, contraste, estados vacío/carga/error, comportamiento por rol, doble envío y manejo del 409. Lo que no se pudo comprobar se declara como pendiente, nunca como aprobado. Las pruebas de backend (`cd backend && mvn test`) no validan la interfaz.

## 12. Definition of Done

- [ ] Cada requisito de interfaz tiene criterios de aceptación y evidencia.
- [ ] El diff contiene solo cambios solicitados o indispensables; no se renombró `id`, `name`, `data-*` ni clases usadas por pruebas o scripts.
- [ ] `cd frontend && npm run build` en verde (y `mvn test` en verde si se tocó el backend).
- [ ] Estados de carga, vacío y error cubiertos; 400/403/404/409 manejados con mensajes claros.
- [ ] Permisos reflejados por rol, sin sustituir la autorización del backend.
- [ ] Modales accesibles (foco atrapado, `Esc`, ARIA); contraste AA; `prefers-reduced-motion` respetado.
- [ ] Responsivo en 375, 768 y 1280 px; lista de verificación manual entregada si no hay pruebas de UI.
- [ ] Sin dependencias nuevas ni secretos en el diff; documentación afectada actualizada.
- [ ] Tabla de fases y resumen conforme a `../orquestador/orquestador.md`; commit local con la convención del historial (`feat(A5-2): ...`, `style(A8): ...`) y sin `push`.

No declares terminada una tarea con criterios obligatorios fallidos o sin verificar.

## 13. Referencias

Referencias generales de apoyo; verifica la versión de la documentación contra la de `frontend/package.json` antes de copiar APIs o configuración. Las reglas SPEC y de alcance son convenciones de estos documentos.

- React, documentación oficial: https://react.dev
- Vite, documentación oficial: https://vite.dev
- WAI-ARIA Authoring Practices, patrón de diálogo modal: https://www.w3.org/WAI/ARIA/apg/patterns/dialog-modal/
- WCAG 2.2: https://www.w3.org/TR/WCAG22/
- MDN, `prefers-reduced-motion`: https://developer.mozilla.org/en-US/docs/Web/CSS/@media/prefers-reduced-motion
