# 07 — API REST

Base: `/api`. Formato: JSON (salvo la subida de archivos, que es `multipart/form-data`). Autenticación: cabecera `Authorization: Bearer <token>` en todos los endpoints excepto los de `/api/auth`.

La documentación interactiva se genera con springdoc-openapi en `/swagger-ui.html` (solo en el perfil `dev`).

## Convenciones

| Tema | Convención |
|---|---|
| Nombres | Recursos en plural y en español: `/solicitudes`, `/reservas` |
| Paginación | `?page=0&size=20&sort=fechaRegistro,desc`; la respuesta trae `content`, `totalElements`, `totalPages` |
| Fechas | ISO 8601: `2026-10-24`, `10:00`, `2026-10-17T09:30:00` |
| JSON | Propiedades en camelCase: `fechaRegistro`, `tipoSolicitudId` |
| Errores | `ProblemDetail` (RFC 7807) |

## Códigos HTTP

| Código | Cuándo |
|---|---|
| 200 OK | Consulta o actualización correcta |
| 201 Created | Recurso creado (con cabecera `Location`) |
| 204 No Content | Acción sin cuerpo de respuesta (ej. marcar notificaciones) |
| 400 Bad Request | Datos inválidos (Bean Validation) o JSON mal formado |
| 401 Unauthorized | Sin token, token inválido o vencido; credenciales incorrectas |
| 403 Forbidden | Rol sin permiso o acción sobre algo que no le corresponde (RN-06) |
| 404 Not Found | No existe o el usuario no puede verlo (RN-20) |
| 409 Conflict | Cruce de horario (RN-04); correo ya registrado |
| 422 Unprocessable Entity | Regla de negocio incumplida: transición no permitida, técnico de otra área, falta adjunto obligatorio |
| 500 Internal Server Error | Error inesperado (se registra en el log; al cliente no se le muestra el detalle) |

## Formato de error

```json
{
  "type": "about:blank",
  "title": "Transición de estado no permitida",
  "status": 422,
  "detail": "No se puede pasar de 'Registrada' a 'Resuelta'.",
  "instance": "/api/solicitudes/15/estado",
  "codigo": "TRANSICION_NO_PERMITIDA"
}
```

En errores de validación se agrega `errores`: una lista de `{ "campo": "descripcion", "mensaje": "es obligatorio" }`.

## Endpoints

Roles: `SOL` Solicitante · `TEC` Técnico · `PA` Personal de área · `SUP` Supervisor · `ADM` Administrador · `*` cualquier usuario autenticado.

### Autenticación

| Método | Ruta | Rol | Descripción | HU |
|---|---|---|---|---|
| POST | `/api/auth/login` | Público | Devuelve `{ token, usuario }` | HU-01 |
| POST | `/api/auth/registro` | Público | Crea un usuario `SOLICITANTE` | HU-02 |
| GET | `/api/auth/me` | * | Datos del usuario autenticado | HU-03 |

### Solicitudes

| Método | Ruta | Rol | Descripción | HU |
|---|---|---|---|---|
| POST | `/api/solicitudes` | * | Registra una solicitud (multipart: parte `datos` en JSON + parte `archivos`, 0..n) | HU-04, HU-15 |
| GET | `/api/solicitudes/mis-solicitudes` | * | Solicitudes propias, paginadas | HU-05 |
| GET | `/api/solicitudes` | SUP, PA, ADM | Bandeja con filtros `estadoId`, `categoriaId`, `prioridadId`, `vencidas`; PA ve solo su área | HU-07 |
| GET | `/api/solicitudes/asignadas` | TEC | Solicitudes donde es responsable | HU-09 |
| GET | `/api/solicitudes/{id}` | * | Detalle (según RN-20) | HU-06 |
| GET | `/api/solicitudes/{id}/historial` | * | Historial de estados | HU-06 |
| GET | `/api/solicitudes/{id}/transiciones` | * | Estados destino permitidos para el usuario actual | HU-09 |
| PUT | `/api/solicitudes/{id}/asignar` | SUP | `{ responsableId }` (RN-07) | HU-08 |
| PUT | `/api/solicitudes/{id}/estado` | TEC, PA, SUP, SOL | `{ estadoDestinoId, motivo?, respuesta? }` (RN-05) | HU-09, HU-16 |
| PUT | `/api/solicitudes/{id}/prioridad` | SUP | `{ prioridadId }` | HU-07 |
| POST | `/api/solicitudes/{id}/evidencias` | * (involucrados) | Agrega archivos (multipart) | HU-15 |
| GET | `/api/solicitudes/{id}/evidencias` | * (involucrados) | Lista de evidencias | HU-06 |
| GET | `/api/evidencias/{id}/descarga` | * (involucrados) | Descarga el archivo | HU-06 |
| GET | `/api/solicitudes/{id}/comentarios` | * (involucrados) | Lista de comentarios | HU-10 |
| POST | `/api/solicitudes/{id}/comentarios` | * (involucrados) | `{ texto }` | HU-10 |

### Reservas y ambientes

| Método | Ruta | Rol | Descripción | HU |
|---|---|---|---|---|
| GET | `/api/ambientes` | * | Lista de ambientes activos; filtros `tipo`, `pabellon`, `piso` | HU-13 |
| GET | `/api/ambientes/{id}/disponibilidad?fecha=` | * | Reservas aprobadas del ambiente en esa fecha | HU-13 |
| POST | `/api/reservas` | * | `{ ambienteId, fecha, horaInicio, horaFin, motivo }` (RN-04, RN-17) | HU-13 |
| GET | `/api/reservas/mis-reservas` | * | Reservas propias | HU-13 |
| GET | `/api/reservas` | SUP, ADM | Bandeja con filtros `estadoId`, `ambienteId`, `fecha` | HU-14 |
| PUT | `/api/reservas/{id}/aprobar` | SUP | Revalida el cruce (RN-04) | HU-14 |
| PUT | `/api/reservas/{id}/rechazar` | SUP | `{ motivo }` (RN-13) | HU-14 |

### Catálogos (lectura)

| Método | Ruta | Rol | Descripción |
|---|---|---|---|
| GET | `/api/catalogos/categorias` | * | Categorías activas |
| GET | `/api/catalogos/tipos-solicitud?categoriaId=` | * | Tipos activos de una categoría (incluye `requiereAdjunto`) |
| GET | `/api/catalogos/prioridades` | * | Prioridades |
| GET | `/api/catalogos/estados?ambito=` | * | Estados |
| GET | `/api/usuarios/tecnicos?areaId=` | SUP | Técnicos activos de un área (HU-08) |

### Notificaciones

| Método | Ruta | Rol | Descripción | HU |
|---|---|---|---|---|
| GET | `/api/notificaciones?leida=` | * | Notificaciones propias, paginadas | HU-17 |
| GET | `/api/notificaciones/no-leidas/cantidad` | * | `{ cantidad }` para la campana | HU-17 |
| PUT | `/api/notificaciones/{id}/leida` | * | Marca una como leída (solo las propias) | HU-17 |
| PUT | `/api/notificaciones/leer-todas` | * | Marca todas las propias como leídas | HU-17 |

### Dashboard

| Método | Ruta | Rol | Descripción | HU |
|---|---|---|---|---|
| GET | `/api/dashboard?desde=&hasta=` | SUP, ADM | Indicadores del período | HU-11 |

### Administración

Todos con rol `ADM`. CRUD estándar (`GET` lista, `GET /{id}`, `POST`, `PUT /{id}`) más `PUT /{id}/activo` para activar o desactivar. No hay `DELETE` sobre catálogos.

| Recurso | Ruta |
|---|---|
| Usuarios | `/api/admin/usuarios` |
| Áreas | `/api/admin/areas` |
| Categorías | `/api/admin/categorias` |
| Tipos de solicitud | `/api/admin/tipos-solicitud` |
| Prioridades | `/api/admin/prioridades` |
| Estados | `/api/admin/estados` |
| Transiciones | `/api/admin/transiciones` |
| Ambientes | `/api/admin/ambientes` |

## Ejemplo: registrar una solicitud

```http
POST /api/solicitudes
Authorization: Bearer eyJhbGciOi...
Content-Type: multipart/form-data; boundary=----X

------X
Content-Disposition: form-data; name="datos"
Content-Type: application/json

{ "tipoSolicitudId": 1, "prioridadId": 2, "ubicacion": "Pabellón Antiguo, Salón A-204",
  "descripcion": "El proyector no enciende" }
------X
Content-Disposition: form-data; name="archivos"; filename="foto1.jpg"
Content-Type: image/jpeg

(bytes)
------X--
```

Respuesta `201 Created`, `Location: /api/solicitudes/15`:

```json
{
  "id": 15,
  "codigo": "SOL-2026-0015",
  "estado": "Registrada",
  "fechaRegistro": "2026-10-17T09:30:00",
  "fechaLimite": "2026-10-18T09:30:00"
}
```
