-- V2: catalog seed data (docs/05-modelo-datos.md). No users are seeded:
-- the initial administrator is created at startup from environment variables.

INSERT INTO rol (nombre) VALUES
    ('SOLICITANTE'), ('TECNICO'), ('PERSONAL_AREA'), ('SUPERVISOR'), ('ADMIN');

INSERT INTO area (nombre) VALUES
    ('Soporte técnico'), ('Secretaría académica'), ('Oficina de matrícula');

INSERT INTO categoria (nombre) VALUES
    ('Soporte técnico e infraestructura'), ('Rectificaciones'), ('Matrícula');

INSERT INTO prioridad (nombre, orden) VALUES
    ('Alta', 1), ('Media', 2), ('Baja', 3);

INSERT INTO estado (nombre, ambito, orden, es_final) VALUES
    ('Registrada',    'AMBOS',     1, FALSE),
    ('En evaluación', 'SOLICITUD', 2, FALSE),
    ('Asignada',      'SOLICITUD', 3, FALSE),
    ('En atención',   'SOLICITUD', 4, FALSE),
    ('Resuelta',      'SOLICITUD', 5, FALSE),
    ('Cerrada',       'SOLICITUD', 6, TRUE),
    ('Aprobada',      'RESERVA',   7, TRUE),
    ('Rechazada',     'AMBOS',     8, TRUE);

-- Request types. SLA hours are example values pending team confirmation.
INSERT INTO tipo_solicitud (categoria_id, area_id, nombre, requiere_adjunto, sla_horas)
SELECT c.id, a.id, t.nombre, t.requiere_adjunto, t.sla_horas
FROM (
          SELECT 'Soporte técnico e infraestructura' AS categoria, 'Soporte técnico' AS area,
                 'Equipo audiovisual (proyector, parlantes, ecran)' AS nombre, FALSE AS requiere_adjunto, 24 AS sla_horas
    UNION ALL SELECT 'Soporte técnico e infraestructura', 'Soporte técnico', 'Equipo de cómputo (PC, impresora)', FALSE, 48
    UNION ALL SELECT 'Soporte técnico e infraestructura', 'Soporte técnico', 'Red o wifi', FALSE, 24
    UNION ALL SELECT 'Soporte técnico e infraestructura', 'Soporte técnico', 'Infraestructura (luz, mobiliario, aire acondicionado)', FALSE, 72
    UNION ALL SELECT 'Rectificaciones', 'Secretaría académica', 'Rectificación de nota', TRUE, 120
    UNION ALL SELECT 'Rectificaciones', 'Secretaría académica', 'Rectificación de datos personales', TRUE, 72
    UNION ALL SELECT 'Rectificaciones', 'Secretaría académica', 'Corrección de nombre en documentos', TRUE, 120
    UNION ALL SELECT 'Matrícula', 'Oficina de matrícula', 'Matrícula extemporánea', TRUE, 48
    UNION ALL SELECT 'Matrícula', 'Oficina de matrícula', 'Retiro de curso', TRUE, 72
    UNION ALL SELECT 'Matrícula', 'Oficina de matrícula', 'Reserva de matrícula', TRUE, 72
    UNION ALL SELECT 'Matrícula', 'Oficina de matrícula', 'Cambio de sección u horario', TRUE, 48
) t
JOIN categoria c ON c.nombre = t.categoria
JOIN area a ON a.nombre = t.area;

-- State transitions (RN-05). rol = NULL means the system executes it.
INSERT INTO estado_transicion (estado_origen_id, estado_destino_id, rol_id, ambito, requiere_motivo)
SELECT o.id, d.id, r.id, t.ambito, t.requiere_motivo
FROM (
          SELECT 'SOLICITUD' AS ambito, 'Registrada' AS origen, 'En evaluación' AS destino, 'SUPERVISOR' AS rol, FALSE AS requiere_motivo
    UNION ALL SELECT 'SOLICITUD', 'En evaluación', 'Asignada',      'SUPERVISOR',    FALSE
    UNION ALL SELECT 'SOLICITUD', 'En evaluación', 'Rechazada',     'SUPERVISOR',    TRUE
    UNION ALL SELECT 'SOLICITUD', 'Asignada',      'En atención',   'TECNICO',       FALSE
    UNION ALL SELECT 'SOLICITUD', 'En atención',   'Resuelta',      'TECNICO',       FALSE
    UNION ALL SELECT 'SOLICITUD', 'Registrada',    'En evaluación', NULL,            FALSE
    UNION ALL SELECT 'SOLICITUD', 'En evaluación', 'En atención',   'PERSONAL_AREA', FALSE
    UNION ALL SELECT 'SOLICITUD', 'En atención',   'Resuelta',      'PERSONAL_AREA', FALSE
    UNION ALL SELECT 'SOLICITUD', 'En atención',   'Rechazada',     'PERSONAL_AREA', TRUE
    UNION ALL SELECT 'SOLICITUD', 'Resuelta',      'Cerrada',       'SOLICITANTE',   FALSE
    UNION ALL SELECT 'RESERVA',   'Registrada',    'Aprobada',      'SUPERVISOR',    FALSE
    UNION ALL SELECT 'RESERVA',   'Registrada',    'Rechazada',     'SUPERVISOR',    TRUE
) t
JOIN estado o ON o.nombre = t.origen
JOIN estado d ON d.nombre = t.destino
LEFT JOIN rol r ON r.nombre = t.rol;

-- Rooms: two 4-floor pavilions (floors 1-2 classrooms, 3-4 labs, 15 per floor) and the central auditorium.
INSERT INTO ambiente (nombre, tipo, pabellon, piso, capacidad)
WITH RECURSIVE numero (n) AS (
    SELECT 1 UNION ALL SELECT n + 1 FROM numero WHERE n < 15
),
piso (p) AS (
    SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
),
pabellon (codigo, letra) AS (
    SELECT 'ANTIGUO', 'A' UNION ALL SELECT 'NUEVO', 'N'
)
SELECT CONCAT(IF(piso.p <= 2, 'Salón ', 'Laboratorio '), pabellon.letra, '-', piso.p, LPAD(numero.n, 2, '0')),
       IF(piso.p <= 2, 'SALON', 'LABORATORIO'),
       pabellon.codigo,
       piso.p,
       30
FROM pabellon
CROSS JOIN piso
CROSS JOIN numero
ORDER BY pabellon.codigo, piso.p, numero.n;

INSERT INTO ambiente (nombre, tipo, pabellon, piso, capacidad)
VALUES ('Auditorio', 'AUDITORIO', 'CENTRAL', NULL, 300);
