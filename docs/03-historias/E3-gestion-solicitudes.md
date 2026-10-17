# E3 — Gestión de solicitudes

Permitir supervisar, asignar y atender las solicitudes siguiendo un flujo de estados.

Reglas relacionadas: RN-05, RN-06, RN-07, RN-13, RN-14, RN-15 ([06-reglas-negocio.md](../06-reglas-negocio.md)).

---

## HU-07. Ver todas las solicitudes y filtrarlas

**Como** supervisor, **quiero** ver todas las solicitudes técnicas y filtrarlas por estado, categoría o prioridad **para** organizar la atención.

| Escenario | Dado | Cuando | Entonces |
|---|---|---|---|
| 1. Listado general | que el supervisor está autenticado | entra a la bandeja de solicitudes | el sistema muestra las solicitudes, paginadas y ordenadas por fecha |
| 2. Filtrado | que existen solicitudes en distintos estados | aplica un filtro por estado | el sistema muestra solo las que coinciden |

| ID | Capa | Tarea |
|---|---|---|
| T-20 | Backend | Crear endpoint `GET /api/solicitudes` con filtros y paginación |
| T-21 | Frontend | Crear bandeja con filtros y tabla de solicitudes |

**Notas de implementación**
- El personal de área usa el mismo endpoint, pero el servicio filtra automáticamente por su área (RN-20).
- El supervisor puede cambiar la prioridad desde la bandeja (`PUT /api/solicitudes/{id}/prioridad`).

---

## HU-08. Asignar una solicitud a un técnico

**Como** supervisor, **quiero** asignar una solicitud a un técnico **para** que tenga un responsable claro.

| Escenario | Dado | Cuando | Entonces |
|---|---|---|---|
| 1. Asignación correcta | que existe una solicitud en estado "En evaluación" | el supervisor selecciona un técnico del área y confirma | el sistema asigna el responsable y cambia el estado a "Asignada" |
| 2. Rol no autorizado | que el usuario no es supervisor | intenta asignar | el sistema responde 403 y no realiza la asignación |
| 3. Técnico de otra área | que el técnico elegido no pertenece al área del tipo de solicitud | el supervisor confirma | el sistema responde 422 y no realiza la asignación |

| ID | Capa | Tarea |
|---|---|---|
| T-22 | Backend | Crear endpoint `PUT /api/solicitudes/{id}/asignar` |
| T-23 | Backend | Crear endpoint `GET /api/usuarios/tecnicos?areaId=` |
| T-24 | Frontend | Crear selector de técnico y botón "Asignar" en el detalle |

**Notas de implementación**
- Asignar = fijar `responsable_id` + transición "En evaluación → Asignada" en una sola transacción (RN-07).

---

## HU-09. Cambiar el estado de una solicitud

**Como** técnico, **quiero** actualizar el estado de las solicitudes que me asignaron **para** informar el avance de la atención.

| Escenario | Dado | Cuando | Entonces |
|---|---|---|---|
| 1. Transición válida | que la solicitud está "Asignada" al técnico | la cambia a "En atención" | el sistema actualiza el estado y registra el cambio en el historial |
| 2. Transición inválida | que la solicitud está "Registrada" | el técnico intenta pasarla a "Resuelta" | el sistema responde 422 con "Transición de estado no permitida" |
| 3. Solicitud ajena | que la solicitud está asignada a otro técnico | intenta cambiar su estado | el sistema responde 403 y no la modifica |

| ID | Capa | Tarea |
|---|---|---|
| T-25 | Backend | Crear endpoint `PUT /api/solicitudes/{id}/estado` |
| T-26 | Backend | Implementar la validación de transiciones consultando `estado_transicion` |
| T-27 | BD | Crear tablas `estado_transicion` e `historial_estado` |
| T-28 | Frontend | Agregar control de cambio de estado en el detalle (solo muestra los destinos permitidos) |

**Notas de implementación**
- El servicio no usa `if` fijos: consulta `estado_transicion` por (estado actual, destino, rol, ámbito) (RN-05).
- Además del rol, valida que el técnico sea el `responsable_id` (RN-06).
- El solicitante también usa este endpoint para cerrar una solicitud "Resuelta".

---

## HU-10. Agregar comentarios a una solicitud

**Como** usuario involucrado, **quiero** comentar una solicitud **para** dar o recibir información adicional.

| Escenario | Dado | Cuando | Entonces |
|---|---|---|---|
| 1. Comentario correcto | que el usuario tiene acceso a la solicitud | escribe y envía un comentario | el sistema lo guarda con autor y fecha y lo muestra en el detalle |
| 2. Comentario vacío | que el campo está vacío | intenta enviarlo | el sistema muestra un mensaje de validación y no lo guarda |

| ID | Capa | Tarea |
|---|---|---|
| T-29 | BD | Crear tabla `comentario` |
| T-30 | Backend | Crear endpoints `POST` y `GET /api/solicitudes/{id}/comentarios` |
| T-31 | Frontend | Agregar sección de comentarios en el detalle |
