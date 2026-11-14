package pe.edu.unmsm.servicios.notificacion.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import pe.edu.unmsm.servicios.comentario.event.ComentarioCreadoEvent;
import pe.edu.unmsm.servicios.common.BusinessException;
import pe.edu.unmsm.servicios.notificacion.dto.NotificacionDtos.CantidadNoLeidasResponse;
import pe.edu.unmsm.servicios.notificacion.dto.NotificacionDtos.NotificacionResponse;
import pe.edu.unmsm.servicios.notificacion.entity.Notificacion;
import pe.edu.unmsm.servicios.notificacion.entity.TipoNotificacion;
import pe.edu.unmsm.servicios.notificacion.repository.NotificacionRepository;
import pe.edu.unmsm.servicios.reserva.event.ReservaRegistradaEvent;
import pe.edu.unmsm.servicios.reserva.event.ReservaResueltaEvent;
import pe.edu.unmsm.servicios.solicitud.entity.Solicitud;
import pe.edu.unmsm.servicios.solicitud.event.EstadoCambiadoEvent;
import pe.edu.unmsm.servicios.solicitud.event.SolicitudAsignadaEvent;
import pe.edu.unmsm.servicios.solicitud.repository.SolicitudRepository;
import pe.edu.unmsm.servicios.usuario.entity.Usuario;
import pe.edu.unmsm.servicios.usuario.repository.UsuarioRepository;

@Service
public class NotificacionService {

    private static final Logger log = LoggerFactory.getLogger(NotificacionService.class);

    private final NotificacionRepository notificaciones;
    private final SolicitudRepository solicitudes;
    private final UsuarioRepository usuarios;
    private final Clock clock;

    public NotificacionService(NotificacionRepository notificaciones, SolicitudRepository solicitudes,
            UsuarioRepository usuarios, Clock clock) {
        this.notificaciones = notificaciones;
        this.solicitudes = solicitudes;
        this.usuarios = usuarios;
        this.clock = clock;
    }

    /** HU-17: lists the authenticated user's notifications, newest first. */
    @Transactional(readOnly = true)
    public Page<NotificacionResponse> listar(Long usuarioId, Boolean leida, Pageable pageable) {
        return notificaciones.buscarDelUsuario(usuarioId, leida, pageable)
                .map(NotificacionResponse::de);
    }

    /** HU-17: unread counter for the header bell. */
    @Transactional(readOnly = true)
    public CantidadNoLeidasResponse cantidadNoLeidas(Long usuarioId) {
        return new CantidadNoLeidasResponse(notificaciones.countByUsuarioIdAndLeidaFalse(usuarioId));
    }

    /** HU-17: marks a single notification as read if it belongs to the authenticated user. */
    @Transactional
    public void marcarLeida(Long usuarioId, Long notificacionId) {
        Notificacion notificacion = notificaciones.findById(notificacionId)
                .orElseThrow(() -> BusinessException.notFound("Notificación no encontrada",
                        "La notificación no existe."));
        if (!Objects.equals(notificacion.getUsuario().getId(), usuarioId)) {
            throw BusinessException.forbidden("No tiene permiso para modificar esta notificación.");
        }
        notificacion.marcarLeida(ahora());
    }

    /** HU-17: marks all unread notifications of the authenticated user as read. */
    @Transactional
    public void marcarTodasLeidas(Long usuarioId) {
        notificaciones.marcarTodasComoLeidas(usuarioId, ahora());
    }

    /** RN-16: persists notifications for a technician assignment in a new transaction after commit. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void notificarAsignacion(SolicitudAsignadaEvent event) {
        Solicitud solicitud = buscarSolicitud(event.solicitudId());
        if (solicitud == null) {
            return;
        }
        List<Long> destinatarios = DestinatariosNotificacion.paraAsignacion(
                solicitud.getUsuario().getId(), event.tecnicoId());
        if (destinatarios.isEmpty()) {
            return;
        }

        Map<Long, Usuario> mapa = cargarUsuarios(destinatarios);
        String tecnicoNombre = solicitud.getResponsable() != null
                ? solicitud.getResponsable().getNombre()
                : usuarios.findById(event.tecnicoId()).map(Usuario::getNombre).orElse("técnico");
        String titulo = "Solicitud asignada: " + solicitud.getCodigo();
        LocalDateTime ahora = ahora();

        for (Long destinatarioId : destinatarios) {
            Usuario dest = mapa.get(destinatarioId);
            if (dest == null) {
                continue;
            }
            String mensaje = Objects.equals(destinatarioId, event.tecnicoId())
                    ? "Se te asignó la solicitud " + solicitud.getCodigo()
                            + " (" + solicitud.getTipoSolicitud().getNombre() + ")."
                    : "Tu solicitud " + solicitud.getCodigo() + " fue asignada a " + tecnicoNombre + ".";
            notificaciones.save(Notificacion.paraSolicitud(dest, TipoNotificacion.SOLICITUD_ASIGNADA,
                    titulo, mensaje, solicitud.getId(), ahora));
        }
    }

    /** RN-16: persists notifications for a request state change in a new transaction after commit. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void notificarCambioEstado(EstadoCambiadoEvent event) {
        Solicitud solicitud = buscarSolicitud(event.solicitudId());
        if (solicitud == null) {
            return;
        }
        Long responsableId = solicitud.getResponsable() != null ? solicitud.getResponsable().getId() : null;
        List<Long> destinatarios = DestinatariosNotificacion.paraCambioEstado(
                solicitud.getUsuario().getId(), responsableId, event.actorId());
        if (destinatarios.isEmpty()) {
            return;
        }

        String titulo = "Estado actualizado: " + solicitud.getCodigo();
        String mensaje = "La solicitud " + solicitud.getCodigo()
                + " pasó de '" + event.estadoAnterior() + "' a '" + event.estadoNuevo() + "'.";
        guardarParaTodos(destinatarios, TipoNotificacion.ESTADO_CAMBIADO, titulo, mensaje, solicitud.getId());
    }

    /** RN-16: persists notifications for a new comment in a new transaction after commit. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void notificarComentario(ComentarioCreadoEvent event) {
        Solicitud solicitud = buscarSolicitud(event.solicitudId());
        if (solicitud == null) {
            return;
        }
        Long responsableId = solicitud.getResponsable() != null ? solicitud.getResponsable().getId() : null;
        List<Long> destinatarios = DestinatariosNotificacion.paraComentario(
                solicitud.getUsuario().getId(), responsableId, event.autorId());
        if (destinatarios.isEmpty()) {
            return;
        }

        String autorNombre = usuarios.findById(event.autorId())
                .map(Usuario::getNombre)
                .orElse("Un usuario");
        String titulo = "Nuevo comentario en " + solicitud.getCodigo();
        String mensaje = autorNombre + " agregó un comentario en la solicitud " + solicitud.getCodigo() + ".";
        guardarParaTodos(destinatarios, TipoNotificacion.COMENTARIO_NUEVO, titulo, mensaje, solicitud.getId());
    }

    /** RN-16: notifies active supervisors when a room reservation is registered. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void notificarRegistroReserva(ReservaRegistradaEvent event) {
        List<Usuario> supervisores = usuarios.buscarActivosPorRolYArea("SUPERVISOR", null);
        if (supervisores.isEmpty()) {
            return;
        }
        LocalDateTime ahora = ahora();
        String titulo = "Nueva reserva: " + event.codigo();
        String mensaje = "Se registró una nueva reserva para el ambiente " + event.ambienteNombre() + ".";
        for (Usuario sup : supervisores) {
            notificaciones.save(Notificacion.paraReserva(sup, TipoNotificacion.RESERVA_REGISTRADA,
                    titulo, mensaje, event.reservaId(), ahora));
        }
    }

    /** RN-16: notifies the reservation applicant when their reservation is approved or rejected. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void notificarResolucionReserva(ReservaResueltaEvent event) {
        Usuario solicitante = usuarios.findById(event.solicitanteId()).orElse(null);
        if (solicitante == null) {
            return;
        }
        LocalDateTime ahora = ahora();
        String titulo = "Reserva " + event.estado().toLowerCase() + ": " + event.codigo();
        String mensaje = "Aprobada".equalsIgnoreCase(event.estado())
                ? "Tu reserva " + event.codigo() + " fue aprobada."
                : "Tu reserva " + event.codigo() + " fue rechazada. Motivo: " + (event.motivoRechazo() != null ? event.motivoRechazo() : "");
        notificaciones.save(Notificacion.paraReserva(solicitante, TipoNotificacion.RESERVA_RESUELTA,
                titulo, mensaje, event.reservaId(), ahora));
    }

    private void guardarParaTodos(List<Long> destinatarios, TipoNotificacion tipo, String titulo, String mensaje,
            Long solicitudId) {
        Map<Long, Usuario> mapa = cargarUsuarios(destinatarios);
        LocalDateTime ahora = ahora();
        for (Long destinatarioId : destinatarios) {
            Usuario dest = mapa.get(destinatarioId);
            if (dest != null) {
                notificaciones.save(Notificacion.paraSolicitud(dest, tipo, titulo, mensaje, solicitudId, ahora));
            }
        }
    }

    private Solicitud buscarSolicitud(Long solicitudId) {
        Solicitud solicitud = solicitudes.buscarDetalle(solicitudId).orElse(null);
        if (solicitud == null) {
            log.warn("Solicitud {} no encontrada al generar notificación", solicitudId);
        }
        return solicitud;
    }

    private Map<Long, Usuario> cargarUsuarios(List<Long> ids) {
        return usuarios.findAllById(ids).stream()
                .collect(Collectors.toMap(Usuario::getId, Function.identity()));
    }

    private LocalDateTime ahora() {
        return LocalDateTime.now(clock).truncatedTo(ChronoUnit.SECONDS);
    }
}
