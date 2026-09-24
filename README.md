# TallerCore — Portal de identidad

Portal de acceso para taller mecánico. Backend REST: Spring Boot, JPA/MySQL, BCrypt y JWT. Frontend: React, Vite, Tailwind CSS y Lucide.

## Ejecutar

La forma más sencilla es ejecutar `./iniciar-taller.sh` desde la raíz del proyecto. El script inicia MySQL, el backend y el frontend, y después se abre en `http://localhost:5173`.

Para cerrar el backend y el frontend: `./detener-taller.sh`. MySQL se mantiene activo para no interrumpir otros proyectos.

También puede iniciarse manualmente:

1. MySQL ya se configura en `/home/venmve/mis-contenedores/docker-compose.yml` (BD `mi_base_datos`).
2. Backend: `cd backend && /home/venmve/.local/share/JetBrains/Toolbox/apps/intellij-idea/plugins/maven-plugin/lib/maven3/bin/mvn spring-boot:run`
3. Frontend: `cd frontend && npm install && npm run dev`

La inicialización crea los roles `DUENO`, `MECANICO`, `CLIENTE`, `SECRETARIA` y `DESARROLLADOR`, y una cuenta técnica: `desarrollador@taller.local` / `DevTaller2026!`. Cámbiala en cuanto sea posible.

## API

- `POST /api/auth/register`: registro público, siempre crea un `CLIENTE`.
- `POST /api/auth/login`: autentica con correo y contraseña, devuelve JWT.
- `POST /api/auth/recover`: restablece contraseña con correo o celular (en producción debe enviar/validar OTP antes del cambio).

Las cuentas internas deberán crearse desde un endpoint administrativo posterior protegido para `DUENO` y `DESARROLLADOR`.
