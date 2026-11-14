package pe.edu.unmsm.servicios.comentario.event;

/** Published when a user posts a comment on a request (RN-16). */
public record ComentarioCreadoEvent(Long solicitudId, Long comentarioId, Long autorId) {
}
