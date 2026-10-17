-- V1: complete schema (docs/05-modelo-datos.md). Never edit after it has been applied:
-- every change goes in a new migration.

-- ---------------------------------------------------------------- Security and organization
CREATE TABLE rol (
    id     INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(30) NOT NULL,
    CONSTRAINT uk_rol_nombre UNIQUE (nombre)
);

CREATE TABLE area (
    id     INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(80) NOT NULL,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT uk_area_nombre UNIQUE (nombre)
);

CREATE TABLE usuario (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    nombre         VARCHAR(120) NOT NULL,
    correo         VARCHAR(120) NOT NULL,
    password_hash  VARCHAR(100) NOT NULL,
    rol_id         INT NOT NULL,
    area_id        INT NULL,
    activo         BOOLEAN NOT NULL DEFAULT TRUE,
    fecha_creacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_usuario_correo UNIQUE (correo),
    CONSTRAINT fk_usuario_rol FOREIGN KEY (rol_id) REFERENCES rol (id),
    CONSTRAINT fk_usuario_area FOREIGN KEY (area_id) REFERENCES area (id)
);

-- ---------------------------------------------------------------- Request catalogs
CREATE TABLE categoria (
    id     INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(80) NOT NULL,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT uk_categoria_nombre UNIQUE (nombre)
);

CREATE TABLE tipo_solicitud (
    id               INT AUTO_INCREMENT PRIMARY KEY,
    categoria_id     INT NOT NULL,
    area_id          INT NOT NULL,
    nombre           VARCHAR(100) NOT NULL,
    requiere_adjunto BOOLEAN NOT NULL DEFAULT FALSE,
    sla_horas        INT NOT NULL,
    activo           BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT uk_tipo_categoria UNIQUE (categoria_id, nombre),
    CONSTRAINT ck_tipo_sla CHECK (sla_horas > 0),
    CONSTRAINT fk_tipo_categoria FOREIGN KEY (categoria_id) REFERENCES categoria (id),
    CONSTRAINT fk_tipo_area FOREIGN KEY (area_id) REFERENCES area (id)
);

CREATE TABLE prioridad (
    id     INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(20) NOT NULL,
    orden  INT NOT NULL,
    CONSTRAINT uk_prioridad_nombre UNIQUE (nombre)
);

CREATE TABLE estado (
    id       INT AUTO_INCREMENT PRIMARY KEY,
    nombre   VARCHAR(30) NOT NULL,
    ambito   VARCHAR(10) NOT NULL,
    orden    INT NOT NULL,
    es_final BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT uk_estado_nombre UNIQUE (nombre),
    CONSTRAINT ck_estado_ambito CHECK (ambito IN ('SOLICITUD', 'RESERVA', 'AMBOS'))
);

CREATE TABLE estado_transicion (
    id                INT AUTO_INCREMENT PRIMARY KEY,
    estado_origen_id  INT NOT NULL,
    estado_destino_id INT NOT NULL,
    rol_id            INT NULL,           -- NULL = transition executed by the system
    ambito            VARCHAR(10) NOT NULL,
    requiere_motivo   BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT uk_transicion UNIQUE (estado_origen_id, estado_destino_id, rol_id, ambito),
    CONSTRAINT ck_transicion_ambito CHECK (ambito IN ('SOLICITUD', 'RESERVA')),
    CONSTRAINT fk_transicion_origen FOREIGN KEY (estado_origen_id) REFERENCES estado (id),
    CONSTRAINT fk_transicion_destino FOREIGN KEY (estado_destino_id) REFERENCES estado (id),
    CONSTRAINT fk_transicion_rol FOREIGN KEY (rol_id) REFERENCES rol (id)
);

CREATE TABLE secuencia_codigo (
    prefijo      VARCHAR(5) NOT NULL,
    anio         INT NOT NULL,
    ultimo_valor INT NOT NULL DEFAULT 0,
    PRIMARY KEY (prefijo, anio)
);

-- ---------------------------------------------------------------- Requests
CREATE TABLE solicitud (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    codigo            VARCHAR(20) NOT NULL,
    usuario_id        BIGINT NOT NULL,
    tipo_solicitud_id INT NOT NULL,
    prioridad_id      INT NOT NULL,
    estado_id         INT NOT NULL,
    responsable_id    BIGINT NULL,
    descripcion       TEXT NOT NULL,
    ubicacion         VARCHAR(150) NOT NULL,
    respuesta         TEXT NULL,
    fecha_registro    DATETIME NOT NULL,
    fecha_limite      DATETIME NOT NULL,
    fecha_cierre      DATETIME NULL,
    CONSTRAINT uk_solicitud_codigo UNIQUE (codigo),
    CONSTRAINT fk_solicitud_usuario FOREIGN KEY (usuario_id) REFERENCES usuario (id),
    CONSTRAINT fk_solicitud_tipo FOREIGN KEY (tipo_solicitud_id) REFERENCES tipo_solicitud (id),
    CONSTRAINT fk_solicitud_prioridad FOREIGN KEY (prioridad_id) REFERENCES prioridad (id),
    CONSTRAINT fk_solicitud_estado FOREIGN KEY (estado_id) REFERENCES estado (id),
    CONSTRAINT fk_solicitud_responsable FOREIGN KEY (responsable_id) REFERENCES usuario (id)
);

CREATE INDEX idx_solicitud_estado_prio ON solicitud (estado_id, prioridad_id);
CREATE INDEX idx_solicitud_limite ON solicitud (fecha_limite);

CREATE TABLE evidencia (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    solicitud_id   BIGINT NOT NULL,
    usuario_id     BIGINT NOT NULL,
    nombre_archivo VARCHAR(255) NOT NULL,
    ruta           VARCHAR(255) NOT NULL,
    tipo_mime      VARCHAR(50) NOT NULL,
    tamano_bytes   INT NOT NULL,
    fecha_subida   DATETIME NOT NULL,
    CONSTRAINT ck_evidencia_tam CHECK (tamano_bytes <= 5242880),
    CONSTRAINT fk_evidencia_solicitud FOREIGN KEY (solicitud_id) REFERENCES solicitud (id),
    CONSTRAINT fk_evidencia_usuario FOREIGN KEY (usuario_id) REFERENCES usuario (id)
);

CREATE TABLE comentario (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    solicitud_id BIGINT NOT NULL,
    usuario_id   BIGINT NOT NULL,
    texto        TEXT NOT NULL,
    fecha        DATETIME NOT NULL,
    CONSTRAINT fk_comentario_solicitud FOREIGN KEY (solicitud_id) REFERENCES solicitud (id),
    CONSTRAINT fk_comentario_usuario FOREIGN KEY (usuario_id) REFERENCES usuario (id)
);

CREATE TABLE historial_estado (
    id                 BIGINT AUTO_INCREMENT PRIMARY KEY,
    solicitud_id       BIGINT NOT NULL,
    estado_anterior_id INT NULL,
    estado_nuevo_id    INT NOT NULL,
    usuario_id         BIGINT NULL,       -- NULL = change made by the system
    observacion        VARCHAR(500) NULL,
    fecha              DATETIME NOT NULL,
    CONSTRAINT fk_historial_solicitud FOREIGN KEY (solicitud_id) REFERENCES solicitud (id),
    CONSTRAINT fk_historial_anterior FOREIGN KEY (estado_anterior_id) REFERENCES estado (id),
    CONSTRAINT fk_historial_nuevo FOREIGN KEY (estado_nuevo_id) REFERENCES estado (id),
    CONSTRAINT fk_historial_usuario FOREIGN KEY (usuario_id) REFERENCES usuario (id)
);

-- ---------------------------------------------------------------- Reservations
CREATE TABLE ambiente (
    id        INT AUTO_INCREMENT PRIMARY KEY,
    nombre    VARCHAR(80) NOT NULL,
    tipo      VARCHAR(15) NOT NULL,
    pabellon  VARCHAR(10) NOT NULL,
    piso      INT NULL,
    capacidad INT NOT NULL,
    activo    BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT uk_ambiente_nombre UNIQUE (nombre),
    CONSTRAINT ck_ambiente_tipo CHECK (tipo IN ('AUDITORIO', 'LABORATORIO', 'SALON')),
    CONSTRAINT ck_ambiente_pabellon CHECK (pabellon IN ('ANTIGUO', 'NUEVO', 'CENTRAL')),
    CONSTRAINT ck_ambiente_piso CHECK (piso IS NULL OR piso BETWEEN 1 AND 4),
    CONSTRAINT ck_ambiente_capacidad CHECK (capacidad > 0)
);

CREATE TABLE reserva (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    codigo           VARCHAR(20) NOT NULL,
    ambiente_id      INT NOT NULL,
    usuario_id       BIGINT NOT NULL,
    aprobador_id     BIGINT NULL,
    estado_id        INT NOT NULL,
    fecha            DATE NOT NULL,
    hora_inicio      TIME NOT NULL,
    hora_fin         TIME NOT NULL,
    motivo           VARCHAR(255) NOT NULL,
    motivo_rechazo   VARCHAR(255) NULL,
    fecha_registro   DATETIME NOT NULL,
    fecha_resolucion DATETIME NULL,
    CONSTRAINT uk_reserva_codigo UNIQUE (codigo),
    CONSTRAINT ck_reserva_horas CHECK (hora_fin > hora_inicio),
    CONSTRAINT fk_reserva_ambiente FOREIGN KEY (ambiente_id) REFERENCES ambiente (id),
    CONSTRAINT fk_reserva_usuario FOREIGN KEY (usuario_id) REFERENCES usuario (id),
    CONSTRAINT fk_reserva_aprobador FOREIGN KEY (aprobador_id) REFERENCES usuario (id),
    CONSTRAINT fk_reserva_estado FOREIGN KEY (estado_id) REFERENCES estado (id)
);

CREATE INDEX idx_reserva_cruce ON reserva (ambiente_id, fecha, hora_inicio, hora_fin);

-- ---------------------------------------------------------------- Notifications
CREATE TABLE notificacion (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    usuario_id     BIGINT NOT NULL,
    tipo           VARCHAR(30) NOT NULL,
    titulo         VARCHAR(120) NOT NULL,
    mensaje        VARCHAR(500) NOT NULL,
    solicitud_id   BIGINT NULL,
    reserva_id     BIGINT NULL,
    leida          BOOLEAN NOT NULL DEFAULT FALSE,
    fecha_creacion DATETIME NOT NULL,
    fecha_lectura  DATETIME NULL,
    CONSTRAINT ck_notificacion_origen CHECK (solicitud_id IS NULL OR reserva_id IS NULL),
    CONSTRAINT fk_notificacion_usuario FOREIGN KEY (usuario_id) REFERENCES usuario (id),
    CONSTRAINT fk_notificacion_solicitud FOREIGN KEY (solicitud_id) REFERENCES solicitud (id),
    CONSTRAINT fk_notificacion_reserva FOREIGN KEY (reserva_id) REFERENCES reserva (id)
);

CREATE INDEX idx_notificacion_usuario ON notificacion (usuario_id, leida, fecha_creacion);
