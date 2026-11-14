package pe.edu.unmsm.servicios.solicitud.event;

/** Published when a request changes its state (RN-16). */
public record EstadoCambiadoEvent(Long solicitudId, String estadoAnterior, String estadoNuevo, Long actorId) {
}
