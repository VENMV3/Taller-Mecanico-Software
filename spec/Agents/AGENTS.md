# Repository Guidelines

## Project Structure & Module Organization

- `backend/`: API REST en Spring Boot 3.5, Java 21, JPA, Spring Security y Flyway.
  - `src/main/java/com/taller/`: controladores, fachadas, repositorios, entidades y validadores.
  - `src/main/resources/db/migration/`: migraciones versionadas (`V1__...sql`). Nunca edites una migración ya aplicada; crea una nueva.
  - `src/test/java/com/taller/portal/`: pruebas JUnit/Spring Boot; `src/test/resources/` contiene fixtures, como vPIC.
- `frontend/src/`: portal React/Vite; vistas PascalCase (`ClientesConsulta.jsx`) y servicios camelCase (`statusApi.js`).
- `scripts/`: importadores y semillas ejecutables. `data/` alberga catálogos locales ignorados por Git.
- `docs/`: documentación funcional y diagramas Archify.

## Build, Test, and Development Commands

Desde la raíz, `./iniciar-taller.sh` inicia MySQL, API y portal; abre `http://localhost:5173`. Usa `./detener-taller.sh` para detener API y Vite sin apagar MySQL.

```bash
cd backend && mvn test          # pruebas Java con H2 y Flyway
cd backend && mvn package       # genera el JAR de Spring Boot
cd frontend && npm run dev      # servidor Vite local
cd frontend && npm run build    # compilación de producción
```

El proyecto configura Maven/Surefire para Byte Buddy; no cambies esa configuración sin una prueba reproducible que lo justifique.

## Coding Style & Naming Conventions

Mantén Java con paquetes `com.taller.portal`, clases PascalCase y métodos/campos camelCase. Conserva **Controller → Facade → Repository**: la fachada concentra autorización, validación y transacciones; los repositorios usan consultas parametrizadas. Documenta clases y métodos públicos con propósito, parámetros, retorno y excepciones.

En React, no renombres `id`, `name`, `data-*` ni clases ya usadas por pruebas. Reutiliza tokens CSS de `frontend/src/index.css`, conserva la paleta oscura/azul y respeta `prefers-reduced-motion` y foco visible.

## Testing Guidelines

Agrega pruebas junto al módulo afectado, con nombres `*Test` y casos descriptivos. Cubre permisos, validación, duplicados, aislamiento por taller y transacciones. Ejecuta `mvn test` y `npm run build` antes de entregar. No conectes pruebas a la base de desarrollo: usa H2, fixtures y transacciones de prueba.

## Commit & Pull Request Guidelines

Sigue el historial: `feat(A7): ...`, `fix: ...`, `style(A8): ...`, `test: ...`, `docs(B4): ...`. Cada commit debe ser pequeño y verificable. En una PR incluye propósito, módulos afectados, pruebas y capturas visuales. No subas `.env`, credenciales, `target/`, `node_modules/`, catálogos descargados ni logs.

## Security & Local Data

No expongas tokens JWT, contraseñas o rutas de carga. Mantén controles de rol también en backend; ocultar una opción en UI no es autorización. Los catálogos SEPOMEX y vPIC se importan localmente y no deben requerir red en tiempo de ejecución.
