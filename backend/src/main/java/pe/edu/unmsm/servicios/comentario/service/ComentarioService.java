package pe.edu.unmsm.servicios.comentario.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.edu.unmsm.servicios.comentario.dto.ComentarioDtos.ComentarioResponse;
import pe.edu.unmsm.servicios.comentario.dto.ComentarioDtos.CrearComentarioRequest;
import pe.edu.unmsm.servicios.comentario.entity.Comentario;
import pe.edu.unmsm.servicios.comentario.event.ComentarioCreadoEvent;
import pe.edu.unmsm.servicios.comentario.repository.ComentarioRepository;
import pe.edu.unmsm.servicios.common.BusinessException;
import pe.edu.unmsm.servicios.solicitud.entity.Solicitud;
import pe.edu.unmsm.servicios.solicitud.repository.SolicitudRepository;
import pe.edu.unmsm.servicios.solicitud.service.VisibilidadSolicitudes;
import pe.edu.unmsm.servicios.usuario.entity.Usuario;
import pe.edu.unmsm.servicios.usuario.repository.UsuarioRepository;

@Service
public class ComentarioService {

    private final ComentarioRepository comentarios;
    private final SolicitudRepository solicitudes;
    private final UsuarioRepository usuarios;
    private final VisibilidadSolicitudes visibilidad;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    public ComentarioService(ComentarioRepository comentarios, SolicitudRepository solicitudes,
            UsuarioRepository usuarios, VisibilidadSolicitudes visibilidad, ApplicationEventPublisher events,
            Clock clock) {
        this.comentarios = comentarios;
        this.solicitudes = solicitudes;
        this.usuarios = usuarios;
        this.visibilidad = visibilidad;
        this.events = events;
        this.clock = clock;
    }

    /** HU-10: lists comments in chronological order for users who can see the request (RN-20). */
    @Transactional(readOnly = true)
    public List<ComentarioResponse> listar(Long usuarioId, Long solicitudId) {
        visible(usuarioId, solicitudId);
        return comentarios.findBySolicitudIdOrderByFechaAscIdAsc(solicitudId).stream()
                .map(ComentarioResponse::de)
                .toList();
    }

    /** HU-10: adds a comment to a visible request and publishes ComentarioCreadoEvent (RN-16). */
    @Transactional
    public ComentarioResponse crear(Long usuarioId, Long solicitudId, CrearComentarioRequest datos) {
        Usuario autor = buscarUsuario(usuarioId);
        Solicitud solicitud = visible(autor, solicitudId);
        LocalDateTime ahora = LocalDateTime.now(clock).truncatedTo(ChronoUnit.SECONDS);

        Comentario guardado = comentarios.save(new Comentario(solicitud, autor, datos.texto().trim(), ahora));
        events.publishEvent(new ComentarioCreadoEvent(solicitud.getId(), guardado.getId(), autor.getId()));
        return ComentarioResponse.de(guardado);
    }

    private Solicitud visible(Long usuarioId, Long solicitudId) {
        return visible(buscarUsuario(usuarioId), solicitudId);
    }

    private Solicitud visible(Usuario usuario, Long solicitudId) {
        return solicitudes.buscarDetalle(solicitudId)
                .filter(s -> visibilidad.puedeVer(usuario, s))
                .orElseThrow(() -> BusinessException.notFound("Solicitud no encontrada",
                        "La solicitud no existe."));
    }

    private Usuario buscarUsuario(Long usuarioId) {
        return usuarios.findById(usuarioId)
                .orElseThrow(() -> BusinessException.notFound("Usuario no encontrado", "El usuario ya no existe."));
    }
}
