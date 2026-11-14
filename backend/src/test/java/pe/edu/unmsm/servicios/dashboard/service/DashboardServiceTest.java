package pe.edu.unmsm.servicios.dashboard.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import pe.edu.unmsm.servicios.catalogo.entity.Categoria;
import pe.edu.unmsm.servicios.catalogo.entity.Estado;
import pe.edu.unmsm.servicios.catalogo.entity.Prioridad;
import pe.edu.unmsm.servicios.catalogo.entity.TipoSolicitud;
import pe.edu.unmsm.servicios.common.BusinessException;
import pe.edu.unmsm.servicios.dashboard.dto.DashboardDtos.DashboardResponse;
import pe.edu.unmsm.servicios.solicitud.entity.Solicitud;
import pe.edu.unmsm.servicios.solicitud.repository.SolicitudRepository;
import pe.edu.unmsm.servicios.usuario.entity.Usuario;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DashboardServiceTest {

    @Mock
    private SolicitudRepository solicitudes;

    private final Clock clock = Clock.fixed(Instant.parse("2026-10-25T12:00:00Z"), ZoneId.of("America/Lima"));
    private DashboardService service;

    private Categoria soporte;
    private Categoria rectificaciones;
    private Prioridad alta;
    private Prioridad media;
    private Estado enAtencion; // no final
    private Estado resuelta;   // final
    private Usuario tecnico1;

    @BeforeEach
    void setUp() {
        service = new DashboardService(solicitudes, clock);

        soporte = mock(Categoria.class);
        when(soporte.getId()).thenReturn(1);
        when(soporte.getNombre()).thenReturn("Soporte técnico");

        rectificaciones = mock(Categoria.class);
        when(rectificaciones.getId()).thenReturn(2);
        when(rectificaciones.getNombre()).thenReturn("Rectificaciones");

        alta = mock(Prioridad.class);
        when(alta.getId()).thenReturn(1);
        when(alta.getNombre()).thenReturn("Alta");

        media = mock(Prioridad.class);
        when(media.getId()).thenReturn(2);
        when(media.getNombre()).thenReturn("Media");

        enAtencion = mock(Estado.class);
        when(enAtencion.getId()).thenReturn(3);
        when(enAtencion.getNombre()).thenReturn(Estado.EN_ATENCION);
        when(enAtencion.isEsFinal()).thenReturn(false);

        resuelta = mock(Estado.class);
        when(resuelta.getId()).thenReturn(4);
        when(resuelta.getNombre()).thenReturn(Estado.RESUELTA);
        when(resuelta.isEsFinal()).thenReturn(true);

        tecnico1 = mock(Usuario.class);
        when(tecnico1.getId()).thenReturn(100L);
        when(tecnico1.getNombre()).thenReturn("Técnico Juan");
    }

    @Test
    @DisplayName("HU-11 RN-03: calcula correctamente totales, pendientes, atendidas, vencidas, tiempo promedio y %")
    void calculaIndicadoresCorrectamente() {
        TipoSolicitud tipoSoporte = mock(TipoSolicitud.class);
        when(tipoSoporte.getCategoria()).thenReturn(soporte);

        TipoSolicitud tipoRect = mock(TipoSolicitud.class);
        when(tipoRect.getCategoria()).thenReturn(rectificaciones);

        // Solicitud 1: Atendida y cerrada en 4 horas
        Solicitud s1 = mock(Solicitud.class);
        when(s1.getTipoSolicitud()).thenReturn(tipoSoporte);
        when(s1.getPrioridad()).thenReturn(alta);
        when(s1.getEstado()).thenReturn(resuelta);
        when(s1.getResponsable()).thenReturn(tecnico1);
        when(s1.getFechaRegistro()).thenReturn(LocalDateTime.of(2026, 10, 20, 8, 0));
        when(s1.getFechaCierre()).thenReturn(LocalDateTime.of(2026, 10, 20, 12, 0)); // 4 horas

        // Solicitud 2: Pendiente y VENCIDA (fechaLimite 2026-10-24 < ahora 2026-10-25)
        Solicitud s2 = mock(Solicitud.class);
        when(s2.getTipoSolicitud()).thenReturn(tipoRect);
        when(s2.getPrioridad()).thenReturn(media);
        when(s2.getEstado()).thenReturn(enAtencion);
        when(s2.getResponsable()).thenReturn(null); // Sin asignar
        when(s2.getFechaLimite()).thenReturn(LocalDateTime.of(2026, 10, 24, 10, 0));

        when(solicitudes.buscarParaDashboard(any(), any())).thenReturn(List.of(s1, s2));

        DashboardResponse resp = service.obtenerIndicadores(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31));

        assertThat(resp.totalRegistradas()).isEqualTo(2);
        assertThat(resp.totalAtendidas()).isEqualTo(1);
        assertThat(resp.totalPendientes()).isEqualTo(1);
        assertThat(resp.totalVencidas()).isEqualTo(1); // s2 está vencida
        assertThat(resp.porcentajeResueltas()).isEqualTo(50.0);
        assertThat(resp.tiempoPromedioAtencionHoras()).isEqualTo(4.0);

        // Agrupaciones
        assertThat(resp.porCategoria()).hasSize(2);
        assertThat(resp.porPrioridad()).hasSize(2);
        assertThat(resp.porResponsable()).hasSize(2);
        assertThat(resp.porResponsable()).anyMatch(r -> "Sin asignar".equals(r.nombre()) && r.cantidad() == 1);
        assertThat(resp.porResponsable()).anyMatch(r -> "Técnico Juan".equals(r.nombre()) && r.cantidad() == 1);
    }

    @Test
    @DisplayName("HU-11 Escenario 2: devuelve ceros cuando no hay solicitudes en el período")
    void devuelveCerosSiNoHayDatos() {
        when(solicitudes.buscarParaDashboard(any(), any())).thenReturn(List.of());

        DashboardResponse resp = service.obtenerIndicadores(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31));

        assertThat(resp.totalRegistradas()).isZero();
        assertThat(resp.totalPendientes()).isZero();
        assertThat(resp.totalAtendidas()).isZero();
        assertThat(resp.totalVencidas()).isZero();
        assertThat(resp.porcentajeResueltas()).isEqualTo(0.0);
        assertThat(resp.tiempoPromedioAtencionHoras()).isEqualTo(0.0);
        assertThat(resp.porCategoria()).isEmpty();
    }

    @Test
    @DisplayName("Valida que fecha desde no sea posterior a hasta")
    void validaRangoFechas() {
        LocalDate desde = LocalDate.of(2026, 10, 25);
        LocalDate hasta = LocalDate.of(2026, 10, 20);

        assertThatThrownBy(() -> service.obtenerIndicadores(desde, hasta))
                .isInstanceOf(BusinessException.class)
                .matches(e -> "RANGO_INVALIDO".equals(((BusinessException) e).getCodigo()));
    }
}
