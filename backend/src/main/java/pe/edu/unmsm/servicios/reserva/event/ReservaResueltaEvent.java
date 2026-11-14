package pe.edu.unmsm.servicios.reserva.event;

public record ReservaResueltaEvent(
        Long reservaId,
        Long solicitanteId,
        String estado,
        String motivoRechazo,
        String codigo
) {
}
