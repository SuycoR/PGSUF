# 02 — Requerimientos

## Clasificación de solicitudes

Las solicitudes se organizan en tres niveles: **Categoría → Tipo de solicitud → Área responsable**. Las reservas son un caso aparte: no usan `tipo_solicitud`, sino sus propias tablas `ambiente` y `reserva` (ver [05-modelo-datos.md](05-modelo-datos.md)).

| Categoría | Tipos de solicitud | Área responsable | Flujo |
|---|---|---|---|
| Soporte técnico e infraestructura | Equipos audiovisuales (proyector, parlantes, ecran); equipos de cómputo (PC, impresora, red o wifi); infraestructura (luz, mobiliario, aire acondicionado) | Soporte técnico | Registrada → En evaluación → Asignada → En atención → Resuelta → Cerrada |
| Reserva de ambientes | Auditorio (único), laboratorios, salones | Supervisor | Registrada → Aprobada / Rechazada |
| Rectificaciones | Rectificación de nota, de datos personales, corrección de nombre en documentos | Secretaría académica | Registrada → En evaluación (automático) → En atención → Resuelta / Rechazada → Cerrada |
| Matrícula | Matrícula extemporánea, retiro de curso, reserva de matrícula, cambio de sección u horario | Oficina de matrícula | Igual que Rectificaciones |

El detalle de qué rol ejecuta cada transición está en [06-reglas-negocio.md](06-reglas-negocio.md#rn-05--transiciones-de-estado).

## Requerimientos funcionales

| ID | Requerimiento | HU |
|---|---|---|
| RF-01 | Iniciar sesión con correo y contraseña, emitiendo un token JWT. | HU-01 |
| RF-02 | Registrar nuevas cuentas de usuario con rol Solicitante. | HU-02 |
| RF-03 | Restringir funcionalidades y endpoints según el rol del usuario. | HU-03 |
| RF-04 | Registrar solicitudes con categoría, tipo, descripción, ubicación, prioridad y evidencias, generando un código único. | HU-04, HU-15 |
| RF-05 | Consultar las solicitudes propias, su estado, su detalle e historial. | HU-05, HU-06 |
| RF-06 | Listar y filtrar solicitudes por estado, categoría y prioridad. | HU-07 |
| RF-07 | Asignar un responsable a cada solicitud técnica. | HU-08 |
| RF-08 | Cambiar el estado según las transiciones permitidas (incluido Rechazada) y guardar el historial. | HU-09, HU-16 |
| RF-09 | Registrar comentarios en una solicitud. | HU-10 |
| RF-10 | Reservar el auditorio, laboratorios o salones validando cruces de horario. | HU-13 |
| RF-11 | Aprobar o rechazar reservas con motivo. | HU-14 |
| RF-12 | Mostrar indicadores de atención en un dashboard. | HU-11 |
| RF-13 | Administrar usuarios, roles, categorías, tipos, prioridades, estados, transiciones, ambientes y áreas. | HU-12 |
| RF-14 | Notificar dentro del sistema los eventos relevantes a cada usuario. | HU-17 |

## Requerimientos no funcionales

| ID | Atributo | Requerimiento |
|---|---|---|
| RNF-01 | Seguridad | Contraseñas con BCrypt, JWT con expiración, validación de entradas, secretos en variables de entorno, prácticas OWASP Top 10. |
| RNF-02 | Usabilidad | Interfaz responsive (móvil y escritorio) con mensajes de validación claros. |
| RNF-03 | Rendimiento | Las operaciones comunes de la API responden en menos de 2 segundos. |
| RNF-04 | Disponibilidad | Aplicación desplegada en la nube mediante contenedores Docker. |
| RNF-05 | Mantenibilidad | Arquitectura por capas, Git, pruebas automatizadas, migraciones versionadas. |
| RNF-06 | Trazabilidad | Historial de cambios de estado con usuario, fecha y hora. |
| RNF-07 | Observabilidad | Logs, métricas y trazas para monitoreo e incidentes. |

## Tabla resumen de historias

| Épica | ID | Historia | Rol | Prioridad |
|---|---|---|---|---|
| E1. Autenticación y acceso | HU-01 | Iniciar sesión | Todos | Alta |
| | HU-02 | Registrarme en el sistema | Solicitante | Alta |
| | HU-03 | Acceder solo a lo que mi rol permite | Todos | Alta |
| E2. Registro de solicitudes | HU-04 | Registrar una solicitud técnica | Solicitante | Alta |
| | HU-05 | Ver mis solicitudes y su estado | Solicitante | Alta |
| | HU-06 | Ver el detalle de una solicitud | Involucrados | Media |
| E3. Gestión de solicitudes | HU-07 | Ver todas las solicitudes y filtrarlas | Supervisor | Alta |
| | HU-08 | Asignar una solicitud a un técnico | Supervisor | Alta |
| | HU-09 | Cambiar el estado de una solicitud | Técnico | Alta |
| | HU-10 | Agregar comentarios a una solicitud | Involucrados | Baja |
| E4. Dashboard, administración y notificaciones | HU-11 | Ver indicadores de atención | Supervisor | Media |
| | HU-12 | Gestionar usuarios y catálogos | Administrador | Media |
| | HU-17 | Recibir notificaciones en el sistema | Todos | Media |
| E5. Reserva de ambientes | HU-13 | Solicitar la reserva de un ambiente | Todos | Alta |
| | HU-14 | Aprobar o rechazar una reserva | Supervisor | Alta |
| E6. Trámites académicos | HU-15 | Registrar un trámite académico | Solicitante | Alta |
| | HU-16 | Resolver un trámite académico | Personal de área | Media |

> **HU-17 es nueva** respecto del informe parcial: surge de la decisión de incluir notificaciones dentro del sistema. Hay que agregarla al informe.
