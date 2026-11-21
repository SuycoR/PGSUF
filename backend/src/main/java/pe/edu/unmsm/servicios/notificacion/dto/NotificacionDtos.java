package pe.edu.unmsm.servicios.notificacion.dto;

import java.time.LocalDateTime;

import pe.edu.unmsm.servicios.notificacion.entity.Notificacion;
import pe.edu.unmsm.servicios.notificacion.entity.TipoNotificacion;

/** Request and response contracts for notifications (HU-17, docs/07-api.md). */
public final class NotificacionDtos {

    private NotificacionDtos() {
    }

    public record NotificacionResponse(
            Long id,
            TipoNotificacion tipo,
            String titulo,
            String mensaje,
            Long solicitudId,
            Long reservaId,
            boolean leida,
            LocalDateTime fechaCreacion,
            LocalDateTime fechaLectura) {

        public static NotificacionResponse de(Notificacion n) {
            return new NotificacionResponse(
                    n.getId(),
                    n.getTipo(),
                    n.getTitulo(),
                    n.getMensaje(),
                    n.getSolicitudId(),
                    n.getReservaId(),
                    n.isLeida(),
                    n.getFechaCreacion(),
                    n.getFechaLectura());
        }
    }

    public record CantidadNoLeidasResponse(long cantidad) {
    }
}
