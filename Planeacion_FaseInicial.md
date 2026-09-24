# Documentación de Fase Inicial

> Estado del inventario: 24 de septiembre de 2026. Este documento describe exclusivamente lo que existe en el repositorio; no asume funcionalidades que aún no han sido implementadas.

## 1. Resumen de Credenciales y Base de Datos

### Ubicación de la configuración

La configuración que consume el backend se encuentra en [`backend/src/main/resources/application.yml`](backend/src/main/resources/application.yml). El archivo usa variables de entorno con valores predeterminados locales:

| Propiedad Spring Boot | Variable de entorno | Uso |
| --- | --- | --- |
| `spring.datasource.url` | `DB_URL` | URL JDBC de MySQL y nombre de la base de datos. |
| `spring.datasource.username` | `DB_USER` | Usuario de conexión de MySQL. |
| `spring.datasource.password` | `DB_PASSWORD` | Contraseña de conexión de MySQL. |
| `app.jwt-secret` | `JWT_SECRET` | Clave de firma de tokens JWT. |
| `app.jwt-expiration-ms` | No externalizada actualmente | Duración del token: 86 400 000 ms (24 h). |

Para desarrollo local, el servicio MySQL está definido fuera de este repositorio en `/home/venmve/mis-contenedores/docker-compose.yml`. El backend se conecta a la base `mi_base_datos` y JPA usa `ddl-auto: update`, por lo que crea o ajusta las tablas `roles` y `usuarios`.

### Seguridad de credenciales: acción obligatoria antes de GitHub/producción

El archivo `application.yml` contiene valores predeterminados sensibles para desarrollo y `DataInitializer.java` contiene una cuenta técnica inicial. **No es apto publicar esos secretos ni usarlos en producción.** Antes de subir o desplegar:

1. Eliminar los valores predeterminados sensibles de `application.yml`; exigir variables de entorno.
2. Mover la cuenta técnica inicial a un mecanismo de aprovisionamiento seguro o eliminarla después de crear el primer administrador.
3. Rotar la contraseña actual de MySQL, la clave JWT y la contraseña de la cuenta técnica.
4. Mantener `.env` fuera de Git y usar secretos del proveedor de despliegue.

### Esquema implementado

| Tabla/entidad | Datos persistidos | Relaciones y restricciones |
| --- | --- | --- |
| `roles` / `Role` | `id`, `nombre`, `descripcion` | `nombre` es único. Roles iniciales: `DUENO`, `MECANICO`, `CLIENTE`, `SECRETARIA`, `DESARROLLADOR`. |
| `usuarios` / `User` | `id`, `nombre`, `email`, `celular`, `passwordHash`, `activo`, `emailVerificado`, `creadoEn`, `ultimoAcceso` | `email` y `celular` son únicos; `rol_id` es obligatorio y referencia `roles`. |

Las contraseñas se almacenan con BCrypt en `passwordHash`; no se persiste la contraseña en texto plano.

## 2. Desglose de Módulos por Fase

| Fase | Módulo | Estado | Datos asociados | Descripción técnica |
| --- | --- | --- | --- | --- |
| Inicial | Modelo de identidad y roles | **100% terminado para el alcance actual** | `roles`, `usuarios` | Entidades JPA, claves únicas, rol obligatorio y carga inicial de los cinco roles. |
| Inicial | Registro de clientes | **100% terminado para el flujo básico** | nombre, correo, celular, hash BCrypt, rol `CLIENTE` | `POST /api/auth/register` valida formato, evita duplicados y asigna exclusivamente el rol Cliente. |
| Inicial | Inicio de sesión | **100% terminado para el flujo básico** | correo, hash de contraseña, estado, último acceso | `POST /api/auth/login` valida credenciales y estado activo; emite JWT con correo y rol. |
| Inicial | Interfaz de acceso | **100% terminado para el alcance visual actual** | formularios de acceso, alta y recuperación | SPA React responsive, tema negro con acentos azul eléctrico, mensajes de error y navegación entre formularios. |
| Inicial | Dashboard por rol | **100% terminado como interfaz estática** | nombre y rol en token/respuesta | Los clientes ven vehículos, citas, servicios e historial; los roles internos ven indicadores operativos. Los contadores no consultan datos reales aún. |
| Inicial | Automatización local | **100% terminado** | PIDs y registros locales | `iniciar-taller.sh` levanta MySQL, Spring Boot y Vite. `detener-taller.sh` termina backend/frontend y conserva MySQL activo. |
| Pendiente crítico | Autorización real por JWT y permisos | **Pendiente** | JWT, roles y futuras reglas de permiso | `JwtService` genera tokens, pero falta un filtro que lea `Authorization: Bearer`, autentique el contexto de Spring Security y aplique reglas `hasRole`/`hasAuthority`. No existen endpoints protegidos de negocio todavía. |
| Pendiente crítico | Recuperación segura de contraseña | **Pendiente para producción** | correo/celular, token OTP, expiración, auditoría | `POST /api/auth/recover` cambia la contraseña al identificar correo o celular; no existe OTP, envío de correo/SMS, caducidad ni limitación de intentos. No debe publicarse así. |
| Pendiente | Administración de cuentas internas | **Pendiente** | usuario, rol, auditoría de creador | No existe endpoint ni interfaz para que Dueño/Desarrollador cree Mecánicos, Secretaría, Dueños o Desarrolladores. |
| Pendiente | Operación del taller | **Pendiente** | vehículos, citas, órdenes, servicios, notificaciones | El dashboard presenta lugares reservados; faltan entidades, migraciones, endpoints REST, permisos e interfaz CRUD. |
| Pendiente | Calidad y producción | **Pendiente** | perfiles, pruebas, logs, secretos | Faltan pruebas automatizadas, migraciones versionadas (Flyway/Liquibase), perfiles `dev`/`prod`, rate limiting, observabilidad y CI/CD. |

## 3. Documentación del Código Generado

### Backend: `backend/`

| Archivo o paquete | Responsabilidad |
| --- | --- |
| `pom.xml` | Spring Boot 3.5.6, Web, JPA, Security, Validation, MySQL Connector y JJWT. Java objetivo 21. |
| `PortalTallerApplication.java` | Punto de entrada de Spring Boot. |
| `model/Role.java`, `model/RoleName.java` | Catálogo de roles de la aplicación. |
| `model/User.java` | Identidad del usuario y estado de la cuenta. |
| `repository/RoleRepository.java`, `repository/UserRepository.java` | Acceso JPA a roles y usuarios. |
| `dto/AuthDtos.java` | Contratos y validaciones de login, registro, recuperación y respuesta de autenticación. |
| `service/AuthService.java` | Lógica de registro, login, recuperación y emisión de respuesta. |
| `service/JwtService.java` | Firma y lectura del sujeto de un JWT. |
| `controller/AuthController.java` | API REST bajo `/api/auth`: `login`, `register`, `recover`. |
| `config/SecurityConfig.java` | BCrypt, CORS para desarrollo y sesiones stateless. |
| `config/DataInitializer.java` | Inserta roles y la cuenta técnica local al arrancar si no existen. |

### Frontend: `frontend/`

| Archivo | Responsabilidad |
| --- | --- |
| `package.json` | Scripts Vite (`dev`, `build`) y dependencias React, Tailwind y Lucide. |
| `vite.config.js` | React/Tailwind y proxy local `/api` hacia `http://127.0.0.1:8080`. Ese proxy solo sirve en desarrollo. |
| `src/main.jsx` | Formularios de login/registro/recuperación, llamadas REST y dashboards condicionales por rol. |
| `src/index.css` | Variables y estilos globales del tema negro/azul. |
| `index.html` | Punto de montaje de la SPA. |

### Herramientas locales

| Archivo | Responsabilidad |
| --- | --- |
| `iniciar-taller.sh` | Levanta la base Docker, API y cliente; conserva PIDs y logs en `logs/`. |
| `detener-taller.sh` | Finaliza los procesos registrados de API y cliente. |
| `.gitignore` | Excluye dependencias, artefactos de compilación y registros. |

## 4. Instrucciones de Publicación (Frontend & Backend)

### Arquitectura recomendada

- **Frontend: Vercel.** Es adecuado para la SPA estática de React/Vite y se integra automáticamente con GitHub. Vercel detecta proyectos Vite y construye el resultado estático; cada push puede generar un despliegue de vista previa. Consulte [Vite: Static Deploy / Vercel](https://vite.dev/guide/static-deploy) y [Vercel: Git deployments](https://vercel.com/docs/git).
- **Backend: Render Web Service.** Es adecuado para un servicio Java/Spring Boot conectado a MySQL y permite configurar secretos como variables de entorno. Render puede desplegar desde Git y ejecutar aplicaciones Java o imágenes Docker. Consulte [Render: Docker](https://render.com/docs/docker) y [Spring Boot: Externalized Configuration](https://docs.spring.io/spring-boot/reference/features/external-config.html).
- **Base de datos: MySQL administrado.** No reutilizar el contenedor local para producción. Puede usar una instancia MySQL administrada o desplegar MySQL en Render con volumen persistente en `/var/lib/mysql`; Render documenta este requisito en [Deploy MySQL](https://render.com/docs/deploy-mysql).

### Cambios requeridos antes de desplegar

1. Cambiar el frontend para tomar la URL de API desde `VITE_API_URL` en lugar de usar exclusivamente `'/api/auth'`. En Vercel no existirá el proxy de desarrollo de Vite.
2. Cambiar `server.port: 8080` por `server.port: ${PORT:8080}` en `application.yml`, para que el proveedor asigne el puerto de ejecución.
3. Implementar el filtro JWT, permisos de rol y recuperación con OTP antes de exponer la API públicamente.
4. Quitar valores secretos y la cuenta técnica del código versionado; configurar `DB_URL`, `DB_USER`, `DB_PASSWORD`, `JWT_SECRET` y la URL permitida de CORS como secretos.
5. Sustituir `ddl-auto: update` por migraciones Flyway/Liquibase y una estrategia segura de producción.

### Publicar el frontend en Vercel

1. Subir el repositorio a GitHub.
2. En Vercel, seleccionar **New Project** e importar `VENMV3/Taller-Mecanico-Software`.
3. Indicar **Root Directory**: `frontend`.
4. Confirmar los valores: **Build Command** `npm run build` y **Output Directory** `dist`.
5. Agregar `VITE_API_URL=https://<tu-api>.onrender.com` en las variables de entorno, después de aplicar el cambio requerido de configuración.
6. Desplegar. Los pushes posteriores a la rama de producción desplegarán automáticamente.

### Publicar el backend en Render

1. Crear/provisionar MySQL administrado y guardar sus datos de conexión.
2. En Render, crear **New > Web Service**, conectar el repositorio y definir **Root Directory**: `backend`.
3. Elegir el runtime Java o añadir un `Dockerfile` multi-etapa. Para runtime Java, usar **Build Command** `mvn -DskipTests package` y **Start Command** `java -jar target/portal-taller-1.0.0.jar`.
4. Definir como secretos: `DB_URL`, `DB_USER`, `DB_PASSWORD`, `JWT_SECRET`, `PORT` y el origen CORS de Vercel.
5. Tras aplicar el cambio de `${PORT:8080}`, desplegar y verificar `GET/POST` del API desde el dominio público.
6. Actualizar `VITE_API_URL` en Vercel con el dominio HTTPS del servicio Render y volver a desplegar el frontend.

### Verificación posterior al despliegue

1. Registrar un cliente y comprobar que queda con rol `CLIENTE`.
2. Iniciar sesión y validar la caducidad y firma del token.
3. Confirmar que CORS permite exclusivamente el dominio de Vercel.
4. Verificar que no hay secretos en el historial de Git ni en los logs.
5. Ejecutar pruebas de autorización, recuperación OTP y restricción de creación de cuentas internas cuando esos módulos estén implementados.
