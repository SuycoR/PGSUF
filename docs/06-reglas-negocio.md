# 06 — Reglas de negocio

Cada regla tiene un número fijo (RN-xx) para citarla en historias, código y pruebas. **Ninguna de estas reglas se puede garantizar solo con la base de datos**: todas se implementan en la capa de servicio y se cubren con pruebas.

| RN | Tema | Dónde se aplica |
|---|---|---|
| RN-01 | Código único | `SecuenciaCodigoService` |
| RN-02 | Fecha límite | `SolicitudService` |
| RN-03 | Solicitud vencida | `DashboardService` |
| RN-04 | Cruce de horario | `ReservaService` |
| RN-05 | Transiciones de estado | `TransicionService` |
| RN-06 | Autorización por propiedad | `SolicitudService` |
| RN-07 | Asignación de técnico | `SolicitudService` |
| RN-08 | Personal de área toma el trámite | `SolicitudService` |
| RN-09 | Derivación automática de trámites | `SolicitudService` |
| RN-10 | Área según rol | `UsuarioService` |
| RN-11 | Estados por ámbito | `TransicionService` |
| RN-12 | Archivos adjuntos | `EvidenciaService` |
| RN-13 | Motivo obligatorio | `TransicionService` |
| RN-14 | Historial de estados | `SolicitudService` |
| RN-15 | Fecha de cierre | `SolicitudService` |
| RN-16 | Notificaciones | `NotificacionListener` |
| RN-17 | Datos válidos de una reserva | `ReservaService` |
| RN-18 | Registro público | `AuthService` |
| RN-19 | Usuario inactivo | `AuthService` |
| RN-20 | Visibilidad | `SolicitudService`, `ReservaService` |

---

## RN-01 — Código único

- Formato: `SOL-AAAA-NNNN` para solicitudes y `RES-AAAA-NNNN` para reservas (ej. `SOL-2026-0001`). La numeración se reinicia cada año.
- Se genera **en la misma transacción** que inserta la solicitud o la reserva:
  1. `SELECT ultimo_valor FROM secuencia_codigo WHERE prefijo = ? AND anio = ? FOR UPDATE`
  2. Si no existe la fila, se inserta con `ultimo_valor = 0`.
  3. Se incrementa `ultimo_valor` y se arma el código.
- Si la transacción falla, el número no se consume (el bloqueo se libera con el rollback).

## RN-02 — Fecha límite

`fecha_limite = fecha_registro + tipo_solicitud.sla_horas`. Se calcula al registrar y **no se recalcula** si después el administrador cambia el SLA del tipo.

- Las horas son **corridas**: no se descuentan fines de semana ni feriados (calcular días hábiles requeriría un calendario de feriados).
- Las fechas se guardan en segundos enteros (la columna es `DATETIME`), así que se truncan antes de guardar y de responder.

## RN-03 — Solicitud vencida

Una solicitud está vencida si `fecha_limite < ahora` **y** su estado no es final (`estado.es_final = false`). No se guarda como estado: se calcula en las consultas.

## RN-04 — Cruce de horario

Dos reservas se cruzan si son del **mismo ambiente**, la **misma fecha** y `nueva.hora_inicio < existente.hora_fin AND nueva.hora_fin > existente.hora_inicio`.

- **Al registrar:** se compara contra las reservas **aprobadas**. Si hay cruce → 409.
- **Al aprobar:** se vuelve a validar contra las aprobadas, porque pudo aprobarse otra reserva pendiente para el mismo horario. Si hay cruce → 409.
- **Concurrencia:** antes de consultar, se bloquea la fila del ambiente dentro de la transacción (`SELECT ... FROM ambiente WHERE id = ? FOR UPDATE`). Así, las operaciones sobre un mismo ambiente se procesan de a una.
- Que una reserva termine a las 12:00 y otra empiece a las 12:00 **no** es cruce.

## RN-05 — Transiciones de estado

El servicio no usa `if` fijos: valida contra `estado_transicion` buscando (estado actual, estado destino, rol del usuario, ámbito). Si no existe la fila → 422 "Transición de estado no permitida".

Datos iniciales (`rol = —` significa que la ejecuta el sistema):

| Ámbito | Origen | Destino | Rol | Requiere motivo | Uso |
|---|---|---|---|---|---|
| SOLICITUD | Registrada | En evaluación | SUPERVISOR | No | Técnicas: el supervisor la revisa |
| SOLICITUD | En evaluación | Asignada | SUPERVISOR | No | Técnicas: asignación (RN-07) |
| SOLICITUD | En evaluación | Rechazada | SUPERVISOR | Sí | Técnicas: no procede |
| SOLICITUD | Asignada | En atención | TECNICO | No | Técnicas |
| SOLICITUD | En atención | Resuelta | TECNICO | No | Técnicas |
| SOLICITUD | Registrada | En evaluación | — | No | Trámites: derivación automática (RN-09) |
| SOLICITUD | En evaluación | En atención | PERSONAL_AREA | No | Trámites: lo toma (RN-08) |
| SOLICITUD | En atención | Resuelta | PERSONAL_AREA | No | Trámites |
| SOLICITUD | En atención | Rechazada | PERSONAL_AREA | Sí | Trámites |
| SOLICITUD | Resuelta | Cerrada | SOLICITANTE | No | Ambos: el solicitante confirma |
| RESERVA | Registrada | Aprobada | SUPERVISOR | No | |
| RESERVA | Registrada | Rechazada | SUPERVISOR | Sí | |

**No hay cierre automático:** una solicitud "Resuelta" pasa a "Cerrada" solo cuando la cierra el solicitante.

## RN-06 — Autorización por propiedad

El rol solo dice **qué tipo** de acción se puede hacer; además, el usuario debe estar relacionado con la solicitud:

| Rol | Puede actuar sobre la solicitud si… |
|---|---|
| TECNICO | es el `responsable_id` |
| PERSONAL_AREA | `usuario.area_id = tipo_solicitud.area_id` (y para resolver o rechazar, además es el `responsable_id`) |
| SOLICITANTE | es el `usuario_id` (solicitante) |
| SUPERVISOR | la solicitud es de la categoría Soporte técnico e infraestructura |

Si no se cumple → 403.

## RN-07 — Asignación de técnico

- El usuario asignado debe tener rol `TECNICO`, estar activo y cumplir `usuario.area_id = tipo_solicitud.area_id`. Si no → 422.
- Asignar = fijar `responsable_id` + transición "En evaluación → Asignada", en una sola transacción.

## RN-08 — Personal de área toma el trámite

Cuando el personal de área ejecuta "En evaluación → En atención", el sistema fija `responsable_id` con ese usuario. Desde ese momento solo él puede resolverlo o rechazarlo.

## RN-09 — Derivación automática de trámites

Al registrar una solicitud de categoría Rectificaciones o Matrícula, en la misma transacción el sistema la pasa de "Registrada" a "En evaluación" (transición con `rol = null`). El historial guarda los dos pasos; el segundo con `usuario_id = null`.

## RN-10 — Área según rol

- `TECNICO` y `PERSONAL_AREA`: `area_id` obligatorio.
- `SOLICITANTE`, `SUPERVISOR` y `ADMIN`: `area_id` nulo.

Si no se cumple → 422.

## RN-11 — Estados por ámbito

Una solicitud solo puede tener estados de ámbito `SOLICITUD` o `AMBOS`; una reserva, solo `RESERVA` o `AMBOS`. Por ejemplo, una solicitud nunca queda "Aprobada" ni una reserva "En atención".

## RN-12 — Archivos adjuntos

- Formatos permitidos: PDF, JPG, PNG. Tamaño máximo: 5 MB por archivo. Hasta 5 archivos por envío.
- Todos los archivos se validan **antes** de escribir nada; si la transacción falla después de escribirlos, se borran del disco.
- El tipo se valida **por el contenido del archivo** (sus primeros bytes), no solo por la extensión o el `Content-Type` que envía el navegador (OWASP: subida de archivos).
- En disco se guarda con un nombre generado (UUID), nunca con el nombre original.
- Si `tipo_solicitud.requiere_adjunto = true`, la solicitud no se registra sin al menos un archivo válido.

## RN-13 — Motivo obligatorio

Si la transición tiene `requiere_motivo = true`, el motivo es obligatorio (no vacío). Se guarda en `solicitud.respuesta` o en `reserva.motivo_rechazo`, y también en `historial_estado.observacion`.

## RN-14 — Historial de estados

Todo cambio de estado de una solicitud inserta una fila en `historial_estado`, **incluido el registro inicial** (`estado_anterior_id = null`). Va en la misma transacción que el cambio.

## RN-15 — Fecha de cierre

Cuando una solicitud llega a un estado final (`es_final = true`), se fija `fecha_cierre`.

## RN-16 — Notificaciones

Solo dentro del sistema. Se crean **después del commit** (`@TransactionalEventListener(phase = AFTER_COMMIT)`): si la operación falla, no se notifica; si la notificación falla, la operación no se deshace.

| Tipo (`notificacion.tipo`) | Evento | Destinatario |
|---|---|---|
| `SOLICITUD_REGISTRADA` | Se registra una solicitud técnica | Todos los supervisores activos |
| `SOLICITUD_DERIVADA` | Se deriva un trámite (RN-09) | Todo el personal activo del área |
| `SOLICITUD_ASIGNADA` | Se asigna una solicitud | El técnico asignado |
| `ESTADO_CAMBIADO` | Cambia el estado de una solicitud | El solicitante (si no fue él quien la cambió) |
| `COMENTARIO_NUEVO` | Se agrega un comentario | El solicitante y el responsable, excepto el autor |
| `RESERVA_REGISTRADA` | Se registra una reserva | Todos los supervisores activos |
| `RESERVA_RESUELTA` | Se aprueba o rechaza una reserva | El solicitante de la reserva |

## RN-17 — Datos válidos de una reserva

- `hora_fin > hora_inicio`.
- La fecha y hora de inicio no pueden estar en el pasado.
- El ambiente debe estar activo.

## RN-18 — Registro público

El registro público (`/api/auth/registro`) siempre crea usuarios con rol `SOLICITANTE`. Los demás roles solo los asigna el administrador.

## RN-19 — Usuario inactivo

Un usuario con `activo = false` no puede iniciar sesión. Se responde el mismo error que con credenciales incorrectas.

## RN-20 — Visibilidad

| Rol | Ve |
|---|---|
| SOLICITANTE | Sus solicitudes y sus reservas |
| TECNICO | Las solicitudes donde es responsable |
| PERSONAL_AREA | Las solicitudes cuyo tipo pertenece a su área |
| SUPERVISOR | Todas las solicitudes técnicas y todas las reservas |
| ADMIN | Todo |

Si un usuario pide una solicitud o reserva que no puede ver → 404 (no se revela que existe).
