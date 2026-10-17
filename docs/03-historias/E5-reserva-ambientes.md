# E5 — Reserva de ambientes

Permitir que cualquier usuario autenticado reserve el auditorio (único), un laboratorio o un salón, evitando cruces de horario.

Reglas relacionadas: RN-01, RN-04, RN-05, RN-13, RN-17 ([06-reglas-negocio.md](../06-reglas-negocio.md)).

---

## HU-13. Solicitar la reserva de un ambiente

**Como** usuario de la universidad (estudiante, docente o administrativo), **quiero** reservar el auditorio, un laboratorio o un salón en una fecha y horario **para** realizar una actividad académica.

| Escenario | Dado | Cuando | Entonces |
|---|---|---|---|
| 1. Reserva correcta | que el ambiente está libre en la fecha y horario elegidos | el usuario envía la reserva con todos los datos | el sistema la registra con un código único (ej. `RES-2026-0001`) en estado "Registrada" |
| 2. Cruce de horario | que el auditorio tiene una reserva aprobada de 10:00 a 12:00 | otro usuario solicita el auditorio de 11:00 a 13:00 el mismo día | el sistema responde 409 con "El ambiente ya está reservado en ese horario" y no registra la reserva |
| 3. Horario inválido | que el usuario está registrando una reserva | la hora de fin es anterior o igual a la de inicio, o la fecha ya pasó | el sistema muestra un mensaje de validación y no registra la reserva |

| ID | Capa | Tarea |
|---|---|---|
| T-36 | BD | Crear tablas `ambiente` y `reserva` |
| T-37 | Backend | Crear endpoint `POST /api/reservas` con validación de cruce (RN-04) |
| T-38 | Backend | Crear endpoint `GET /api/ambientes/{id}/disponibilidad?fecha=` |
| T-39 | Frontend | Crear formulario de reserva y vista de disponibilidad por horas |

**Notas de implementación**
- La validación de cruce bloquea la fila del ambiente (`SELECT ... FOR UPDATE`) antes de consultar, para que dos reservas simultáneas no pasen las dos (RN-04).
- Al registrar se compara solo contra reservas **aprobadas**. Por eso al aprobar se vuelve a validar (HU-14).

---

## HU-14. Aprobar o rechazar una reserva

**Como** supervisor, **quiero** aprobar o rechazar las reservas solicitadas **para** controlar el uso de los ambientes.

| Escenario | Dado | Cuando | Entonces |
|---|---|---|---|
| 1. Aprobación | que existe una reserva "Registrada" sin cruces | el supervisor la aprueba | el estado cambia a "Aprobada", se registra el aprobador y el horario queda ocupado |
| 2. Rechazo con motivo | que existe una reserva "Registrada" | el supervisor la rechaza indicando el motivo | el estado cambia a "Rechazada" y el solicitante ve el motivo |
| 3. Rechazo sin motivo | que el supervisor rechaza una reserva | deja el motivo vacío | el sistema exige el motivo y no cambia el estado |
| 4. Cruce al aprobar | que dos reservas pendientes piden el mismo horario y una ya fue aprobada | el supervisor intenta aprobar la otra | el sistema responde 409 y no la aprueba |

| ID | Capa | Tarea |
|---|---|---|
| T-40 | Backend | Crear endpoints `PUT /api/reservas/{id}/aprobar` y `PUT /api/reservas/{id}/rechazar` |
| T-41 | Frontend | Crear bandeja de reservas pendientes con acciones de aprobar y rechazar |

**Notas de implementación**
- Al aprobar o rechazar se completan `aprobador_id` y `fecha_resolucion`.
- El escenario 4 es nuevo respecto del informe parcial: sale de la revalidación del cruce al aprobar (RN-04).
