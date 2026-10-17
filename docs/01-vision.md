# 01 — Visión del proyecto

## Problema

Las solicitudes de servicios dentro de la universidad se gestionan por canales dispersos: correos, formularios aislados, llamadas telefónicas o sistemas independientes. Esto provoca:

- Pérdida o duplicación de solicitudes.
- Falta de seguimiento y trazabilidad.
- Dificultad para saber quién es el responsable de atender cada caso.
- Ausencia de datos sobre tiempos de atención.
- Imposibilidad de medir la calidad del servicio.
- Falta de indicadores para la toma de decisiones.

## Objetivo general

Desarrollar una aplicación web full-stack que permita registrar, gestionar, priorizar, atender y monitorear solicitudes de servicios universitarios, garantizando usabilidad, seguridad, calidad, rendimiento y disponibilidad.

## Objetivos específicos

1. Especificar los requerimientos mediante épicas, historias de usuario, criterios de aceptación y escenarios BDD.
2. Diseñar la interfaz y la arquitectura mediante wireframes, mockups y una arquitectura por capas.
3. Construir la aplicación con frontend en React, backend en Spring Boot y base de datos MySQL, integrados mediante una API REST.
4. Implementar autenticación y control de acceso por roles.
5. Permitir el seguimiento completo de cada solicitud mediante un flujo de estados con trazabilidad y responsable asignado.
6. Aplicar buenas prácticas de ingeniería: Git, pruebas, CI/CD, seguridad OWASP y observabilidad.
7. Registrar el uso responsable de herramientas de IA.

## Alcance

El sistema gestiona **cuatro categorías** de solicitudes, realizadas por estudiantes, docentes y personal administrativo:

| # | Categoría | Ejemplos |
|---|---|---|
| 1 | Soporte técnico e infraestructura | Proyector malogrado, PC sin red, falla de luz |
| 2 | Reserva de ambientes | Auditorio (único), laboratorios, salones |
| 3 | Rectificaciones | Rectificación de nota, de datos personales, de nombre |
| 4 | Matrícula | Matrícula extemporánea, retiro de curso, cambio de sección |

**Incluye:** autenticación, registro y gestión de solicitudes, reservas con control de cruces, trámites con documentos de sustento, notificaciones dentro del sistema, dashboard de indicadores, administración de catálogos, despliegue en la nube con CI/CD, pruebas, seguridad y observabilidad.

**No incluye (decisiones del equipo):**

- Notificaciones por correo electrónico (solo dentro del sistema).
- Cierre automático de solicitudes resueltas (las cierra el solicitante).
- Avisos automáticos de solicitudes por vencer.
- Integración con sistemas académicos existentes de la universidad.

## Actores

Docentes y personal administrativo usan el mismo rol de solicitante que los estudiantes.

| Rol | Código | Quiénes | Funciones principales |
|---|---|---|---|
| Solicitante | `SOLICITANTE` | Estudiantes, docentes, personal administrativo | Registrar solicitudes, reservas y trámites; consultar su estado; comentar; cerrar solicitudes resueltas |
| Técnico | `TECNICO` | Personal de soporte técnico e infraestructura | Atender solicitudes técnicas asignadas y actualizar su estado |
| Personal de área | `PERSONAL_AREA` | Secretaría académica, Oficina de matrícula | Tomar, resolver o rechazar los trámites de su área |
| Supervisor | `SUPERVISOR` | Jefatura o coordinación de servicios | Evaluar, asignar y priorizar solicitudes técnicas; aprobar o rechazar reservas; revisar el dashboard |
| Administrador | `ADMIN` | Administrador del sistema | Gestionar usuarios, roles, catálogos, ambientes y transiciones |
