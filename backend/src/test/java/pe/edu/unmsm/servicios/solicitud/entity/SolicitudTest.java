package pe.edu.unmsm.servicios.solicitud.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import pe.edu.unmsm.servicios.catalogo.entity.Estado;
import pe.edu.unmsm.servicios.catalogo.entity.TipoSolicitud;

class SolicitudTest {

    private static final LocalDateTime REGISTRO = LocalDateTime.of(2026, 10, 17, 9, 30);

    private static Estado estado(boolean esFinal) {
        Estado e = mock(Estado.class);
        when(e.isEsFinal()).thenReturn(esFinal);
        return e;
    }

    private static Solicitud solicitud(int slaHoras, Estado estado) {
        TipoSolicitud tipo = mock(TipoSolicitud.class);
        when(tipo.getSlaHoras()).thenReturn(slaHoras);
        return new Solicitud("SOL-2026-0001", null, tipo, null, estado, "desc", "aula", REGISTRO);
    }

    @Test
    void rn02DeadlineAddsTheSlaInCalendarHours() {
        assertThat(solicitud(24, estado(false)).getFechaLimite()).isEqualTo(LocalDateTime.of(2026, 10, 18, 9, 30));
        // Friday + 72 h lands on Monday: weekends are not skipped.
        assertThat(Solicitud.calcularFechaLimite(LocalDateTime.of(2026, 10, 16, 18, 0), 72))
                .isEqualTo(LocalDateTime.of(2026, 10, 19, 18, 0));
    }

    @Test
    void rn03OverdueOnlyWhenDeadlinePassedAndNotFinal() {
        LocalDateTime despues = REGISTRO.plusHours(25);

        assertThat(solicitud(24, estado(false)).estaVencida(despues)).isTrue();
        assertThat(solicitud(24, estado(false)).estaVencida(REGISTRO.plusHours(23))).isFalse();
        assertThat(solicitud(24, estado(true)).estaVencida(despues)).isFalse();
    }

    @Test
    void rn15ReachingAFinalStateSetsTheClosingDate() {
        Solicitud s = solicitud(24, estado(false));
        LocalDateTime cierre = REGISTRO.plusHours(5);

        s.cambiarEstado(estado(false), cierre);
        assertThat(s.getFechaCierre()).isNull();

        s.cambiarEstado(estado(true), cierre);
        assertThat(s.getFechaCierre()).isEqualTo(cierre);
    }
}
