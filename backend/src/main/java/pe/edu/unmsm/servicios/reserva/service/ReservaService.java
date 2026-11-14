package pe.edu.unmsm.servicios.reserva.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.edu.unmsm.servicios.ambiente.entity.Ambiente;
import pe.edu.unmsm.servicios.ambiente.repository.AmbienteRepository;
import pe.edu.unmsm.servicios.catalogo.entity.Estado;
import pe.edu.unmsm.servicios.catalogo.repository.EstadoRepository;
import pe.edu.unmsm.servicios.common.BusinessException;
import pe.edu.unmsm.servicios.reserva.dto.ReservaDtos.CrearReservaRequest;
import pe.edu.unmsm.servicios.reserva.dto.ReservaDtos.ReservaResponse;
import pe.edu.unmsm.servicios.reserva.dto.ReservaDtos.ReservaResumen;
import pe.edu.unmsm.servicios.reserva.entity.Reserva;
import pe.edu.unmsm.servicios.reserva.event.ReservaRegistradaEvent;
import pe.edu.unmsm.servicios.reserva.event.ReservaResueltaEvent;
import pe.edu.unmsm.servicios.reserva.repository.ReservaRepository;
import pe.edu.unmsm.servicios.solicitud.service.SecuenciaCodigoService;
import pe.edu.unmsm.servicios.usuario.entity.Usuario;
import pe.edu.unmsm.servicios.usuario.repository.UsuarioRepository;

@Service
public class ReservaService {

    private final ReservaRepository reservas;
    private final AmbienteRepository ambientes;
    private final UsuarioRepository usuarios;
    private final EstadoRepository estados;
    private final SecuenciaCodigoService secuencia;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    public ReservaService(
            ReservaRepository reservas,
            AmbienteRepository ambientes,
            UsuarioRepository usuarios,
            EstadoRepository estados,
            SecuenciaCodigoService secuencia,
            ApplicationEventPublisher events,
            Clock clock) {
        this.reservas = reservas;
        this.ambientes = ambientes;
        this.usuarios = usuarios;
        this.estados = estados;
        this.secuencia = secuencia;
        this.events = events;
        this.clock = clock;
    }

    /**
     * HU-13: registers a room reservation with concurrency locking and overlap validation (RN-01, RN-04, RN-17).
     */
    @Transactional
    public ReservaResponse crear(Long usuarioId, CrearReservaRequest request) {
        LocalDateTime ahora = ahora();

        // RN-17: validations
        if (!request.horaFin().isAfter(request.horaInicio())) {
            throw BusinessException.unprocessable("HORARIO_INVALIDO", "Horario inválido",
                    "La hora de fin debe ser posterior a la hora de inicio.");
        }
        LocalDateTime inicio = LocalDateTime.of(request.fecha(), request.horaInicio());
        if (inicio.isBefore(ahora)) {
            throw BusinessException.unprocessable("FECHA_PASADA", "Fecha u hora inválida",
                    "La fecha y hora de la reserva no pueden estar en el pasado.");
        }

        // RN-04: Concurrency control - lock room row first
        Ambiente ambiente = ambientes.findByIdForUpdate(request.ambienteId())
                .orElseThrow(() -> BusinessException.notFound("Ambiente no encontrado",
                        "El ambiente con id " + request.ambienteId() + " no existe."));

        if (!ambiente.isActivo()) {
            throw BusinessException.unprocessable("AMBIENTE_INACTIVO", "Ambiente no disponible",
                    "El ambiente seleccionado no está activo.");
        }

        // RN-04: check conflict against approved reservations
        boolean hayCruce = reservas.existeCruce(ambiente.getId(), request.fecha(), request.horaInicio(), request.horaFin(), null);
        if (hayCruce) {
            throw BusinessException.conflict("CRUCE_HORARIO", "Cruce de horario",
                    "El ambiente ya está reservado en ese horario.");
        }

        Usuario usuario = usuarios.findById(usuarioId)
                .orElseThrow(() -> BusinessException.notFound("Usuario no encontrado", "El solicitante no existe."));

        Estado estadoRegistrada = estados.findByNombreAndAmbitoIn(Estado.REGISTRADA, java.util.List.of("RESERVA", "AMBOS"))
                .orElseThrow(() -> new IllegalStateException("Estado 'Registrada' para reservas no configurado"));

        // RN-01: unique code RES-AAAA-NNNN
        String codigo = secuencia.siguiente("RES", ahora.getYear());

        Reserva reserva = new Reserva(
                codigo,
                ambiente,
                usuario,
                estadoRegistrada,
                request.fecha(),
                request.horaInicio(),
                request.horaFin(),
                request.motivo().trim(),
                ahora
        );
        reserva = reservas.save(reserva);

        // RN-16: notify supervisors post-commit
        events.publishEvent(new ReservaRegistradaEvent(reserva.getId(), usuario.getId(), ambiente.getNombre(), codigo));

        return ReservaResponse.de(reserva);
    }

    /**
     * HU-13 / RN-20: lists the reservations belonging to the authenticated user.
     */
    @Transactional(readOnly = true)
    public Page<ReservaResumen> listarMisReservas(Long usuarioId, Pageable pageable) {
        return reservas.buscarMisReservas(usuarioId, pageable).map(ReservaResumen::de);
    }

    /**
     * HU-14 / RN-20: supervisor and admin inbox with optional filters.
     */
    @Transactional(readOnly = true)
    public Page<ReservaResumen> listarBandeja(Integer estadoId, Integer ambienteId, LocalDate fecha, Pageable pageable) {
        return reservas.buscarBandeja(estadoId, ambienteId, fecha, pageable).map(ReservaResumen::de);
    }

    /**
     * HU-13 / RN-20: obtains reservation detail with visibility rules.
     */
    @Transactional(readOnly = true)
    public ReservaResponse obtenerPorId(Long id, Long usuarioId, String rol) {
        Reserva reserva = reservas.buscarDetalle(id)
                .orElseThrow(() -> BusinessException.notFound("Reserva no encontrada", "La reserva solicitada no existe."));

        boolean esSupervisorOAdmin = "SUPERVISOR".equals(rol) || "ADMIN".equals(rol);
        boolean esSuya = Objects.equals(reserva.getUsuario().getId(), usuarioId);

        if (!esSupervisorOAdmin && !esSuya) {
            throw BusinessException.notFound("Reserva no encontrada", "La reserva solicitada no existe.");
        }

        return ReservaResponse.de(reserva);
    }

    /**
     * HU-14: approves a pending reservation, revalidating room conflict with pessimistic lock (RN-04).
     */
    @Transactional
    public ReservaResponse aprobar(Long id, Long supervisorId) {
        Reserva reserva = reservas.findById(id)
                .orElseThrow(() -> BusinessException.notFound("Reserva no encontrada", "La reserva no existe."));

        if (!Estado.REGISTRADA.equalsIgnoreCase(reserva.getEstado().getNombre())) {
            throw BusinessException.unprocessable("ESTADO_INVALIDO", "Estado inválido",
                    "Solo se pueden aprobar reservas en estado 'Registrada'.");
        }

        // RN-04: pessimistic lock on room before revalidating overlap
        ambientes.findByIdForUpdate(reserva.getAmbiente().getId())
                .orElseThrow(() -> BusinessException.notFound("Ambiente no encontrado", "El ambiente no existe."));

        boolean hayCruce = reservas.existeCruce(
                reserva.getAmbiente().getId(),
                reserva.getFecha(),
                reserva.getHoraInicio(),
                reserva.getHoraFin(),
                reserva.getId()
        );
        if (hayCruce) {
            throw BusinessException.conflict("CRUCE_HORARIO", "Cruce de horario",
                    "El ambiente ya tiene una reserva aprobada en ese horario.");
        }

        Usuario supervisor = usuarios.findById(supervisorId)
                .orElseThrow(() -> BusinessException.notFound("Supervisor no encontrado", "El supervisor no existe."));

        Estado estadoAprobada = estados.findByNombreAndAmbitoIn(Estado.APROBADA, java.util.List.of("RESERVA", "AMBOS"))
                .orElseThrow(() -> new IllegalStateException("Estado 'Aprobada' no configurado"));

        LocalDateTime ahora = ahora();
        reserva.aprobar(supervisor, estadoAprobada, ahora);

        // RN-16: notify applicant post-commit
        events.publishEvent(new ReservaResueltaEvent(reserva.getId(), reserva.getUsuario().getId(), "Aprobada", null, reserva.getCodigo()));

        return ReservaResponse.de(reserva);
    }

    /**
     * HU-14: rejects a reservation with mandatory reason (RN-13).
     */
    @Transactional
    public ReservaResponse rechazar(Long id, Long supervisorId, String motivo) {
        if (motivo == null || motivo.trim().isEmpty()) {
            throw BusinessException.unprocessable("MOTIVO_REQUERIDO", "Motivo requerido",
                    "El motivo de rechazo es obligatorio.");
        }

        Reserva reserva = reservas.findById(id)
                .orElseThrow(() -> BusinessException.notFound("Reserva no encontrada", "La reserva no existe."));

        if (!Estado.REGISTRADA.equalsIgnoreCase(reserva.getEstado().getNombre())) {
            throw BusinessException.unprocessable("ESTADO_INVALIDO", "Estado inválido",
                    "Solo se pueden rechazar reservas en estado 'Registrada'.");
        }

        Usuario supervisor = usuarios.findById(supervisorId)
                .orElseThrow(() -> BusinessException.notFound("Supervisor no encontrado", "El supervisor no existe."));

        Estado estadoRechazada = estados.findByNombreAndAmbitoIn(Estado.RECHAZADA, java.util.List.of("RESERVA", "AMBOS"))
                .orElseThrow(() -> new IllegalStateException("Estado 'Rechazada' no configurado"));

        LocalDateTime ahora = ahora();
        String motivoLimpio = motivo.trim();
        reserva.rechazar(supervisor, estadoRechazada, motivoLimpio, ahora);

        // RN-16: notify applicant post-commit
        events.publishEvent(new ReservaResueltaEvent(reserva.getId(), reserva.getUsuario().getId(), "Rechazada", motivoLimpio, reserva.getCodigo()));

        return ReservaResponse.de(reserva);
    }

    private LocalDateTime ahora() {
        return LocalDateTime.now(clock).truncatedTo(ChronoUnit.SECONDS);
    }
}
