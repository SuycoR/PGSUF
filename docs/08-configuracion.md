# 08 — Configuración y entorno local

## Requisitos

| Herramienta | Versión | Notas |
|---|---|---|
| JDK | 21 o superior | El proyecto compila para Java 21 (`release 21`). Sirve cualquier JDK 21+; en la máquina de desarrollo actual hay un JDK 22 en `C:\Program Files\Java\jdk-22`. Definir `JAVA_HOME` apuntando a él |
| Maven | No hace falta | El proyecto trae Maven Wrapper (`mvnw` / `mvnw.cmd`) |
| Node.js | 22.12 o superior | Para el frontend con Vite. React Router 8 exige Node 22.22+, por eso el proyecto usa React Router 7 |
| MySQL | 9.x | Servidor local en `127.0.0.1:3306` |
| Git | Cualquiera reciente | |

## Regla de oro: ningún secreto en el repositorio

- Contraseñas, `JWT_SECRET` y credenciales **solo** en variables de entorno.
- En el repositorio va un `.env.example` **sin valores**. El `.env` real está en `.gitignore`.
- Si un secreto llegó a un commit, se considera comprometido: hay que **cambiarlo**, no basta con borrarlo del historial.

## Variables de entorno

| Variable | Ejemplo | Descripción |
|---|---|---|
| `DB_URL` | `jdbc:mysql://127.0.0.1:3306/servicios_universitarios?serverTimezone=America/Lima` | URL JDBC |
| `DB_USER` | `app_servicios` | Usuario de la aplicación (no `root`) |
| `DB_PASSWORD` | — | Contraseña del usuario de la aplicación |
| `JWT_SECRET` | — | Clave de al menos 32 bytes, en Base64 |
| `JWT_EXPIRATION_MINUTES` | `60` | Duración del token |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:5173` | Origen del frontend |
| `UPLOAD_DIR` | `./uploads` | Carpeta de evidencias |
| `ADMIN_EMAIL` | — | Correo del administrador inicial |
| `ADMIN_PASSWORD` | — | Contraseña del administrador inicial |
| `SPRING_PROFILES_ACTIVE` | `dev` | `dev`, `test` o `prod` |

`.env.example`:

```dotenv
DB_URL=jdbc:mysql://127.0.0.1:3306/servicios_universitarios?serverTimezone=America/Lima
DB_USER=
DB_PASSWORD=
JWT_SECRET=
JWT_EXPIRATION_MINUTES=60
CORS_ALLOWED_ORIGINS=http://localhost:5173
UPLOAD_DIR=./uploads
ADMIN_EMAIL=
ADMIN_PASSWORD=
SPRING_PROFILES_ACTIVE=dev
```

Generar un `JWT_SECRET` con un generador criptográficamente seguro (Git Bash, que trae OpenSSL):

```bash
openssl rand -base64 48
```

> No usar `Get-Random` de PowerShell ni `Math.random()`: no son criptográficamente seguros.

## Base de datos

Crear la base y un usuario propio para la aplicación (ejecutar una sola vez como `root`). Reemplazar `<contraseña>` en el momento; **no guardar este comando con la contraseña escrita**:

```sql
CREATE DATABASE servicios_universitarios
  CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;

CREATE USER 'app_servicios'@'localhost' IDENTIFIED BY '<contraseña>';
GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, ALTER, INDEX, REFERENCES, DROP
  ON servicios_universitarios.* TO 'app_servicios'@'localhost';
```

> El permiso `DROP` es para que Flyway pueda gestionar el esquema en desarrollo. En producción se separa el usuario de migraciones del usuario de la aplicación.

Las tablas **no se crean a mano**: Flyway ejecuta las migraciones de `src/main/resources/db/migration/` al arrancar.

## `application.yml`

```yaml
spring:
  datasource:
    url: ${DB_URL}
    username: ${DB_USER}
    password: ${DB_PASSWORD}
  jpa:
    hibernate:
      ddl-auto: validate      # Flyway crea el esquema; Hibernate solo valida
    open-in-view: false
  flyway:
    enabled: true
  servlet:
    multipart:
      max-file-size: 5MB
      max-request-size: 25MB
  mvc:
    problemdetails:
      enabled: true

app:
  jwt:
    secret: ${JWT_SECRET}
    expiration-minutes: ${JWT_EXPIRATION_MINUTES:60}
  cors:
    allowed-origins: ${CORS_ALLOWED_ORIGINS}
  upload-dir: ${UPLOAD_DIR:./uploads}
  admin:
    email: ${ADMIN_EMAIL:}
    password: ${ADMIN_PASSWORD:}

springdoc:
  swagger-ui:
    path: /swagger-ui.html
```

En el perfil `prod`, Swagger se desactiva (`springdoc.api-docs.enabled: false`).

## Administrador inicial

No se siembra ningún usuario en las migraciones. Al arrancar, un `CommandLineRunner` verifica si existe algún `ADMIN`:

- Si no existe y `ADMIN_EMAIL` y `ADMIN_PASSWORD` están definidas → lo crea con la contraseña cifrada con BCrypt.
- Si ya existe → no hace nada.
- Nunca escribe la contraseña en el log.

## Levantar el proyecto

Backend (desde `backend/`, con las variables de entorno cargadas):

```powershell
.\mvnw.cmd spring-boot:run
```

- API: `http://localhost:8080/api`
- Swagger: `http://localhost:8080/swagger-ui.html`

Frontend React (desde `frontend/app/`):

```powershell
npm install
npm run dev
```

- App: `http://localhost:5173`. En desarrollo, Vite reenvía `/api` al backend (proxy en `vite.config.js`).

## Perfiles

| Perfil | Uso | Diferencias |
|---|---|---|
| `dev` | Desarrollo local | Swagger activo, logs en nivel DEBUG para el paquete del proyecto |
| `test` | Pruebas automatizadas | Base de datos de pruebas (MySQL en Testcontainers o una base `_test`) |
| `prod` | Despliegue | Swagger desactivado, logs en INFO, HTTPS |
