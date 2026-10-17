# Registro de uso de IA

Requisito transversal de la guía del curso: por cada uso significativo de IA se registra la herramienta, el prompt, el resultado, si se usó, cómo se validó, las modificaciones y el responsable. El objetivo es demostrar que la IA se usó como herramienta de ingeniería y no como sustituto del aprendizaje.

## Plantilla

Copiar este bloque para cada registro nuevo:

```markdown
### IA-NN — <título corto>

| Campo | Registro |
|---|---|
| Fecha | AAAA-MM-DD |
| Herramienta | |
| Prompt | |
| Resultado generado | |
| ¿Se utilizó? | Sí / Parcialmente / No |
| Validación realizada | |
| Modificaciones | |
| Responsable | |
```

---

### IA-01 — Informe parcial (Fase I)

| Campo | Registro |
|---|---|
| Fecha | 2026-10-17 |
| Herramienta | Claude (Claude Code) |
| Prompt | Resumir lo que pide la primera presentación según la guía del curso y completar el informe parcial: actores, clasificación de solicitudes, requerimientos, épicas de reservas y trámites, arquitectura preliminar y wireframes. |
| Resultado generado | Secciones del informe, tablas de requerimientos, historias con escenarios BDD, diagrama de arquitectura y wireframes. |
| ¿Se utilizó? | Parcialmente: se ajustó a las decisiones del equipo (stack de la guía y tipos de solicitud definidos por el grupo). |
| Validación realizada | Revisión manual contra la guía del docente (PDF) y revisión del equipo. |
| Modificaciones | [Completar] |
| Responsable | Suyco Rivera Pedro Jesus |

### IA-02 — Modelo de datos

| Campo | Registro |
|---|---|
| Fecha | 2026-10-17 |
| Herramienta | Claude (Claude Code); se contrastó con una propuesta generada en otra sesión de Claude |
| Prompt | Diseñar el modelo de datos en Mermaid a partir de las historias de usuario; evaluar la propuesta alternativa e integrar sus mejoras. |
| Resultado generado | Diagrama entidad-relación, índices, restricciones, reglas de negocio y datos iniciales de transiciones. |
| ¿Se utilizó? | Parcialmente: se adoptaron `estado_transicion`, `secuencia_codigo`, `evidencia.usuario_id` y `reserva.fecha_resolucion`; se descartó el cierre automático; se eliminaron índices duplicados y se cambió la estrategia de bloqueo. |
| Validación realizada | Revisión técnica de cada propuesta (concurrencia, índices de InnoDB, soporte de `CHECK` en MySQL 9) y decisión del equipo. |
| Modificaciones | [Completar] |
| Responsable | Suyco Rivera Pedro Jesus |

### IA-03 — Documentación para la construcción

| Campo | Registro |
|---|---|
| Fecha | 2026-10-17 |
| Herramienta | Claude (Claude Code) |
| Prompt | Generar la documentación en Markdown para construir la aplicación: visión, requerimientos, historias por épica, arquitectura, modelo de datos, reglas de negocio, API, configuración y plan de construcción. |
| Resultado generado | Carpeta `docs/` con 16 archivos. |
| ¿Se utilizó? | [Completar tras la revisión del equipo] |
| Validación realizada | [Completar] |
| Modificaciones | [Completar] |
| Responsable | Suyco Rivera Pedro Jesus |

### IA-05 — Construcción del sprint S2

| Campo | Registro |
|---|---|
| Fecha | 2026-10-17 |
| Herramienta | Claude (Claude Code) |
| Prompt | Construir el registro y la consulta de solicitudes (HU-04, HU-05, HU-06) con evidencias, códigos únicos, fecha límite, historial y derivación automática de trámites, según supuestos confirmados por el equipo. |
| Resultado generado | Endpoints de solicitudes, evidencias y catálogos; validación de archivos por contenido; pantallas de nueva solicitud, listado y detalle; pruebas unitarias, de integración y de concurrencia. |
| ¿Se utilizó? | [Completar tras la revisión del equipo] |
| Validación realizada | 68 pruebas de backend y 28 de frontend; pruebas manuales con archivos reales (PDF válido, ejecutable renombrado), visibilidad por rol y recorrido completo en el navegador. Se detectó y corrigió un desfase de fracciones de segundo entre la respuesta y la base de datos. |
| Modificaciones | [Completar] |
| Responsable | Suyco Rivera Pedro Jesus |

### IA-04 — Construcción de los sprints S0 y S1

| Campo | Registro |
|---|---|
| Fecha | 2026-10-17 |
| Herramienta | Claude (Claude Code) |
| Prompt | Construir la base del proyecto (S0) y la autenticación (S1) según la documentación de `docs/`, con supuestos confirmados por el equipo antes de generar. |
| Resultado generado | Backend Spring Boot (seguridad JWT, migraciones Flyway, registro, login, creación de usuarios por el administrador) y frontend React + TypeScript (layout, rutas por rol, login y registro), con pruebas automatizadas. |
| ¿Se utilizó? | [Completar tras la revisión del equipo] |
| Validación realizada | 37 pruebas de backend y 18 de frontend; pruebas manuales de la API (casos válidos, errores 400/401/403/409/422, tokens alterados y falsificados) y del flujo completo en el navegador. |
| Modificaciones | [Completar] |
| Responsable | Suyco Rivera Pedro Jesus |
