package pe.edu.unmsm.servicios.notificacion.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class DestinatariosNotificacionTest {

    @Test
    void paraAsignacionNotificaTecnicoYSolicitanteSinDuplicados() {
        assertThat(DestinatariosNotificacion.paraAsignacion(10L, 20L))
                .containsExactlyInAnyOrder(20L, 10L);
        assertThat(DestinatariosNotificacion.paraAsignacion(10L, 10L))
                .containsExactly(10L);
        assertThat(DestinatariosNotificacion.paraAsignacion(10L, null))
                .containsExactly(10L);
    }

    @Test
    void paraCambioEstadoExcluyeAlActorYNotificaInvolucrados() {
        // Supervisor changes state: notify both requester and assigned responsible
        assertThat(DestinatariosNotificacion.paraCambioEstado(10L, 20L, 30L))
                .containsExactlyInAnyOrder(10L, 20L);

        // Technician (responsible) changes state: notify only requester
        assertThat(DestinatariosNotificacion.paraCambioEstado(10L, 20L, 20L))
                .containsExactly(10L);

        // Requester closes request: notify only responsible
        assertThat(DestinatariosNotificacion.paraCambioEstado(10L, 20L, 10L))
                .containsExactly(20L);

        // No responsible assigned yet and requester acts: nobody to notify
        assertThat(DestinatariosNotificacion.paraCambioEstado(10L, null, 10L))
                .isEmpty();
    }

    @Test
    void paraComentarioNuncaNotificaAlAutor() {
        // Requester comments: notify responsible only
        assertThat(DestinatariosNotificacion.paraComentario(10L, 20L, 10L))
                .containsExactly(20L);

        // Responsible comments: notify requester only
        assertThat(DestinatariosNotificacion.paraComentario(10L, 20L, 20L))
                .containsExactly(10L);

        // Supervisor or area staff comments: notify both requester and responsible
        assertThat(DestinatariosNotificacion.paraComentario(10L, 20L, 30L))
                .containsExactlyInAnyOrder(10L, 20L);

        // Requester comments before anyone is assigned: no notification
        assertThat(DestinatariosNotificacion.paraComentario(10L, null, 10L))
                .isEmpty();
    }
}
