# Registro de uso de IA

Requisito transversal de la guía del curso: por cada uso significativo de IA se registra la herramienta, el prompt, el resultado, si se usó, cómo se validó, las modificaciones y el responsable. El objetivo es demostrar que la IA se usó como herramienta de ingeniería y no como sustituto del aprendizaje.

## Plantilla

Copiar este bloque para cada registro nuevo:

```markdown
### IA-NN — <título corto>

| Campo | Registro |
|---|---|
| Fecha | AAAA-MM-DD |
| Herramienta | |
| Prompt | |
| Resultado generado | |
| ¿Se utilizó? | Sí / Parcialmente / No |
| Validación realizada | |
| Modificaciones | |
| Responsable | |
```

---

### IA-01 — Informe parcial (Fase I)

| Campo | Registro |
|---|---|
| Fecha | 2026-10-17 |
| Herramienta | Claude (Claude Code) |
| Prompt | Resumir lo que pide la primera presentación según la guía del curso y completar el informe parcial: actores, clasificación de solicitudes, requerimientos, épicas de reservas y trámites, arquitectura preliminar y wireframes. |
| Resultado generado | Secciones del informe, tablas de requerimientos, historias con escenarios BDD, diagrama de arquitectura y wireframes. |
| ¿Se utilizó? | Parcialmente: se ajustó a las decisiones del equipo (stack de la guía y tipos de solicitud definidos por el grupo). |
| Validación realizada | Revisión manual contra la guía del docente (PDF) y revisión del equipo. |
| Modificaciones | [Completar] |
| Responsable | Suyco Rivera Pedro Jesus |

### IA-02 — Modelo de datos

| Campo | Registro |
|---|---|
| Fecha | 2026-10-17 |
| Herramienta | Claude (Claude Code); se contrastó con una propuesta generada en otra sesión de Claude |
| Prompt | Diseñar el modelo de datos en Mermaid a partir de las historias de usuario; evaluar la propuesta alternativa e integrar sus mejoras. |
| Resultado generado | Diagrama entidad-relación, índices, restricciones, reglas de negocio y datos iniciales de transiciones. |
| ¿Se utilizó? | Parcialmente: se adoptaron `estado_transicion`, `secuencia_codigo`, `evidencia.usuario_id` y `reserva.fecha_resolucion`; se descartó el cierre automático; se eliminaron índices duplicados y se cambió la estrategia de bloqueo. |
| Validación realizada | Revisión técnica de cada propuesta (concurrencia, índices de InnoDB, soporte de `CHECK` en MySQL 9) y decisión del equipo. |
| Modificaciones | [Completar] |
| Responsable | Suyco Rivera Pedro Jesus |

### IA-03 — Documentación para la construcción

| Campo | Registro |
|---|---|
| Fecha | 2026-10-17 |
| Herramienta | Claude (Claude Code) |
| Prompt | Generar la documentación en Markdown para construir la aplicación: visión, requerimientos, historias por épica, arquitectura, modelo de datos, reglas de negocio, API, configuración y plan de construcción. |
| Resultado generado | Carpeta `docs/` con 16 archivos. |
| ¿Se utilizó? | [Completar tras la revisión del equipo] |
| Validación realizada | [Completar] |
| Modificaciones | [Completar] |
| Responsable | Suyco Rivera Pedro Jesus |

### IA-05 — Construcción del sprint S2

| Campo | Registro |
|---|---|
| Fecha | 2026-10-17 |
| Herramienta | Claude (Claude Code) |
| Prompt | Construir el registro y la consulta de solicitudes (HU-04, HU-05, HU-06) con evidencias, códigos únicos, fecha límite, historial y derivación automática de trámites, según supuestos confirmados por el equipo. |
| Resultado generado | Endpoints de solicitudes, evidencias y catálogos; validación de archivos por contenido; pantallas de nueva solicitud, listado y detalle; pruebas unitarias, de integración y de concurrencia. |
| ¿Se utilizó? | [Completar tras la revisión del equipo] |
| Validación realizada | 68 pruebas de backend y 28 de frontend; pruebas manuales con archivos reales (PDF válido, ejecutable renombrado), visibilidad por rol y recorrido completo en el navegador. Se detectó y corrigió un desfase de fracciones de segundo entre la respuesta y la base de datos. |
| Modificaciones | [Completar] |
| Responsable | Suyco Rivera Pedro Jesus |

### IA-04 — Construcción de los sprints S0 y S1

| Campo | Registro |
|---|---|
| Fecha | 2026-10-17 |
| Herramienta | Claude (Claude Code) |
| Prompt | Construir la base del proyecto (S0) y la autenticación (S1) según la documentación de `docs/`, con supuestos confirmados por el equipo antes de generar. |
| Resultado generado | Backend Spring Boot (seguridad JWT, migraciones Flyway, registro, login, creación de usuarios por el administrador) y frontend React + TypeScript (layout, rutas por rol, login y registro), con pruebas automatizadas. |
| ¿Se utilizó? | [Completar tras la revisión del equipo] |
| Validación realizada | 37 pruebas de backend y 18 de frontend; pruebas manuales de la API (casos válidos, errores 400/401/403/409/422, tokens alterados y falsificados) y del flujo completo en el navegador. |
| Modificaciones | [Completar] |
| Responsable | Suyco Rivera Pedro Jesus |

### IA-06 — Sprint S3: gestión y asignación de solicitudes (HU-07, HU-08, HU-09)

| Campo | Registro |
|---|---|
| Fecha | 2026-11-13 |
| Herramienta | T3 Code: Claude (Claude Code) como orquestador y revisor; subagente Gemini 4 Hydrogen (Antigravity) como implementador |
| Prompt | Implementar en `feature/s3-gestion-solicitudes` la bandeja paginada con filtros (`estadoId`, `categoriaId`, `prioridadId`, `vencidas`), las solicitudes asignadas al técnico, las transiciones permitidas por `estado_transicion` y rol (RN-05), la asignación de técnico (RN-07), el cambio de estado con historial y validación de propiedad/área (RN-06), el cambio de prioridad y la subida/descarga de evidencias; en el frontend, `BandejaPage`, `AsignadasPage` y los controles del detalle; con pruebas unitarias y MockMvc de 401/403 y transiciones válidas/inválidas. |
| Resultado generado | Entidad `EstadoTransicion`, `TransicionService`, eventos `SolicitudAsignadaEvent`/`EstadoCambiadoEvent`, nuevos endpoints en `SolicitudController` y `UsuarioController`, pantallas de bandeja y asignadas, detalle con transiciones, asignación, prioridad y evidencias, y pruebas de backend y frontend (PR #2). |
| ¿Se utilizó? | Sí, con correcciones. |
| Validación realizada | Suite completa de backend contra MySQL 9 en Docker (90 pruebas); lint, build y 32 pruebas de frontend. La revisión detectó que la prueba de solicitudes vencidas dependía de la zona horaria del servidor de base de datos (`NOW()` en UTC frente al `Clock` de la aplicación en America/Lima) y del caché de primer nivel de Hibernate tras un `UPDATE` por JDBC. |
| Modificaciones | La prueba `bandejaFiltersAndAreaRestrictionWork` ahora calcula la fecha límite con el `Clock` de la aplicación y limpia el `EntityManager` después del `UPDATE` (commit `6b2f056`). |
| Responsable | [Completar: Persona 1] |

### IA-07 — Fase III: endurecimiento de seguridad OWASP

| Campo | Registro |
|---|---|
| Fecha | 2026-11-13 |
| Herramienta | T3 Code: Claude (Claude Code) como orquestador y revisor; subagente Gemini 4 Hydrogen (Antigravity) como implementador |
| Prompt | Endurecer `SecurityConfig` en `feature/seguridad-owasp`: cabeceras `X-Content-Type-Options`, `X-Frame-Options`, `Referrer-Policy` y CSP básica compatible con Swagger; CORS leído de `CORS_ALLOWED_ORIGINS` y validado al arrancar; ningún stacktrace en `ProblemDetail`; límite de intentos fallidos en `/api/auth/login` con 429 y `Retry-After`; pruebas de cabeceras, CORS, 401/403, 500 genérico y limitador. |
| Resultado generado | Cabeceras y CSP, `CorsOriginsValidator` (rechaza `*` con credenciales y en `prod`), manejador genérico de 500, `server.error.include-*` en `never`, `LoginRateLimiter` en memoria por IP + correo con `Clock` inyectable y propiedades configurables, documentación en `docs/08-configuracion.md` y pruebas (PR #1). |
| ¿Se utilizó? | Sí. |
| Validación realizada | Revisión del diff (la IP se toma de `remoteAddr`, no de `X-Forwarded-For`, para que no se pueda falsificar); suite completa de backend contra MySQL 9 (87 pruebas) y suite combinada con S3 (109 pruebas), sin conflictos de merge. |
| Modificaciones | Ninguna. Limitación conocida: el limitador es por IP + correo, así que no frena un ataque que rote correos; es suficiente como protección básica. |
| Responsable | [Completar: Persona 1] |

### IA-08 — Sprint S6: comentarios y notificaciones (HU-10, HU-17, RN-16)

| Campo | Registro |
|---|---|
| Fecha | 2026-11-13 |
| Herramienta | T3 Code: Claude (Claude Code) como orquestador y revisor; subagente Gemini 4 Hydrogen (Antigravity) como implementador |
| Prompt | Implementar en `feature/comentarios-notificaciones`, sobre S3 y la rama de seguridad, los comentarios por solicitud con la misma visibilidad que el detalle, las notificaciones del usuario (listar, contar no leídas, marcar una o todas) y su creación con `@TransactionalEventListener(AFTER_COMMIT)` al asignar, cambiar estado o comentar, sin que un fallo de notificación afecte la operación principal; en el frontend, caja de comentarios, `NotificacionesPage` y campana con contador. |
| Resultado generado | Paquetes `comentario` y `notificacion`, `NotificacionListener` con `REQUIRES_NEW` y captura de errores, cálculo de destinatarios (nunca el autor), pantallas y campana con sondeo cada 30 s, y pruebas (PR #3). |
| ¿Se utilizó? | Sí. |
| Validación realizada | Suite completa de backend contra MySQL 9 (124 pruebas); lint, build y 36 pruebas de frontend. Se revisó el listener: las notificaciones solo se crean si la transacción confirma. |
| Modificaciones | Ninguna. Decisión: un usuario sin relación con la solicitud recibe 404 en comentarios (como en detalle, evidencias e historial) para no revelar que existe. |
| Responsable | [Completar: Persona 1] |
