# 09 — Plan de construcción

Orden de implementación alineado con las fases de la guía del curso. Cada sprint termina con algo **funcionando de punta a punta** y con pruebas, no con capas sueltas.

## Estado

| Sprint | Estado | Fecha |
|---|---|---|
| S0 | Completado | 2026-10-17 |
| S1 | Completado (incluye `POST/GET /api/admin/usuarios`, adelantado de la HU-12) | 2026-10-17 |
| S2 | Completado (incluye la derivación automática de trámites, RN-09, adelantada del S4) | 2026-10-17 |

## Hitos de la guía

| Hito | Semana | Qué hay que demostrar |
|---|---|---|
| Fase I — Especificación y diseño | Antes de la semana 10 | Documento, historias, wireframes; primeras pantallas en React como evidencia de desarrollo |
| **Presentación parcial (MVP)** | **10** | Flujo end-to-end: login → registrar solicitud → guardar en MySQL → consultar → modificar estado |
| Fase III — APIs, nube, DevOps, seguridad | Después de la 10 | App desplegada, API documentada, CI/CD, seguridad, pruebas |
| **Sustentación final** | **16** | Demostración integral + informe final |

## Fase I — Especificación y base del proyecto

Decisión del equipo: **no se construye un prototipo aparte** en HTML/CSS/JS; se va directo a React + Spring Boot.

| Sprint | Entregable | Historias / tareas |
|---|---|---|
| S0 | Repositorio y estructura | Repositorio Git con `docs/`, `backend/`, `frontend/`; `.gitignore`; `.env.example`; ramas (`main`, `develop`, `feature/*`) |
| S0 | Esqueleto del backend | Proyecto Spring Boot con las dependencias de [04-arquitectura.md](04-arquitectura.md), manejo de errores con `ProblemDetail` y seguridad base |
| S0 | Esqueleto del frontend | Proyecto React + Vite con rutas, cliente `fetch` y layout base |

## Fase II — Construcción full stack (hasta la semana 10)

| Sprint | Objetivo | Historias | Incluye |
|---|---|---|---|
| S1 | Base de datos y autenticación | HU-01, HU-02, HU-03 | Migraciones V1 (esquema completo) y V2 (catálogos); `Usuario`, `Rol`; JWT; administrador inicial; manejador global de errores; login en React |
| S2 | Registrar y consultar solicitudes | HU-04, HU-05, HU-06 | Código único (RN-01), fecha límite (RN-02), historial (RN-14), evidencias (RN-12); pantallas de nueva solicitud, lista y detalle |
| S3 | Cambiar estado | HU-07, HU-08, HU-09 | `estado_transicion` (RN-05), autorización por propiedad (RN-06), asignación (RN-07); bandeja del supervisor |

**Al final de S3 está el MVP de la semana 10:** login → registrar solicitud → guardar en MySQL → consultar → modificar estado.

## Después de la semana 10

| Sprint | Objetivo | Historias | Incluye |
|---|---|---|---|
| S4 | Trámites académicos | HU-15, HU-16 | Derivación automática (RN-09), tomar trámite (RN-08), adjunto obligatorio |
| S5 | Reservas | HU-13, HU-14 | Cruce de horario con bloqueo (RN-04), aprobación con revalidación |
| S6 | Comentarios y notificaciones | HU-10, HU-17 | Eventos después del commit (RN-16), campana en React |
| S7 | Dashboard y administración | HU-11, HU-12 | Consultas agregadas, vencidas (RN-03), CRUD de catálogos |
| S8 | DevOps | — | Dockerfile, `docker-compose` (app + MySQL), GitHub Actions (build + pruebas), despliegue en la nube, ambientes dev/test/prod |
| S9 | Calidad y observabilidad | — | Pruebas de API, SAST/DAST, pruebas de carga, logs estructurados, métricas con Actuator, trazas (OpenTelemetry) |
| S10 | Entrega final | — | Manual de usuario, manual técnico, informe de pruebas, revisión del registro de IA, demostración |

## Definición de terminado (para cada historia)

- [ ] Los escenarios BDD de la historia pasan como pruebas automatizadas.
- [ ] Las reglas RN involucradas tienen pruebas unitarias del servicio.
- [ ] El endpoint aparece documentado en Swagger.
- [ ] No hay secretos ni valores fijos de configuración en el código.
- [ ] Los errores devuelven `ProblemDetail` con el código HTTP de [07-api.md](07-api.md).
- [ ] El cambio entró a `develop` mediante un pull request revisado por otro integrante.
- [ ] Si se usó IA, quedó registrada en [registro-ia.md](registro-ia.md).

## Estrategia de pruebas

| Nivel | Herramienta | Qué cubre |
|---|---|---|
| Unitarias | JUnit 5 + Mockito | Servicios y reglas RN |
| Integración | `@SpringBootTest` + MySQL de pruebas | Repositorios, migraciones Flyway, transacciones y bloqueos (RN-01, RN-04) |
| API | MockMvc o REST Assured | Endpoints, códigos HTTP, autorización por rol |
| Frontend | Vitest + Testing Library | Componentes y validaciones de formularios |
| Carga (Fase IV) | JMeter o k6 | RNF-03 (respuesta menor a 2 s) |
