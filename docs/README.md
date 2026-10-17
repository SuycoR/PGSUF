# Sistema Web de Gestión de Servicios Universitarios — Documentación

Documentación técnica para construir la aplicación del proyecto del curso **Taller de Construcción de Software Web** (UNMSM, 2026-II), grupo G2.

> Esta carpeta es la **fuente de verdad para construir**. El informe en Word es el entregable académico; si algo difiere, manda lo que dice aquí y se actualiza el informe.

## Stack

| Capa | Tecnología |
|---|---|
| Frontend | React 19 + TypeScript + Vite, React Router 7, Vitest |
| Backend | Java 21 + Spring Boot 3.5.16 (Maven Wrapper, Maven 3.9.16) |
| Base de datos | MySQL 9 + Flyway |
| Seguridad | Spring Security + JWT + BCrypt |
| Documentación API | springdoc-openapi (Swagger UI) |
| DevOps (Fase III) | Git, Docker, GitHub Actions, despliegue en la nube |

## Índice

| Documento | Contenido | Cuándo leerlo |
|---|---|---|
| [01-vision.md](01-vision.md) | Problema, objetivos, alcance, actores | Para entender qué se construye |
| [02-requerimientos.md](02-requerimientos.md) | Clasificación de solicitudes, RF, RNF | Antes de diseñar una funcionalidad |
| [03-historias/](03-historias/) | Épicas E1–E6: historias, escenarios BDD, tareas | Al implementar una historia |
| [04-arquitectura.md](04-arquitectura.md) | Capas, paquetes, seguridad, decisiones | Antes de escribir código |
| [05-modelo-datos.md](05-modelo-datos.md) | Diagrama ER, tablas, índices, datos iniciales | Al crear entidades y migraciones |
| [06-reglas-negocio.md](06-reglas-negocio.md) | Reglas RN numeradas | Al implementar servicios y pruebas |
| [07-api.md](07-api.md) | Endpoints, roles, códigos HTTP, errores | Al implementar controladores o el frontend |
| [08-configuracion.md](08-configuracion.md) | Variables de entorno, setup local | Al levantar el proyecto |
| [09-plan-construccion.md](09-plan-construccion.md) | Orden de implementación por sprints | Para planificar el trabajo |
| [registro-ia.md](registro-ia.md) | Registro de uso de IA (requisito del curso) | Cada vez que se use IA |

## Historias de usuario

| Épica | Archivo |
|---|---|
| E1. Autenticación y acceso | [03-historias/E1-autenticacion.md](03-historias/E1-autenticacion.md) |
| E2. Registro de solicitudes | [03-historias/E2-registro-solicitudes.md](03-historias/E2-registro-solicitudes.md) |
| E3. Gestión de solicitudes | [03-historias/E3-gestion-solicitudes.md](03-historias/E3-gestion-solicitudes.md) |
| E4. Dashboard, administración y notificaciones | [03-historias/E4-dashboard-administracion.md](03-historias/E4-dashboard-administracion.md) |
| E5. Reserva de ambientes | [03-historias/E5-reserva-ambientes.md](03-historias/E5-reserva-ambientes.md) |
| E6. Trámites académicos | [03-historias/E6-tramites-academicos.md](03-historias/E6-tramites-academicos.md) |

## Reglas para el equipo

- **Ningún secreto en el repositorio.** Contraseñas, `JWT_SECRET` y credenciales van en variables de entorno (ver [08-configuracion.md](08-configuracion.md)).
- **Cada cambio de modelo es una migración Flyway nueva.** Nunca se edita una migración ya aplicada.
- **Cada uso significativo de IA se registra** en [registro-ia.md](registro-ia.md).
- **Una regla de negocio nueva se agrega a [06-reglas-negocio.md](06-reglas-negocio.md)** con su número RN antes de implementarla.
