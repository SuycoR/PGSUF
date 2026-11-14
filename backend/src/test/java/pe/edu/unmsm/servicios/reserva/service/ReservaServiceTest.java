package pe.edu.unmsm.servicios.reserva.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import pe.edu.unmsm.servicios.ambiente.entity.Ambiente;
import pe.edu.unmsm.servicios.ambiente.entity.Pabellon;
import pe.edu.unmsm.servicios.ambiente.entity.TipoAmbiente;
import pe.edu.unmsm.servicios.ambiente.repository.AmbienteRepository;
import pe.edu.unmsm.servicios.catalogo.entity.Estado;
import pe.edu.unmsm.servicios.catalogo.repository.EstadoRepository;
import pe.edu.unmsm.servicios.common.BusinessException;
import pe.edu.unmsm.servicios.reserva.dto.ReservaDtos.CrearReservaRequest;
import pe.edu.unmsm.servicios.reserva.dto.ReservaDtos.ReservaResponse;
import pe.edu.unmsm.servicios.reserva.entity.Reserva;
import pe.edu.unmsm.servicios.reserva.event.ReservaRegistradaEvent;
import pe.edu.unmsm.servicios.reserva.event.ReservaResueltaEvent;
import pe.edu.unmsm.servicios.reserva.repository.ReservaRepository;
import pe.edu.unmsm.servicios.solicitud.service.SecuenciaCodigoService;
import pe.edu.unmsm.servicios.usuario.entity.Usuario;
import pe.edu.unmsm.servicios.usuario.repository.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ReservaServiceTest {

    @Mock
    private ReservaRepository reservas;

    @Mock
    private AmbienteRepository ambientes;

    @Mock
    private UsuarioRepository usuarios;

    @Mock
    private EstadoRepository estados;

    @Mock
    private SecuenciaCodigoService secuencia;

    @Mock
    private ApplicationEventPublisher events;

    private final Clock clock = Clock.fixed(Instant.parse("2026-10-20T10:00:00Z"), ZoneId.of("America/Lima"));
    private ReservaService service;

    private Ambiente auditorio;
    private Usuario solicitante;
    private Usuario supervisor;
    private Estado estadoRegistrada;
    private Estado estadoAprobada;
    private Estado estadoRechazada;

    @BeforeEach
    void setUp() {
        service = new ReservaService(reservas, ambientes, usuarios, estados, secuencia, events, clock);

        auditorio = new Ambiente("Auditorio Central", TipoAmbiente.AUDITORIO, Pabellon.CENTRAL, null, 300, true);
        ReflectionTestUtils.setField(auditorio, "id", 1);

        solicitante = mock(Usuario.class);
        when(solicitante.getId()).thenReturn(10L);
        when(solicitante.getNombre()).thenReturn("Ana Solicitante");

        supervisor = mock(Usuario.class);
        when(supervisor.getId()).thenReturn(20L);
        when(supervisor.getNombre()).thenReturn("Carlos Supervisor");

        estadoRegistrada = mock(Estado.class);
        when(estadoRegistrada.getId()).thenReturn(1);
        when(estadoRegistrada.getNombre()).thenReturn(Estado.REGISTRADA);

        estadoAprobada = mock(Estado.class);
        when(estadoAprobada.getId()).thenReturn(6);
        when(estadoAprobada.getNombre()).thenReturn(Estado.APROBADA);

        estadoRechazada = mock(Estado.class);
        when(estadoRechazada.getId()).thenReturn(7);
        when(estadoRechazada.getNombre()).thenReturn(Estado.RECHAZADA);
    }

    @Test
    @DisplayName("HU-13 RN-17: rechaza si la hora fin es igual o anterior a la hora inicio")
    void validaHoraFinPosteriorAHoraInicio() {
        CrearReservaRequest req = new CrearReservaRequest(
                1, LocalDate.of(2026, 10, 25),
                LocalTime.of(14, 0), LocalTime.of(12, 0),
                "Charla académica"
        );

        assertThatThrownBy(() -> service.crear(10L, req))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("La hora de fin debe ser posterior a la hora de inicio")
                .matches(e -> "HORARIO_INVALIDO".equals(((BusinessException) e).getCodigo()));

        verify(reservas, never()).save(any());
    }

    @Test
    @DisplayName("HU-13 RN-17: rechaza si la fecha y hora solicitada ya pasaron")
    void validaFechaHoraNoEnElPasado() {
        CrearReservaRequest req = new CrearReservaRequest(
                1, LocalDate.of(2026, 10, 19),
                LocalTime.of(10, 0), LocalTime.of(12, 0),
                "Charla académica"
        );

        assertThatThrownBy(() -> service.crear(10L, req))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("no pueden estar en el pasado")
                .matches(e -> "FECHA_PASADA".equals(((BusinessException) e).getCodigo()));
    }

    @Test
    @DisplayName("HU-13 RN-17: rechaza si el ambiente está inactivo")
    void validaAmbienteActivo() {
        Ambiente inactivo = new Ambiente("Lab 1", TipoAmbiente.LABORATORIO, Pabellon.ANTIGUO, 1, 20, false);
        ReflectionTestUtils.setField(inactivo, "id", 2);
        when(ambientes.findByIdForUpdate(2)).thenReturn(Optional.of(inactivo));

        CrearReservaRequest req = new CrearReservaRequest(
                2, LocalDate.of(2026, 10, 25),
                LocalTime.of(10, 0), LocalTime.of(12, 0),
                "Práctica"
        );

        assertThatThrownBy(() -> service.crear(10L, req))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("no está activo")
                .matches(e -> "AMBIENTE_INACTIVO".equals(((BusinessException) e).getCodigo()));
    }

    @Test
    @DisplayName("HU-13 RN-04: detecta cruce de horario contra reservas aprobadas al registrar")
    void detectaCruceAlRegistrar() {
        when(ambientes.findByIdForUpdate(1)).thenReturn(Optional.of(auditorio));
        when(reservas.existeCruce(1, LocalDate.of(2026, 10, 25), LocalTime.of(10, 0), LocalTime.of(12, 0), null))
                .thenReturn(true);

        CrearReservaRequest req = new CrearReservaRequest(
                1, LocalDate.of(2026, 10, 25),
                LocalTime.of(10, 0), LocalTime.of(12, 0),
                "Sustentación"
        );

        assertThatThrownBy(() -> service.crear(10L, req))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("El ambiente ya está reservado en ese horario")
                .matches(e -> "CRUCE_HORARIO".equals(((BusinessException) e).getCodigo()));
    }

    @Test
    @DisplayName("HU-13 RN-01 RN-04: registra exitosamente con código único y emite notificación")
    void creaReservaCorrectamente() {
        when(ambientes.findByIdForUpdate(1)).thenReturn(Optional.of(auditorio));
        when(reservas.existeCruce(1, LocalDate.of(2026, 10, 25), LocalTime.of(10, 0), LocalTime.of(12, 0), null))
                .thenReturn(false);
        when(usuarios.findById(10L)).thenReturn(Optional.of(solicitante));
        when(estados.findByNombreAndAmbitoIn(eq(Estado.REGISTRADA), any())).thenReturn(Optional.of(estadoRegistrada));
        when(secuencia.siguiente("RES", 2026)).thenReturn("RES-2026-0001");
        when(reservas.save(any(Reserva.class))).thenAnswer(invocation -> {
            Reserva r = invocation.getArgument(0);
            ReflectionTestUtils.setField(r, "id", 100L);
            return r;
        });

        CrearReservaRequest req = new CrearReservaRequest(
                1, LocalDate.of(2026, 10, 25),
                LocalTime.of(10, 0), LocalTime.of(12, 0),
                "Conferencia"
        );

        ReservaResponse resp = service.crear(10L, req);

        assertThat(resp.id()).isEqualTo(100L);
        assertThat(resp.codigo()).isEqualTo("RES-2026-0001");
        assertThat(resp.estado().nombre()).isEqualTo(Estado.REGISTRADA);
        verify(events).publishEvent(any(ReservaRegistradaEvent.class));
    }

    @Test
    @DisplayName("HU-14 RN-04: revalida cruce al momento de aprobar y bloquea si ya existe una reserva aprobada")
    void detectaCruceAlAprobar() {
        Reserva pendiente = new Reserva(
                "RES-2026-0002", auditorio, solicitante, estadoRegistrada,
                LocalDate.of(2026, 10, 25), LocalTime.of(10, 0), LocalTime.of(12, 0),
                "Evento", LocalDateTime.now(clock)
        );
        ReflectionTestUtils.setField(pendiente, "id", 200L);

        when(reservas.findById(200L)).thenReturn(Optional.of(pendiente));
        when(ambientes.findByIdForUpdate(1)).thenReturn(Optional.of(auditorio));
        when(reservas.existeCruce(1, LocalDate.of(2026, 10, 25), LocalTime.of(10, 0), LocalTime.of(12, 0), 200L))
                .thenReturn(true);

        assertThatThrownBy(() -> service.aprobar(200L, 20L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("ya tiene una reserva aprobada en ese horario")
                .matches(e -> "CRUCE_HORARIO".equals(((BusinessException) e).getCodigo()));
    }

    @Test
    @DisplayName("HU-14: aprueba reserva exitosamente y fija aprobador y fechaResolucion")
    void apruebaReservaExitosamente() {
        Reserva pendiente = new Reserva(
                "RES-2026-0002", auditorio, solicitante, estadoRegistrada,
                LocalDate.of(2026, 10, 25), LocalTime.of(10, 0), LocalTime.of(12, 0),
                "Evento", LocalDateTime.now(clock)
        );
        ReflectionTestUtils.setField(pendiente, "id", 200L);

        when(reservas.findById(200L)).thenReturn(Optional.of(pendiente));
        when(ambientes.findByIdForUpdate(1)).thenReturn(Optional.of(auditorio));
        when(reservas.existeCruce(1, LocalDate.of(2026, 10, 25), LocalTime.of(10, 0), LocalTime.of(12, 0), 200L))
                .thenReturn(false);
        when(usuarios.findById(20L)).thenReturn(Optional.of(supervisor));
        when(estados.findByNombreAndAmbitoIn(eq(Estado.APROBADA), any())).thenReturn(Optional.of(estadoAprobada));

        ReservaResponse resp = service.aprobar(200L, 20L);

        assertThat(resp.estado().nombre()).isEqualTo(Estado.APROBADA);
        assertThat(resp.aprobador().nombre()).isEqualTo("Carlos Supervisor");
        assertThat(resp.fechaResolucion()).isNotNull();
        verify(events).publishEvent(any(ReservaResueltaEvent.class));
    }

    @Test
    @DisplayName("HU-14 RN-13: rechazo exige motivo obligatorio no vacío")
    void rechazoExigeMotivo() {
        assertThatThrownBy(() -> service.rechazar(200L, 20L, "   "))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("El motivo de rechazo es obligatorio")
                .matches(e -> "MOTIVO_REQUERIDO".equals(((BusinessException) e).getCodigo()));
    }

    @Test
    @DisplayName("HU-14: rechazo guarda motivo y actualiza estado a Rechazada")
    void rechazaReservaCorrectamente() {
        Reserva pendiente = new Reserva(
                "RES-2026-0002", auditorio, solicitante, estadoRegistrada,
                LocalDate.of(2026, 10, 25), LocalTime.of(10, 0), LocalTime.of(12, 0),
                "Evento", LocalDateTime.now(clock)
        );
        ReflectionTestUtils.setField(pendiente, "id", 200L);

        when(reservas.findById(200L)).thenReturn(Optional.of(pendiente));
        when(usuarios.findById(20L)).thenReturn(Optional.of(supervisor));
        when(estados.findByNombreAndAmbitoIn(eq(Estado.RECHAZADA), any())).thenReturn(Optional.of(estadoRechazada));

        ReservaResponse resp = service.rechazar(200L, 20L, "Feriado no laborable");

        assertThat(resp.estado().nombre()).isEqualTo(Estado.RECHAZADA);
        assertThat(resp.motivoRechazo()).isEqualTo("Feriado no laborable");
        verify(events).publishEvent(any(ReservaResueltaEvent.class));
    }
}
