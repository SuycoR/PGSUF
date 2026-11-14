package pe.edu.unmsm.servicios.notificacion.listener;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;

import pe.edu.unmsm.servicios.comentario.event.ComentarioCreadoEvent;
import pe.edu.unmsm.servicios.notificacion.service.NotificacionService;
import pe.edu.unmsm.servicios.solicitud.event.EstadoCambiadoEvent;
import pe.edu.unmsm.servicios.solicitud.event.SolicitudAsignadaEvent;

class NotificacionListenerTest {

    private final NotificacionService notificacionService = mock(NotificacionService.class);
    private final NotificacionListener listener = new NotificacionListener(notificacionService);

    @Test
    void failureCreatingNotificationDoesNotPropagate() {
        doThrow(new RuntimeException("DB caída")).when(notificacionService).notificarAsignacion(any());
        doThrow(new RuntimeException("DB caída")).when(notificacionService).notificarCambioEstado(any());
        doThrow(new RuntimeException("DB caída")).when(notificacionService).notificarComentario(any());

        assertThatCode(() -> listener.onSolicitudAsignada(new SolicitudAsignadaEvent(1L, 2L, 3L)))
                .doesNotThrowAnyException();
        assertThatCode(() -> listener.onEstadoCambiado(new EstadoCambiadoEvent(1L, "Asignada", "En atención", 2L)))
                .doesNotThrowAnyException();
        assertThatCode(() -> listener.onComentarioCreado(new ComentarioCreadoEvent(1L, 5L, 2L)))
                .doesNotThrowAnyException();

        verify(notificacionService).notificarAsignacion(any());
        verify(notificacionService).notificarCambioEstado(any());
        verify(notificacionService).notificarComentario(any());
    }
}
