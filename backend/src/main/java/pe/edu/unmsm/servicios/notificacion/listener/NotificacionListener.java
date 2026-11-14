package pe.edu.unmsm.servicios.notificacion.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import pe.edu.unmsm.servicios.comentario.event.ComentarioCreadoEvent;
import pe.edu.unmsm.servicios.notificacion.service.NotificacionService;
import pe.edu.unmsm.servicios.solicitud.event.EstadoCambiadoEvent;
import pe.edu.unmsm.servicios.solicitud.event.SolicitudAsignadaEvent;

/**
 * RN-16: creates notifications after transaction commit so business operations never roll back
 * on notification failures and aborted operations never notify.
 */
@Component
public class NotificacionListener {

    private static final Logger log = LoggerFactory.getLogger(NotificacionListener.class);

    private final NotificacionService notificacionService;

    public NotificacionListener(NotificacionService notificacionService) {
        this.notificacionService = notificacionService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onSolicitudAsignada(SolicitudAsignadaEvent event) {
        ejecutarSinPropagar("asignación de solicitud " + event.solicitudId(),
                () -> notificacionService.notificarAsignacion(event));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onEstadoCambiado(EstadoCambiadoEvent event) {
        ejecutarSinPropagar("cambio de estado de solicitud " + event.solicitudId(),
                () -> notificacionService.notificarCambioEstado(event));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onComentarioCreado(ComentarioCreadoEvent event) {
        ejecutarSinPropagar("nuevo comentario en solicitud " + event.solicitudId(),
                () -> notificacionService.notificarComentario(event));
    }

    private void ejecutarSinPropagar(String contexto, Runnable accion) {
        try {
            accion.run();
        } catch (Exception ex) {
            log.error("Error al crear notificación para {}", contexto, ex);
        }
    }
}
