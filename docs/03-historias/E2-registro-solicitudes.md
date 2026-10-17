# E2 — Registro de solicitudes

Permitir que los usuarios registren y consulten sus solicitudes de servicio.

Reglas relacionadas: RN-01, RN-02, RN-12, RN-14, RN-20 ([06-reglas-negocio.md](../06-reglas-negocio.md)).

---

## HU-04. Registrar una solicitud

**Como** estudiante, docente o administrativo, **quiero** registrar una solicitud de soporte técnico (por ejemplo, un proyector malogrado) **para** informar un problema existente en mi facultad.

| Escenario | Dado | Cuando | Entonces |
|---|---|---|---|
| 1. Registro correcto | que el solicitante está autenticado | registra una solicitud con todos los datos obligatorios | el sistema la guarda, genera un código único (ej. `SOL-2026-0001`) y muestra el estado "Registrada" |
| 2. Datos incompletos | que el solicitante está registrando una solicitud | omite un campo obligatorio | el sistema muestra un mensaje de validación y no la registra |
| 3. Error del servidor | que el servicio no está disponible | el solicitante envía la solicitud | el sistema muestra un mensaje de error y conserva los datos ingresados |

**Campos ingresados por el usuario:** categoría, tipo de solicitud, descripción, ubicación, prioridad y evidencias (opcionales en soporte técnico). El código, la fecha de registro, la fecha límite, el estado y el solicitante se generan automáticamente.

| ID | Capa | Tarea |
|---|---|---|
| T-11 | BD | Crear tablas `categoria`, `tipo_solicitud`, `prioridad`, `estado`, `solicitud`, `secuencia_codigo` |
| T-12 | Backend | Crear entidad, repositorio, servicio y DTO de `Solicitud` |
| T-13 | Backend | Crear endpoint `POST /api/solicitudes` (multipart) con generación de código único (RN-01) |
| T-14 | Frontend | Crear formulario de nueva solicitud; el selector de tipos cambia según la categoría |
| T-15 | Frontend | Conectar el formulario con la API mediante `fetch` |

**Notas de implementación**
- Al registrar, en la misma transacción: generar el código (RN-01), calcular `fecha_limite` (RN-02) e insertar el primer registro de `historial_estado` (RN-14).
- La notificación al supervisor se crea después del commit (RN-16).

---

## HU-05. Ver mis solicitudes y su estado

**Como** solicitante, **quiero** consultar mis solicitudes y su estado **para** hacer seguimiento a su atención.

| Escenario | Dado | Cuando | Entonces |
|---|---|---|---|
| 1. Con solicitudes | que el solicitante tiene solicitudes registradas | entra a "Mis solicitudes" | el sistema muestra la lista con código, categoría, tipo, fecha y estado |
| 2. Sin solicitudes | que el solicitante no registró ninguna | entra a "Mis solicitudes" | el sistema muestra "Aún no tienes solicitudes" |
| 3. Privacidad | que existen solicitudes de otros usuarios | el solicitante consulta su lista | el sistema solo muestra las suyas |

| ID | Capa | Tarea |
|---|---|---|
| T-16 | Backend | Crear endpoint `GET /api/solicitudes/mis-solicitudes` (paginado) |
| T-17 | Frontend | Crear la lista de solicitudes con renderizado de estados |

---

## HU-06. Ver el detalle de una solicitud

**Como** usuario involucrado en una solicitud, **quiero** ver su información completa **para** conocer su historial y responsable.

| Escenario | Dado | Cuando | Entonces |
|---|---|---|---|
| 1. Detalle visible | que existe una solicitud accesible para el usuario | la selecciona | el sistema muestra sus datos, evidencias, responsable, comentarios e historial |
| 2. Solicitud inexistente | que el identificador no existe | intenta abrirla | el sistema responde 404 y muestra "Solicitud no encontrada" |
| 3. Solicitud ajena | que la solicitud no involucra al usuario | intenta abrirla | el sistema responde 404 (no revela que existe) |

| ID | Capa | Tarea |
|---|---|---|
| T-18 | Backend | Crear endpoints `GET /api/solicitudes/{id}` y `GET /api/solicitudes/{id}/historial` |
| T-19 | Frontend | Crear pantalla de detalle de solicitud |

**Notas de implementación**
- Quién puede ver una solicitud está definido en RN-20. Para una solicitud ajena se responde 404, no 403, para no revelar su existencia.
