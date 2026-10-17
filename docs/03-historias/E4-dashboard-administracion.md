# E4 — Dashboard, administración y notificaciones

Aportar indicadores para la toma de decisiones, gestionar los catálogos del sistema y avisar a cada usuario de los eventos que le conciernen.

Reglas relacionadas: RN-03, RN-16, RN-17 ([06-reglas-negocio.md](../06-reglas-negocio.md)).

---

## HU-11. Ver indicadores de atención

**Como** supervisor, **quiero** ver indicadores de atención **para** medir la calidad del servicio.

**Indicadores (Módulo 4 de la guía del curso):** solicitudes registradas, pendientes, atendidas, por categoría, por prioridad, por responsable, tiempo promedio de atención, porcentaje resuelto y vencidas.

| Escenario | Dado | Cuando | Entonces |
|---|---|---|---|
| 1. Indicadores disponibles | que existen solicitudes registradas | el supervisor abre el dashboard | el sistema muestra los indicadores y gráficos del período elegido |
| 2. Sin datos | que no hay solicitudes en el período | abre el dashboard | el sistema muestra los indicadores en cero |

| ID | Capa | Tarea |
|---|---|---|
| T-32 | Backend | Crear consultas agregadas y endpoint `GET /api/dashboard?desde=&hasta=` |
| T-33 | Frontend | Crear componentes de gráficos e indicadores en React |

**Notas de implementación**
- "Pendientes" = estado no final. "Vencidas" según RN-03. "Tiempo promedio" = promedio de `fecha_cierre - fecha_registro` de las solicitudes cerradas.

---

## HU-12. Gestionar usuarios y catálogos

**Como** administrador, **quiero** gestionar usuarios, roles, categorías, tipos, prioridades, estados, transiciones, ambientes y áreas **para** mantener el sistema configurado.

| Escenario | Dado | Cuando | Entonces |
|---|---|---|---|
| 1. Alta de categoría | que el administrador está autenticado | crea una nueva categoría | el sistema la guarda y queda disponible al registrar solicitudes |
| 2. Desactivar usuario | que existe un usuario activo | el administrador lo desactiva | el usuario ya no puede iniciar sesión |
| 3. Técnico sin área | que el administrador crea un usuario con rol `TECNICO` | no indica el área | el sistema responde 422 y no crea el usuario (RN-10) |

| ID | Capa | Tarea |
|---|---|---|
| T-34 | Backend | Crear CRUD bajo `/api/admin/...` para usuarios y catálogos |
| T-35 | Frontend | Crear pantallas de administración en React |

**Notas de implementación**
- Los catálogos se **desactivan** (`activo = false`), no se borran: hay solicitudes históricas que los referencian.

---

## HU-17. Recibir notificaciones en el sistema

> Historia nueva: no está en el informe parcial; hay que agregarla.

**Como** usuario del sistema, **quiero** recibir notificaciones de los eventos que me conciernen **para** enterarme a tiempo sin revisar cada solicitud.

| Escenario | Dado | Cuando | Entonces |
|---|---|---|---|
| 1. Nueva notificación | que el técnico tiene sesión abierta | el supervisor le asigna una solicitud | en menos de un minuto la campana muestra una notificación nueva |
| 2. Marcar como leída | que el usuario tiene notificaciones sin leer | abre una notificación | el sistema la marca como leída y lo lleva a la solicitud o reserva |
| 3. Marcar todas | que el usuario tiene varias sin leer | elige "Marcar todas como leídas" | el contador de la campana queda en cero |
| 4. Privacidad | que existen notificaciones de otros usuarios | el usuario consulta las suyas | el sistema solo muestra las propias |

| ID | Capa | Tarea |
|---|---|---|
| T-47 | BD | Crear tabla `notificacion` con su índice |
| T-48 | Backend | Publicar eventos de dominio y crear notificaciones con `@TransactionalEventListener(AFTER_COMMIT)` |
| T-49 | Backend | Crear endpoints `/api/notificaciones` |
| T-50 | Frontend | Crear la campana con contador (consulta cada 30–60 s) y la lista de notificaciones |

**Notas de implementación**
- Solo notificaciones **dentro del sistema**; no hay correo electrónico.
- Qué evento notifica a quién está definido en RN-16.
