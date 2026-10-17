# E6 — Trámites académicos

Gestionar rectificaciones y solicitudes de matrícula con documentos de sustento y la respuesta del área responsable.

Los trámites usan la misma tabla `solicitud` que las solicitudes técnicas; los distingue su categoría y el área del tipo de solicitud.

Reglas relacionadas: RN-05, RN-06, RN-08, RN-09, RN-12, RN-13 ([06-reglas-negocio.md](../06-reglas-negocio.md)).

---

## HU-15. Registrar un trámite académico

**Como** estudiante, **quiero** registrar una rectificación o una solicitud de matrícula adjuntando el documento de sustento **para** que el área responsable la evalúe.

| Escenario | Dado | Cuando | Entonces |
|---|---|---|---|
| 1. Trámite correcto | que el estudiante está autenticado | elige el tipo (ej. rectificación de nota), completa los datos y adjunta el sustento | el sistema registra el trámite con código único y lo deriva automáticamente a su área, en estado "En evaluación" |
| 2. Sin documento | que el tipo de trámite requiere sustento | el estudiante lo envía sin adjuntar archivo | el sistema muestra "Debe adjuntar el documento de sustento" y no lo registra |
| 3. Archivo no permitido | que el estudiante adjunta un archivo | el formato no es PDF, JPG o PNG, o pesa más de 5 MB | el sistema rechaza el archivo e indica los formatos y el tamaño permitidos |

| ID | Capa | Tarea |
|---|---|---|
| T-42 | BD | Crear tabla `evidencia` (y `tipo_solicitud` con `requiere_adjunto`, si no se creó en E2) |
| T-43 | Backend | Validar y guardar archivos en `POST /api/solicitudes` (multipart) y `POST /api/solicitudes/{id}/evidencias` |
| T-44 | Frontend | Mostrar el campo de adjuntos como obligatorio según el tipo de trámite |

**Notas de implementación**
- Ajuste respecto del informe parcial: el escenario 1 decía "estado Registrada". Con la derivación automática (RN-09) el trámite queda en **"En evaluación"**; el historial guarda los dos pasos (Registrada → En evaluación).
- El tipo de archivo se valida por su contenido, no solo por la extensión (RN-12).

---

## HU-16. Resolver un trámite académico

**Como** personal del área responsable, **quiero** tomar un trámite y aprobarlo o rechazarlo registrando una respuesta **para** que el estudiante conozca el resultado.

| Escenario | Dado | Cuando | Entonces |
|---|---|---|---|
| 1. Tomar trámite | que el trámite está "En evaluación" y pertenece a su área | lo toma | el estado pasa a "En atención" y el sistema lo registra como responsable |
| 2. Trámite resuelto | que el trámite está "En atención" y es su responsable | registra la respuesta y lo marca como "Resuelta" | el estudiante ve el nuevo estado y la respuesta |
| 3. Trámite rechazado | que el trámite no cumple los requisitos | lo rechaza indicando el motivo | el estado cambia a "Rechazada" con el motivo visible |
| 4. Área ajena | que el trámite pertenece a otra área | intenta tomarlo o resolverlo | el sistema responde 403 y no lo modifica |

| ID | Capa | Tarea |
|---|---|---|
| T-45 | Backend | Implementar tomar, resolver y rechazar mediante `PUT /api/solicitudes/{id}/estado` (con `respuesta` o `motivo`) |
| T-46 | Frontend | Crear bandeja por área y formulario de respuesta |

**Notas de implementación**
- Ajuste respecto del informe parcial: T-45 mencionaba un endpoint `/resolver`. Se unifica en `/estado` para no duplicar la lógica de transiciones.
- La respuesta (o el motivo de rechazo) se guarda en `solicitud.respuesta`.
