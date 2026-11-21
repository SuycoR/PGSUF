# Entregable 2 — División del Informe (Hasta Fase III: Release Candidate)

Este documento organiza la continuación del **Informe Parcial verdadero** ([Informa parcial G2 Web .docx.pdf](../../Informa%20parcial%20G2%20Web%20.docx.pdf)) para el **Entregable 2**, que abarca **Fase II (Construcción Full Stack / MVP)** y **Fase III (APIs, Nube, DevOps, Seguridad y Testing)** según la guía del curso ([Proyecto - Taller de construccion de software web.pdf](../../Proyecto%20-%20Taller%20de%20construccion%20de%20software%20web.pdf)).

---

## Base heredada del Informe Parcial (Secciones 1 a 8)

El Informe Parcial ya deja cerrados los siguientes elementos, que se conservan como base del Entregable 2:
- **Carátula:** 6 autores (Villavicencio Davila Giancarlo, Suyco Rivera Pedro Jesus, Sanchez Goteo Edu Joseph, Fernandez Cavero Juan Brandon, Padilla Arellano Alejandro Manuel, Tataje Rodriguez Anderson) — Profesor David Candela Arce.
- **Secciones 1–5:** Problema, Justificación, Objetivos, Usuarios y actores (5 roles), Alcance del proyecto completo. *(Solo se actualiza `5.b` para reflejar el alcance del Entregable 2 hasta Fase III).*
- **Sección 6:** Clasificación de solicitudes, `RF-01` a `RF-14`, `RNF-01` a `RNF-07`, **Reglas de negocio `RN-01` a `RN-20`**, **Flujo de estados y transiciones**, Tabla resumen y Épicas **E1–E6** (`HU-01` a `HU-17`, incluyendo notificaciones).
- **Sección 7:** Arquitectura preliminar, **Modelo de datos de 16 tablas** (Figuras 2, 3 y 4) y **Catálogo de 121 ambientes** (`7.c`).
- **Sección 8:** Wireframes y mockups (Figuras 5 a 10).

> **Nota:** Las secciones `9. Prototipo` y `10. Evidencia del primer desarrollo` del Informe Parcial evolucionan en el Entregable 2 hacia las secciones de **Construcción Full Stack (Fase II)** y **APIs, Seguridad, Testing y DevOps (Fase III)**.

---

## División del Informe — Entregable 2 (3 Personas)

| Sección del Informe (Entregable 2) | Responsable | Qué redacta y evidencia |
|---|---|---|
| **Carátula, Índice y `5.b` Alcance del Entregable 2** | **Persona 1** (consolida) | Actualización de `5.b` (Fase II completa + Fase III Release Candidate) y actualización de la tabla de estado de sprints. |
| **Sección 9. Construcción Full Stack — Fase II** | **Dividida (P1, P2, P3)** | |
| `9.1` Arquitectura implementada (Frontend React + Backend Spring Boot + MySQL/Flyway) | **Persona 1** | Base S0–S2 ya construida (Auth JWT, catálogos, registro y consulta de solicitudes, `ProblemDetail`) y cómo se integran los módulos. |
| `9.2` Módulo de Gestión de Solicitudes, Comentarios y Notificaciones (`S3 + S6`) | **Persona 1** | Bandeja `/bandeja` (`HU-07`), asignación (`HU-08`, RN-07), transiciones de estado (`HU-09`, RN-05/06/14/15), evidencias (`RN-12`), comentarios (`HU-10`) y notificaciones post-commit (`HU-17`, RN-16) + campana en React. |
| `9.3` Módulo de Reservas de Ambientes y Dashboard de Indicadores (`S5 + S7`) | **Persona 2** | Reservas con control de concurrencia (`HU-13`, RN-04/17), aprobación/rechazo con motivo (`HU-14`, RN-13), pantallas `/reservas` y `/reservas/aprobacion`, y Dashboard de KPIs (`HU-11`, RN-03) en `/dashboard`. |
| `9.4` Módulo de Administración de Catálogos y Usuarios (`S7`) | **Persona 3** | CRUDs `/api/admin/*` (`HU-12`: usuarios, áreas, categorías, tipos, prioridades, estados, transiciones, ambientes) con baja lógica (`activo`) y pantalla `/admin` con pestañas. |
| `9.5` Evidencia del flujo End-to-End (MVP Semana 10) | **Persona 1** (con capturas de P2 y P3) | Flujo completo: Login → registrar solicitud → guardar en MySQL → consultar → asignar/cambiar estado → comentar/notificar. |
| **Sección 10. API REST y Documentación — Fase III** | **Dividida (P1 + P2 + P3)** | |
| `10.1` Contratos REST, parámetros, códigos HTTP y errores `ProblemDetail` | **Cada uno sus endpoints** (P1 consolida tabla) | P1: Auth, Solicitudes, Evidencias, Comentarios, Notificaciones. P2: Ambientes, Reservas, Dashboard. P3: Admin. |
| `10.2` Documentación interactiva OpenAPI / Swagger (`/swagger-ui.html`) | **Persona 2** | Auditoría de anotaciones `@Operation` / `@ApiResponse` en todos los controladores y capturas de Swagger UI. |
| `10.3` Panorama de integraciones (Webhooks/Eventos, GraphQL/gRPC, API Gateway) | **Persona 2** | Breve análisis comparativo y justificación de REST + eventos internos `@TransactionalEventListener` en el proyecto. |
| **Sección 11. Seguridad y Principios OWASP — Fase III** | **Persona 1** | JWT stateless, BCrypt, RBAC por rol, validación de propiedad/área (prevención de BOLA/IDOR, RN-06/RN-20), cabeceras HTTP OWASP, CORS por env, secretos sin hardcode, `ProblemDetail` sin stacktrace, rate-limit en `/api/auth/login`. |
| **Sección 12. Testing y Automatización — Fase III** | **Dividida (P1 + P2)** | |
| `12.1` Pruebas unitarias y de API (`MockMvc`) de Solicitudes y Seguridad | **Persona 1** | Tests de permisos por rol (`401`/`403`), transiciones válidas/inválidas (RN-05), propiedad (RN-06), asignación (RN-07), más generación y revisión de tests asistida por IA. |
| `12.2` Pruebas de concurrencia (Reservas) e indicadores (Dashboard) | **Persona 2** | Tests de integración de cruce de horarios simultáneos (RN-04) y cálculo de indicadores del Dashboard. |
| `12.3` Pruebas automatizadas en Frontend (Vitest + Testing Library) | **Persona 1** (base + solicitudes) + **P2/P3** | Resumen de suites Vitest y capturas de ejecución automática. |
| **Sección 13. DevOps, Docker, CI/CD y Despliegue Cloud — Fase III** | **Persona 3** | |
| `13.1` Contenerización (`Dockerfile` backend/frontend y `docker-compose.yml`) | **Persona 3** | Multi-stage builds, Nginx proxy `/api`, red interna, volúmenes MySQL/`uploads` y `.env.example`. |
| `13.2` Estrategia Git (`main`, `develop`, `feature/*`) y Pipeline CI/CD | **Persona 3** | Flujo de `.github/workflows/ci.yml` (jobs Backend con MySQL service, Frontend lint/test/build y Docker build). |
| `13.3` Ambientes (dev / test / prod) y Despliegue Cloud | **Persona 3** | Plataforma cloud elegida, HTTPS, variables de entorno seguras y verificación de `/actuator/health` y `/swagger-ui.html`. |
| **Sección 14. Registro de uso de IA (Actualizado)** | **Todos (cada uno sus filas)** | Consolidado de prompts, validación, modificaciones y responsables en [registro-ia.md](../registro-ia.md). |

---

## Checklist rápido de entrega por persona

- **Persona 1:** Redacta `5.b`, `9.1`, `9.2`, `9.5`, `10.1` (su parte), `11`, `12.1`, `12.3` (su parte) y sus filas de `14`. *(Ya redactado en [persona-1-informe.md](persona-1-informe.md)).*
- **Persona 2:** Redacta `9.3`, `10.1` (reservas/dashboard), `10.2`, `10.3`, `12.2` y sus filas de `14`, adjuntando capturas de `/reservas`, `/reservas/aprobacion`, `/dashboard`, `/swagger-ui.html` y tests de concurrencia.
- **Persona 3:** Redacta `9.4`, `10.1` (admin), `13.1`, `13.2`, `13.3` y sus filas de `14`, adjuntando capturas de `/admin`, GitHub Actions en verde, `docker compose ps` y URL cloud con `/actuator/health`.
