# 12. Construcción Full Stack: Solicitudes, Seguridad y Pruebas (Fase II y III)

## a. Gestión de solicitudes, comentarios y notificaciones (Fase II)

- **Base del sistema (S0–S2):** autenticación JWT con BCrypt (`AuthController`), migraciones Flyway de 16 tablas en MySQL (`V1__esquema.sql`, `V2__datos_iniciales.sql`), registro multiparte de solicitudes con código `SOL-AAAA-NNNN` (RN-01), `fecha_limite` (RN-02) y manejo de errores `ProblemDetail`.
- **Bandeja y asignación (S3 — HU-07, HU-08):** `GET /api/solicitudes` paginado con filtros (`estadoId`, `categoriaId`, `prioridadId`, `vencidas`; `PERSONAL_AREA` restringido a su área), `GET /api/solicitudes/asignadas` para técnicos, `GET /api/usuarios/tecnicos?areaId=` y `PUT /api/solicitudes/{id}/asignar` (RN-07).
- **Flujo de estados y evidencias (S3 — HU-09, HU-16):** `GET /api/solicitudes/{id}/transiciones` y `PUT /api/solicitudes/{id}/estado` validando `estado_transicion` (RN-05), propiedad/área (RN-06), motivo obligatorio en rechazos (RN-13), `historial_estado` (RN-14) y `fecha_cierre` (RN-15). Actualización de prioridad (`PUT /api/solicitudes/{id}/prioridad`) y adjuntos adicionales (`POST /api/solicitudes/{id}/evidencias`, `GET /api/evidencias/{id}/descarga`).
- **Comentarios y notificaciones (S6 — HU-10, HU-17):** `GET/POST /api/solicitudes/{id}/comentarios`; eventos post-commit con `@TransactionalEventListener` (RN-16) y endpoints `/api/notificaciones` (listado, contador de no leídas y marcado de lectura).
- **Frontend React:** vistas `BandejaPage.tsx` (`/bandeja`), `AsignadasPage.tsx` (`/asignadas`), `NotificacionesPage.tsx` (`/notificaciones`), campana con contador en `AppLayout.tsx` y controles de transición, asignación, evidencias y comentarios en `SolicitudDetallePage.tsx`.

*[Completar: capturas de `/bandeja`, `/asignadas`, `/solicitudes/:id` y `/notificaciones`.]*

## b. Endpoints implementados

| Endpoint | Método | Roles | HTTP | Función |
|---|---|---|---|---|
| `/api/solicitudes` | `GET` | `SUP, PA, ADM` | `200, 401, 403` | Bandeja filtrable |
| `/api/solicitudes/asignadas` | `GET` | `TEC` | `200, 401, 403` | Casos del técnico |
| `/api/solicitudes/{id}/transiciones` | `GET` | `*` involucrados | `200, 401, 404` | Transiciones válidas |
| `/api/solicitudes/{id}/asignar` | `PUT` | `SUP` | `200, 400, 403, 404` | Asignar técnico |
| `/api/solicitudes/{id}/estado` | `PUT` | `TEC, PA, SUP, SOL` | `200, 400, 403, 409` | Cambiar estado |
| `/api/solicitudes/{id}/prioridad` | `PUT` | `SUP` | `200, 400, 403, 404` | Cambiar prioridad |
| `/api/solicitudes/{id}/evidencias` | `POST` | `*` involucrados | `201, 400, 403, 404` | Subir evidencia |
| `/api/evidencias/{id}/descarga` | `GET` | `*` involucrados | `200, 401, 404` | Descargar archivo |
| `/api/solicitudes/{id}/comentarios` | `GET / POST` | `*` involucrados | `200/201, 400, 404` | Comentarios |
| `/api/notificaciones` | `GET` | `*` autenticado | `200, 401` | Listar notificaciones |
| `/api/notificaciones/no-leidas/cantidad` | `GET` | `*` autenticado | `200, 401` | Contador no leídas |
| `/api/notificaciones/{id}/leida` | `PUT` | `*` autenticado | `204, 401, 404` | Marcar leída |
| `/api/notificaciones/leer-todas` | `PUT` | `*` autenticado | `204, 401` | Marcar todas leídas |

## c. Seguridad OWASP (Fase III)

- **Acceso y BOLA/IDOR:** sesiones *stateless* con JWT, `@EnableMethodSecurity` por rol y validación de propiedad/área en servicio (RN-06, RN-20).
- **Credenciales y fuerza bruta:** contraseñas con BCrypt, secreto JWT en variable de entorno (`JWT_SECRET`), bloqueo de usuarios inactivos (RN-19) y límite de intentos en `/api/auth/login` (HTTP `429`).
- **Cabeceras, CORS y errores:** `X-Content-Type-Options: nosniff`, `X-Frame-Options: DENY`, `Referrer-Policy` y `Content-Security-Policy` en `SecurityConfig.java`; CORS desde variable de entorno (`AppProperties`); errores en `ProblemDetail` sin *stacktraces*.
- **Validación de entradas:** Bean Validation en DTOs y verificación de tipo MIME real y tamaño ≤ 5 MB en adjuntos (RN-12).

*[Completar: captura de cabeceras HTTP de seguridad y respuesta 401/403/429 en `ProblemDetail`.]*

## d. Pruebas automatizadas (Fase III)

- **Unitarias (JUnit 5 + Mockito):** reglas de transición (RN-05), asignación (RN-07), motivo de rechazo (RN-13), fecha de cierre (RN-15) y notificaciones post-commit (RN-16).
- **API y Seguridad (`MockMvc`):** validación de `401` sin token, `403` por rol o solicitud ajena, `200/201` en flujos válidos, `400/409` en transiciones inválidas y `429` en login.
- **Frontend (Vitest + Testing Library):** pruebas de componentes para `BandejaPage`, `AsignadasPage`, `SolicitudDetallePage` y `NotificacionesPage`.

*[Completar: captura de ejecución de `./mvnw test` y `npm test`.]*
