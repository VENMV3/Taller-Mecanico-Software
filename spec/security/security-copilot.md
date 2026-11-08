---
name: security-copilot-revision-seguridad
description: Cómo usar Microsoft Security Copilot como agente externo de revisión de seguridad del portal de taller, qué evidencia prepararle, qué instrucciones darle y cómo tratar sus hallazgos dentro del flujo SPEC.
version: 1.0.0
language: es
related: ../orquestador/orquestador.md, ../skills/skills.md, ../frontend/frontend.md, ../Agents/AGENTS.md
---

# Agente de seguridad — Microsoft Security Copilot

## 1. Qué es y qué no es (leer primero)

Microsoft Security Copilot es un servicio en la nube de Microsoft, con su propio portal (`securitycopilot.microsoft.com`) y ligado a un tenant de Microsoft. **No se instala dentro del repositorio** ni lo ejecuta el agente de programación como una herramienta local. En este proyecto se integra así:

| Pieza | Dónde vive | Quién la opera |
| --- | --- | --- |
| Agente de programación (el que usa `orquestador.md` y `skills.md`) | Entorno local, sin red | Tú, con el agente |
| Agente de Security Copilot (revisor de seguridad) | Tu tenant de Microsoft | Tú, desde su portal |
| Paquete de evidencia (texto, sin secretos ni datos reales) | `docs/seguridad/` del repositorio | El agente de programación lo genera; tú lo subes |
| Hallazgos | Se registran como propuestas (CR) y entran al flujo SPEC | Tú decides; el agente de programación implementa lo aprobado |

El agente de programación **no llama** a Security Copilot ni asume su respuesta. Un hallazgo de Security Copilot es una **hipótesis a verificar en el código**, no un hecho ni una orden.

## 2. Requisitos (verifícalos tú; el agente no puede)

1. Un tenant de Microsoft con acceso a Security Copilot y la licencia o capacidad que tu plan exija. Revisa los costos en la documentación oficial («Cost considerations») antes de activar nada.
2. Permisos en el portal. Según la documentación consultada, un **Copilot owner** o un **Contributor** instala y administra agentes, y un **Analyst** puede ejecutarlos una vez configurados.
3. Para un agente propio (custom), la documentación describe varias vías de construcción: lenguaje natural, formulario del *agent builder*, subida de un YAML/manifiesto o MCP. Se prueba antes de publicar y se publica con alcance «solo yo» o «todos en el workspace». Para empezar usa **solo yo**.
4. Los plugins que un agente requiera los habilita un administrador. Un agente sin sus plugins no funciona.

Documentación consultada el 3 de noviembre de 2026; la interfaz, los nombres y las licencias cambian con frecuencia, así que verifica contra la fuente oficial (sección 9).

Este proyecto es de desarrollo local, sin dependencia de red. Nada de este archivo obliga a conectar la aplicación a servicios de Microsoft: la revisión se hace **fuera de línea** con evidencia exportada.

## 3. Reglas de datos (no negociables)

El portal maneja datos personales de clientes (nombre, teléfono, e-mail, dirección, fecha de nacimiento). Por eso:

1. **Nunca subas** a Security Copilot: contenido de `.env`, credenciales, tokens JWT, claves, volcados de la BD, filas reales de clientes, catálogos descargados, logs con datos personales ni capturas con datos reales.
2. El paquete de evidencia contiene solo **estructura**: rutas, roles, nombres de tablas y columnas, versiones de dependencias y fragmentos de código sin secretos.
3. Los ejemplos usan datos ficticios.
4. Revisa las obligaciones de protección de datos personales que apliquen a tu taller (por ejemplo, la normativa mexicana vigente para particulares). Este archivo no es asesoría legal.
5. Si dudas de si algo es sensible, no lo incluyas.

## 4. Flujo de trabajo

1. **Preparar evidencia** (agente de programación, sin red): genera el paquete de la sección 5 con el prompt de la sección 7.
2. **Revisar el paquete** (tú): confirma que no tiene secretos ni datos reales antes de subirlo.
3. **Ejecutar la revisión** (tú, en el portal de Security Copilot): usa las instrucciones de la sección 6 con el paquete.
4. **Registrar hallazgos** (tú, con el agente de programación): cada hallazgo pasa a la tabla de la sección 8 y se clasifica.
5. **Verificar en el código** (agente de programación): confirma o descarta cada hallazgo con `rg`, lectura y prueba. Un hallazgo no verificado no se corrige.
6. **Implementar lo aprobado** (agente de programación): sigue el flujo SPEC de `../orquestador/orquestador.md`, una tarea por hallazgo aprobado, con su prueba de regresión.

## 5. Paquete de evidencia (`docs/seguridad/paquete-evidencia.md`)

Debe contener, generado a partir del repositorio real (no de memoria):

| Sección | Contenido |
| --- | --- |
| Mapa de endpoints | Método, ruta, rol requerido, validación de taller (sí/no) y códigos de error, extraído de los Controllers y las Facades |
| Matriz de permisos | Acción × rol (Administrador, Recepcionista, Desarrollador si existe), tal como está en el código |
| Autenticación y sesión | Mecanismo existente (JWT/sesión), CSRF, CORS, expiración; **sin valores secretos** |
| Modelo de datos | Tablas, columnas, claves, restricciones únicas e índices, soft delete (`is_deleted`, `deleted_at`, `deleted_by`) |
| Migraciones | Lista de versiones Flyway con una línea de propósito |
| Dependencias | Versiones de `backend/pom.xml` y `frontend/package.json`, sin enlaces a repositorios privados |
| Configuración | Nombres de variables y propiedades, **sin valores** |
| Registros y errores | Qué se registra, qué se devuelve al usuario y si se ocultan causas |
| Scripts | Importadores y siembras: qué hacen, si son idempotentes y qué credenciales usan (solo el nombre de la variable) |
| Límites conocidos | Lo que no se pudo extraer o verificar |

## 6. Instrucciones para el agente de Security Copilot

Texto para pegar en la vía de **lenguaje natural** del *agent builder* (ajústalo a lo que pida el asistente de construcción). No se incluye un manifiesto YAML porque su esquema debe tomarse de la documentación vigente.

```text
Nombre: Revisor de seguridad — Portal de taller

Objetivo: Revisar el paquete de evidencia de una aplicación web (API Spring Boot,
interfaz React, base de datos MySQL) y reportar riesgos de seguridad verificables.

Entradas: Un documento con endpoints, roles, matriz de permisos, modelo de datos,
dependencias, configuración sin valores y errores. No recibirás secretos ni datos reales.

Qué revisar, en este orden:
1. Autorización: ¿cada acción protegida se valida en el servidor y no solo en la interfaz?
   ¿El alcance por taller se deduce del usuario autenticado y no de parámetros del cliente?
   ¿Hay cambios de recurso entre talleres o escalada de rol posibles?
2. Autenticación y sesión: manejo de tokens, expiración, CSRF, CORS.
3. Integridad de datos: restricciones únicas, duplicados que incluyan registros con borrado
   lógico, transiciones de estado atómicas y carreras (suspender/reactivar).
4. Exposición de datos: datos personales en respuestas, logs y mensajes de error.
5. Entradas: validación, consultas parametrizadas, orden por columnas de lista blanca,
   límites de paginación impuestos por el servidor.
6. Dependencias: versiones con vulnerabilidades conocidas o fuera de soporte.
7. Configuración y secretos: nombres de variables, perfiles y valores por defecto inseguros.

Salida: una tabla con ID, severidad (crítica/alta/media/baja/informativa), componente,
evidencia citada del documento, riesgo, recomendación y nivel de confianza.
Separa claramente lo que está demostrado por la evidencia de lo que es hipótesis.

Restricciones:
- Solo lectura: no ejecutes acciones, no modifiques recursos y no contactes sistemas externos.
- No inventes endpoints, tablas ni versiones que no estén en la evidencia; si falta información,
  dilo como «no verificable con esta evidencia».
- No reportes como hallazgo algo que el documento indica como ya mitigado sin justificar por qué no lo está.
- Responde en español.
```

Para un primer uso, prueba el agente con el paquete y compara sus conclusiones contra el código antes de confiar en él para otra cosa.

## 7. Prompt para el agente de programación (generar el paquete)

Va en la raíz del repositorio; no requiere red. Sustituye `<rama>`.

```text
Rama <rama>. Responde en español.

Lee spec/Agents/AGENTS.md, spec/orquestador/orquestador.md, spec/skills/skills.md
y spec/security/security-copilot.md, y aplícalos durante esta tarea.

NO audites ni hagas tablas de estado de lo ya hecho. Tu primera respuesta debe ser trabajo real:
lecturas y luego el archivo creado.

TAREA ÚNICA: generar docs/seguridad/paquete-evidencia.md siguiendo la sección 5 de
spec/security/security-copilot.md. Solo documentación: no cambies código, migraciones,
permisos ni configuración.

LEE (localiza con rg; máx. 12 archivos): Controllers y Facades (rutas y roles), la configuración de
seguridad, entidades y migraciones, backend/pom.xml, frontend/package.json y los scripts de importación.

REGLAS DE DATOS:
- Extrae todo del código real; no inventes endpoints, roles ni versiones. Si no puedes verificarlo, escribe
  «no verificado».
- NO incluyas valores de .env, credenciales, tokens, claves, cadenas de conexión con contraseña, filas de la BD
  ni datos personales reales. De la configuración incluye solo los NOMBRES de propiedades y variables.
- Ejemplos con datos ficticios.
- Antes de terminar, revisa el archivo con rg buscando patrones de secretos (password, secret, token, key,
  Bearer, jdbc con credenciales) y elimina cualquier valor encontrado.

NO HAGAS: cambios de código, dependencias nuevas, ejecutar herramientas de red ni push.

CIERRE: mvn test no es necesario (solo documentación), pero confirma con git status que solo cambió
docs/seguridad/. Commit local: docs(sec): paquete de evidencia para revisión de seguridad.

RESPUESTA FINAL (máx. 8 líneas): hash, secciones del paquete, qué quedó «no verificado» y el resultado de la
revisión de secretos.
```

## 8. Tratamiento de hallazgos

Cada hallazgo de Security Copilot se registra y clasifica **antes** de tocar código:

| ID | Severidad | Componente | Evidencia (archivo:línea) | ¿Verificado en el código? | Clase | Decisión |
| --- | --- | --- | --- | --- | --- | --- |
| SEC-001 | alta | Facade de clientes | `<ruta real>` | sí / no / parcial | Indispensable / Opcional / Ajena (ver §5 del orquestador) | Aprobado / Rechazado / Pospuesto |

Reglas:

1. Un hallazgo **no verificado** no se corrige; se pide más evidencia o se descarta.
2. Un hallazgo verificado se trata como **CR** hasta que lo apruebes; luego entra al flujo SPEC con su `REQ`, `AC`, `TASK` y prueba de regresión.
3. Una corrección de seguridad no autoriza refactors ni cambios de diseño ajenos; la frontera de alcance del orquestador se mantiene.
4. Los hallazgos críticos o altos verificados en autorización, aislamiento por taller o exposición de datos se atienden primero.
5. Nunca se debilita un control (permiso, validación, restricción única) para reducir un hallazgo.
6. Las correcciones se confirman con una prueba que falle sin el arreglo y pase con él, y con `cd backend && mvn test` en verde (y `cd frontend && npm run build` si hubo interfaz).

## 9. Referencias

- Documentación para desarrolladores de Security Copilot (construir, probar y publicar agentes): https://learn.microsoft.com/copilot/security/developer
- Publicar un agente (alcance «solo yo» o «workspace», roles y Security Store): https://learn.microsoft.com/copilot/security/developer/publish-agent-dev
- Portal de Security Copilot: https://securitycopilot.microsoft.com

Las reglas de datos, el flujo y la clasificación de hallazgos son convenciones de este proyecto, no de Microsoft.
