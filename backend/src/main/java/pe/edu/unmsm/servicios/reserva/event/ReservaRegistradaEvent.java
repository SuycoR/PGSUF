package pe.edu.unmsm.servicios.reserva.event;

public record ReservaRegistradaEvent(
        Long reservaId,
        Long solicitanteId,
        String ambienteNombre,
        String codigo
) {
}
