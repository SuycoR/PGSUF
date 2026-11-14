package pe.edu.unmsm.servicios.solicitud.event;

/** Published when a supervisor assigns a request to a technician (RN-16). */
public record SolicitudAsignadaEvent(Long solicitudId, Long tecnicoId, Long supervisorId) {
}
