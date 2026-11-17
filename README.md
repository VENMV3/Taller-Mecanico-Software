# TallerCore — Portal de identidad

Portal de acceso para taller mecánico. Backend REST: Spring Boot, JPA/MySQL, BCrypt y JWT. Frontend: React, Vite, Tailwind CSS y Lucide.

## Ejecutar

La forma más sencilla es copiar `.env.example` como `.env`, ajustar sus valores locales y ejecutar `./iniciar-taller.sh` desde la raíz del proyecto. El script carga `.env`, inicia MySQL, el backend y el frontend, y después se abre en `http://localhost:5173`. El archivo `.env` está ignorado por Git.

Para cerrar el backend y el frontend: `./detener-taller.sh`. MySQL se mantiene activo para no interrumpir otros proyectos.

También puede iniciarse manualmente:

1. MySQL ya se configura en `/home/venmve/mis-contenedores/docker-compose.yml` (BD `mi_base_datos`).
2. Backend: `cd backend && /home/venmve/.local/share/JetBrains/Toolbox/apps/intellij-idea/plugins/maven-plugin/lib/maven3/bin/mvn spring-boot:run`
3. Frontend: `cd frontend && npm install && npm run dev`

El perfil `dev` inicializa los roles y una cuenta técnica local; su contraseña se obtiene exclusivamente de `DEV_TECHNICAL_ACCOUNT_PASSWORD`. No uses esa cuenta ni valores de desarrollo fuera del entorno local.

## Variables de entorno

Para Render, usa `JAVA_TOOL_OPTIONS=-Xmx300m`. El disco del plan gratuito es efímero: las fotografías en `UPLOAD_DIR` se perderán al reiniciar o redesplegar; usa almacenamiento externo antes de producción.

En `dev` y `prod` se requieren las siguientes variables; los valores mostrados son ejemplos falsos:

| Variable | Uso | Ejemplo falso |
| --- | --- | --- |
| `DB_URL` | URL JDBC de MySQL | `jdbc:mysql://localhost:3306/taller_dev` |
| `DB_USER` | Usuario de MySQL | `taller_dev_user` |
| `DB_PASSWORD` | Contraseña de MySQL | `example-local-db-password` |
| `JWT_SECRET` | Material de firma JWT (mínimo 32 caracteres) | `example-local-jwt-signing-key-change-me-1234567890` |
| `DEV_TECHNICAL_ACCOUNT_PASSWORD` | Contraseña de la cuenta técnica, solo `dev` | `example-technical-account-password` |

`prod` no tiene valores predeterminados para las credenciales: si falta `DB_URL`, `DB_USER`, `DB_PASSWORD` o `JWT_SECRET`, Spring Boot detiene el arranque e identifica la variable faltante. En producción Hibernate valida el esquema (`ddl-auto=validate`) y Flyway es la única herramienta que lo gestiona. El perfil `test` usa H2 en memoria y no requiere una base de datos externa.

## API

- `POST /api/auth/register`: registro público, siempre crea un `CLIENTE`.
- `POST /api/auth/login`: autentica con correo y contraseña, devuelve JWT.
- `POST /api/auth/recover`: restablece contraseña con correo o celular (en producción debe enviar/validar OTP antes del cambio).

Las cuentas internas deberán crearse desde un endpoint administrativo posterior protegido para `DUENO` y `DESARROLLADOR`.
