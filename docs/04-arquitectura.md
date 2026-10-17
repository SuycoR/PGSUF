# 04 — Arquitectura

## Vista general

```mermaid
flowchart TB
    U[Usuarios - navegador] --> F[Frontend SPA<br/>React + Vite]
    F -- "HTTPS · JSON · JWT" --> B
    subgraph B[Backend - Spring Boot]
        C[Controladores<br/>@RestController] --> S[Servicios<br/>reglas de negocio]
        S --> R[Repositorios<br/>Spring Data JPA]
        S -. eventos AFTER_COMMIT .-> N[Notificaciones]
    end
    R -- SQL --> DB[(MySQL 9<br/>migraciones Flyway)]
    S --> FS[(Disco: uploads/<br/>evidencias)]
```

| Capa | Responsabilidad | No debe |
|---|---|---|
| Controlador | Recibir HTTP, validar DTO (`@Valid`), delegar al servicio, devolver DTO | Contener reglas de negocio ni acceder a repositorios |
| Servicio | Reglas de negocio, transacciones (`@Transactional`), autorización por propiedad | Conocer HTTP (`HttpServletRequest`, `ResponseEntity`) |
| Repositorio | Acceso a datos con Spring Data JPA | Contener lógica de negocio |
| DTO | Contrato de entrada y salida de la API (Java `record`) | Exponer entidades JPA al cliente |

## Estructura de paquetes

Organización **por funcionalidad, con capas dentro**. Sigue siendo una arquitectura por capas, pero todo lo de una funcionalidad queda junto.

```
backend/
├── mvnw, mvnw.cmd, pom.xml
└── src/main/java/pe/edu/unmsm/servicios/
    ├── ServiciosApplication.java
    ├── config/            ← seguridad, CORS, OpenAPI, almacenamiento
    ├── common/            ← excepciones, manejador global de errores, paginación
    ├── auth/              ← login, registro, JWT
    ├── usuario/
    │   ├── controller/  service/  repository/  dto/  entity/
    ├── catalogo/          ← categoría, tipo de solicitud, prioridad, estado, área, transición
    ├── solicitud/         ← solicitud, evidencia, comentario, historial, secuencia de códigos
    ├── reserva/           ← ambiente, reserva
    ├── notificacion/
    └── dashboard/
src/main/resources/
    ├── application.yml
    └── db/migration/      ← V1__esquema.sql, V2__datos_iniciales.sql, ...
src/test/java/...          ← misma estructura de paquetes
```

```
frontend/
└── app/                   ← React + TypeScript + Vite (no hay prototipo aparte: decisión del equipo)
    └── src/
        ├── api/           ← cliente fetch con el token JWT y errores ProblemDetail
        ├── auth/          ← sesión del usuario (Context + sessionStorage)
        ├── components/    ← componentes reutilizables y layout
        ├── pages/         ← una pantalla por archivo
        ├── routes/        ← rutas, menú por rol y guardas de acceso
        └── styles/        ← tema con la paleta institucional
```

**Paleta institucional** (definida en `src/styles/theme.css`):

| Rol | Color | Hex | Uso |
|---|---|---|---|
| Primario | Azul San Marcos | `#002147` | Barra de navegación, botones principales, encabezados |
| Acento | Oro Viejo | `#C5A059` | Enlace activo, bordes destacados, foco, detalles |
| Fondo | Blanco Puro | `#FFFFFF` | Fondo de página y tarjetas |
| Fondo alternativo | Gris Claro | `#F4F6F9` | Menú lateral y secciones secundarias |
| Texto | Antracita | `#1A202C` | Texto general |

> El Oro Viejo tiene poco contraste sobre blanco (unos 2,4:1), así que **no se usa para texto** sobre fondo blanco: solo para acentos, bordes e indicadores.

**Convención de nombres:** el dominio va en español, igual que las tablas (`Solicitud`, `Reserva`, `TipoSolicitud`). Los sufijos técnicos van en inglés (`SolicitudController`, `SolicitudService`, `SolicitudRepository`, `CrearSolicitudRequest`, `SolicitudResponse`).

## Seguridad

| Aspecto | Decisión |
|---|---|
| Autenticación | JWT firmado (HS256) con `JWT_SECRET`; expiración configurable; sin sesión en servidor (`STATELESS`) |
| Contraseñas | BCrypt |
| Autorización por rol | `@PreAuthorize("hasRole('SUPERVISOR')")` en los controladores |
| Autorización por propiedad | En el servicio (RN-06, RN-20): ser el solicitante, el responsable o del área correspondiente |
| Validación de entradas | Bean Validation en los DTO; validación de archivos por contenido (RN-12) |
| Errores | `ProblemDetail` (RFC 7807) desde un `@RestControllerAdvice`; nunca se devuelve el stack trace |
| CORS | Solo el origen del frontend, configurado por variable de entorno |
| Secretos | Solo en variables de entorno ([08-configuracion.md](08-configuracion.md)) |
| Token en el frontend | `sessionStorage` (se borra al cerrar la pestaña) |

## Decisiones de arquitectura

| # | Decisión | Alternativa descartada | Motivo |
|---|---|---|---|
| D-01 | Spring Boot 3.5.x + Java 21 | Spring Boot 4.0 | Más material de apoyo y ejemplos para un curso de 16 semanas |
| D-02 | Paquetes por funcionalidad con capas dentro | Paquetes por capa (`controller/`, `service/`…) | Más fácil de navegar cuando crece el proyecto |
| D-03 | Estados y prioridades como tablas de catálogo | `ENUM` de MySQL | La guía pide que el administrador los gestione |
| D-04 | Transiciones de estado en la tabla `estado_transicion` | `if` fijos en el servicio | Configurables sin cambiar código; fáciles de probar |
| D-05 | Código único con la tabla `secuencia_codigo` y `SELECT ... FOR UPDATE` | `AUTO_INCREMENT` como código | Numeración por año (`SOL-2026-0001`) sin duplicados |
| D-06 | Bloqueo de la fila del ambiente para validar cruces | Bloqueo de rango sobre `reserva` | Más simple y predecible |
| D-07 | Notificaciones solo dentro del sistema, creadas después del commit | Correo electrónico; creación dentro de la transacción | Alcance del MVP; una falla al notificar no deshace la operación |
| D-08 | El frontend consulta las notificaciones cada 30–60 s | WebSocket o SSE | Menos complejidad para el MVP |
| D-09 | Evidencias en disco, ruta configurable | Guardarlas en MySQL (BLOB) | La base de datos no crece con archivos; migrable a la nube |
| D-10 | Migraciones con Flyway; ningún usuario sembrado | `ddl-auto=update` de Hibernate | Esquema versionado y reproducible; sin credenciales en el repositorio |
| D-11 | Un endpoint `/estado` para todas las transiciones de solicitudes | Endpoints por acción (`/resolver`, `/tomar`) | La lógica de transiciones queda en un solo lugar |

## Dependencias principales (backend)

| Dependencia | Para qué |
|---|---|
| `spring-boot-starter-web` | API REST |
| `spring-boot-starter-data-jpa` | Repositorios y entidades |
| `spring-boot-starter-security` | Autenticación y autorización |
| `spring-boot-starter-validation` | Bean Validation |
| `mysql-connector-j` | Conexión a MySQL |
| `flyway-core` + `flyway-mysql` | Migraciones |
| Librería JWT (por ejemplo `jjwt`) | Generar y validar tokens |
| `springdoc-openapi-starter-webmvc-ui` | Swagger UI |
| `spring-boot-starter-test` | Pruebas |
| `spring-boot-starter-actuator` | Salud y métricas (Fase IV) |
