package pe.edu.unmsm.servicios.solicitud.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;

import pe.edu.unmsm.servicios.catalogo.entity.Area;
import pe.edu.unmsm.servicios.catalogo.entity.Categoria;
import pe.edu.unmsm.servicios.catalogo.entity.Estado;
import pe.edu.unmsm.servicios.catalogo.entity.EstadoTransicion;
import pe.edu.unmsm.servicios.catalogo.entity.Prioridad;
import pe.edu.unmsm.servicios.catalogo.entity.Rol;
import pe.edu.unmsm.servicios.catalogo.entity.TipoSolicitud;
import pe.edu.unmsm.servicios.catalogo.repository.EstadoRepository;
import pe.edu.unmsm.servicios.catalogo.repository.PrioridadRepository;
import pe.edu.unmsm.servicios.catalogo.repository.TipoSolicitudRepository;
import pe.edu.unmsm.servicios.common.BusinessException;
import pe.edu.unmsm.servicios.solicitud.dto.SolicitudDtos.CambiarEstadoRequest;
import pe.edu.unmsm.servicios.solicitud.dto.SolicitudDtos.EvidenciaResponse;
import pe.edu.unmsm.servicios.solicitud.dto.SolicitudDtos.SolicitudDetalle;
import pe.edu.unmsm.servicios.solicitud.entity.Evidencia;
import pe.edu.unmsm.servicios.solicitud.entity.HistorialEstado;
import pe.edu.unmsm.servicios.solicitud.entity.Solicitud;
import pe.edu.unmsm.servicios.solicitud.event.EstadoCambiadoEvent;
import pe.edu.unmsm.servicios.solicitud.event.SolicitudAsignadaEvent;
import pe.edu.unmsm.servicios.solicitud.repository.EvidenciaRepository;
import pe.edu.unmsm.servicios.solicitud.repository.HistorialEstadoRepository;
import pe.edu.unmsm.servicios.solicitud.repository.SolicitudRepository;
import pe.edu.unmsm.servicios.solicitud.service.AlmacenamientoArchivos.ArchivoGuardado;
import pe.edu.unmsm.servicios.solicitud.service.AlmacenamientoArchivos.ArchivoValidado;
import pe.edu.unmsm.servicios.usuario.entity.Usuario;
import pe.edu.unmsm.servicios.usuario.repository.UsuarioRepository;

class SolicitudServiceGestionTest {

    private static final ZoneId ZONA = ZoneId.of("America/Lima");
    private static final Instant INSTANT = Instant.parse("2026-10-20T15:00:00Z");

    private final SolicitudRepository solicitudes = mock(SolicitudRepository.class);
    private final HistorialEstadoRepository historial = mock(HistorialEstadoRepository.class);
    private final EvidenciaRepository evidencias = mock(EvidenciaRepository.class);
    private final TipoSolicitudRepository tipos = mock(TipoSolicitudRepository.class);
    private final PrioridadRepository prioridades = mock(PrioridadRepository.class);
    private final EstadoRepository estados = mock(EstadoRepository.class);
    private final UsuarioRepository usuarios = mock(UsuarioRepository.class);
    private final SecuenciaCodigoService secuencia = mock(SecuenciaCodigoService.class);
    private final AlmacenamientoArchivos almacenamiento = mock(AlmacenamientoArchivos.class);
    private final VisibilidadSolicitudes visibilidad = new VisibilidadSolicitudes();
    private final TransicionService transiciones = mock(TransicionService.class);
    private final ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
    private final Clock clock = Clock.fixed(INSTANT, ZONA);

    private final SolicitudService service = new SolicitudService(
            solicitudes, historial, evidencias, tipos, prioridades, estados, usuarios,
            secuencia, almacenamiento, visibilidad, transiciones, events, clock);

    private static Area area(int id, String nombre) {
        Area a = mock(Area.class);
        when(a.getId()).thenReturn(id);
        when(a.getNombre()).thenReturn(nombre);
        return a;
    }

    private static Usuario usuario(long id, String nombre, String rolNombre, Area area, boolean activo) {
        Rol r = mock(Rol.class);
        when(r.getNombre()).thenReturn(rolNombre);
        Usuario u = mock(Usuario.class);
        when(u.getId()).thenReturn(id);
        when(u.getNombre()).thenReturn(nombre);
        when(u.getRol()).thenReturn(r);
        when(u.getArea()).thenReturn(area);
        when(u.isActivo()).thenReturn(activo);
        return u;
    }

    private static Estado estado(int id, String nombre, boolean esFinal) {
        Estado e = mock(Estado.class);
        when(e.getId()).thenReturn(id);
        when(e.getNombre()).thenReturn(nombre);
        when(e.isEsFinal()).thenReturn(esFinal);
        return e;
    }

    private static Prioridad prioridad(int id, String nombre) {
        Prioridad p = mock(Prioridad.class);
        when(p.getId()).thenReturn(id);
        when(p.getNombre()).thenReturn(nombre);
        return p;
    }

    private static Solicitud solicitudReal(long id, Usuario solicitante, boolean tramite, Area area, Estado estado) {
        Categoria cat = mock(Categoria.class);
        when(cat.getNombre()).thenReturn(tramite ? "Rectificaciones" : Categoria.SOPORTE_TECNICO);
        when(cat.esTramite()).thenReturn(tramite);
        TipoSolicitud tipo = mock(TipoSolicitud.class);
        when(tipo.getCategoria()).thenReturn(cat);
        when(tipo.getArea()).thenReturn(area);
        when(tipo.getNombre()).thenReturn(tramite ? "Rectificación de nota" : "Proyector");
        when(tipo.getSlaHoras()).thenReturn(24);
        Prioridad prio = prioridad(2, "Media");
        LocalDateTime reg = LocalDateTime.ofInstant(INSTANT, ZONA).minusHours(2);
        Solicitud s = new Solicitud("SOL-2026-0001", solicitante, tipo, prio, estado, "Desc", "Aula", reg);
        org.springframework.test.util.ReflectionTestUtils.setField(s, "id", id);
        return s;
    }

    @Test
    void bandejaAppliesRoleVisibilityFilters() {
        Area secArea = area(2, "Secretaría académica");
        Usuario sup = usuario(1L, "Sup", Rol.SUPERVISOR, null, true);
        Usuario pa = usuario(2L, "Sec", Rol.PERSONAL_AREA, secArea, true);
        when(usuarios.findById(1L)).thenReturn(Optional.of(sup));
        when(usuarios.findById(2L)).thenReturn(Optional.of(pa));
        PageRequest pageable = PageRequest.of(0, 20);
        when(solicitudes.buscarBandeja(any(Boolean.class), any(), any(), any(), any(), any(), any(), any(), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of()));

        service.bandeja(1L, 1, null, 2, true, pageable);
        verify(solicitudes).buscarBandeja(eq(true), eq(Categoria.SOPORTE_TECNICO), eq(null), eq(1), eq(null), eq(2),
                eq(true), any(), eq(pageable));

        service.bandeja(2L, null, null, null, null, pageable);
        verify(solicitudes).buscarBandeja(eq(false), eq(Categoria.SOPORTE_TECNICO), eq(2), eq(null), eq(null), eq(null),
                eq(null), any(), eq(pageable));
    }

    @Test
    void asignarSetsResponsableTransitionsRecordsHistoryAndPublishesEvent() {
        Area soporte = area(1, "Soporte técnico");
        Usuario sup = usuario(10L, "Sup", Rol.SUPERVISOR, null, true);
        Usuario sol = usuario(11L, "Ana", Rol.SOLICITANTE, null, true);
        Usuario tec = usuario(12L, "Luis Tec", Rol.TECNICO, soporte, true);
        Estado enEval = estado(2, Estado.EN_EVALUACION, false);
        Estado asignada = estado(3, Estado.ASIGNADA, false);
        Solicitud s = solicitudReal(100L, sol, false, soporte, enEval);
        EstadoTransicion t = mock(EstadoTransicion.class);
        when(t.getEstadoDestino()).thenReturn(asignada);

        when(usuarios.findById(10L)).thenReturn(Optional.of(sup));
        when(usuarios.findById(12L)).thenReturn(Optional.of(tec));
        when(solicitudes.buscarDetalle(100L)).thenReturn(Optional.of(s));
        when(estados.findByNombre(Estado.ASIGNADA)).thenReturn(Optional.of(asignada));
        when(transiciones.validar(enEval, 3, Rol.SUPERVISOR, EstadoTransicion.AMBITO_SOLICITUD, null)).thenReturn(t);

        SolicitudDetalle detalle = service.asignar(10L, 100L, 12L);

        assertThat(detalle.estado()).isEqualTo("Asignada");
        assertThat(detalle.responsable().id()).isEqualTo(12L);
        verify(historial).save(any(HistorialEstado.class));
        ArgumentCaptor<SolicitudAsignadaEvent> eventCaptor = ArgumentCaptor.forClass(SolicitudAsignadaEvent.class);
        verify(events).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue()).isEqualTo(new SolicitudAsignadaEvent(100L, 12L, 10L));
    }

    @Test
    void asignarRejectsTechnicianFromAnotherAreaWith422() {
        Area soporte = area(1, "Soporte técnico");
        Area otra = area(2, "Secretaría académica");
        Usuario sup = usuario(10L, "Sup", Rol.SUPERVISOR, null, true);
        Usuario sol = usuario(11L, "Ana", Rol.SOLICITANTE, null, true);
        Usuario tecOtra = usuario(12L, "Otro Tec", Rol.TECNICO, otra, true);
        Estado enEval = estado(2, Estado.EN_EVALUACION, false);
        Estado asignada = estado(3, Estado.ASIGNADA, false);
        Solicitud s = solicitudReal(100L, sol, false, soporte, enEval);

        when(usuarios.findById(10L)).thenReturn(Optional.of(sup));
        when(usuarios.findById(12L)).thenReturn(Optional.of(tecOtra));
        when(solicitudes.buscarDetalle(100L)).thenReturn(Optional.of(s));
        when(estados.findByNombre(Estado.ASIGNADA)).thenReturn(Optional.of(asignada));

        assertThatThrownBy(() -> service.asignar(10L, 100L, 12L))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
                    assertThat(be.getCodigo()).isEqualTo("TECNICO_AREA_INVALIDA");
                });
    }

    @Test
    void cambiarEstadoRejectsTechnicianNotAssignedWith403() {
        Area soporte = area(1, "Soporte técnico");
        Usuario tecAsignado = usuario(12L, "Luis", Rol.TECNICO, soporte, true);
        Usuario otroTec = usuario(13L, "Pedro", Rol.TECNICO, soporte, true);
        Usuario sol = usuario(11L, "Ana", Rol.SOLICITANTE, null, true);
        Estado asignada = estado(3, Estado.ASIGNADA, false);
        Solicitud s = solicitudReal(100L, sol, false, soporte, asignada);
        s.asignarResponsable(tecAsignado);

        when(usuarios.findById(13L)).thenReturn(Optional.of(otroTec));
        when(solicitudes.buscarDetalle(100L)).thenReturn(Optional.of(s));

        assertThatThrownBy(() -> service.cambiarEstado(13L, 100L, new CambiarEstadoRequest(4, null)))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getStatus()).isEqualTo(HttpStatus.FORBIDDEN));
    }

    @Test
    void cambiarEstadoByPersonalAreaTakesOwnershipAndSetsFinalClosingDate() {
        Area secArea = area(2, "Secretaría académica");
        Usuario pa1 = usuario(20L, "PA1", Rol.PERSONAL_AREA, secArea, true);
        Usuario pa2 = usuario(21L, "PA2", Rol.PERSONAL_AREA, secArea, true);
        Usuario sol = usuario(11L, "Ana", Rol.SOLICITANTE, null, true);
        Estado enEval = estado(2, Estado.EN_EVALUACION, false);
        Estado enAtencion = estado(4, Estado.EN_ATENCION, false);
        Estado resuelta = estado(5, Estado.RESUELTA, false);
        Estado cerrada = estado(6, Estado.CERRADA, true);
        Solicitud s = solicitudReal(101L, sol, true, secArea, enEval);

        EstadoTransicion tTomar = mock(EstadoTransicion.class);
        when(tTomar.getEstadoDestino()).thenReturn(enAtencion);
        EstadoTransicion tResolver = mock(EstadoTransicion.class);
        when(tResolver.getEstadoDestino()).thenReturn(resuelta);
        EstadoTransicion tCerrar = mock(EstadoTransicion.class);
        when(tCerrar.getEstadoDestino()).thenReturn(cerrada);

        when(usuarios.findById(20L)).thenReturn(Optional.of(pa1));
        when(usuarios.findById(21L)).thenReturn(Optional.of(pa2));
        when(usuarios.findById(11L)).thenReturn(Optional.of(sol));
        when(solicitudes.buscarDetalle(101L)).thenReturn(Optional.of(s));
        when(transiciones.validar(enEval, 4, Rol.PERSONAL_AREA, EstadoTransicion.AMBITO_SOLICITUD, null))
                .thenReturn(tTomar);
        when(transiciones.validar(enAtencion, 5, Rol.PERSONAL_AREA, EstadoTransicion.AMBITO_SOLICITUD, "Nota actualizada"))
                .thenReturn(tResolver);
        when(transiciones.validar(resuelta, 6, Rol.SOLICITANTE, EstadoTransicion.AMBITO_SOLICITUD, null))
                .thenReturn(tCerrar);

        SolicitudDetalle trasTomar = service.cambiarEstado(20L, 101L, new CambiarEstadoRequest(4, null));
        assertThat(trasTomar.responsable().id()).isEqualTo(20L);

        // Another PA in the same area cannot resolve once taken (RN-06)
        assertThatThrownBy(() -> service.cambiarEstado(21L, 101L, new CambiarEstadoRequest(5, "Intruso")))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getStatus()).isEqualTo(HttpStatus.FORBIDDEN));

        SolicitudDetalle trasResolver = service.cambiarEstado(20L, 101L, new CambiarEstadoRequest(5, "Nota actualizada"));
        assertThat(trasResolver.respuesta()).isEqualTo("Nota actualizada");
        assertThat(trasResolver.fechaCierre()).isNull();

        SolicitudDetalle trasCerrar = service.cambiarEstado(11L, 101L, new CambiarEstadoRequest(6, null));
        assertThat(trasCerrar.estadoFinal()).isTrue();
        assertThat(trasCerrar.fechaCierre()).isNotNull();

        verify(events).publishEvent(new EstadoCambiadoEvent(101L, Estado.RESUELTA, Estado.CERRADA, 11L));
    }

    @Test
    void transicionesDisponiblesHidesActionsWhenAnotherPaTookTheRequest() {
        Area secArea = area(2, "Secretaría académica");
        Usuario pa1 = usuario(20L, "PA1", Rol.PERSONAL_AREA, secArea, true);
        Usuario pa2 = usuario(21L, "PA2", Rol.PERSONAL_AREA, secArea, true);
        Usuario sol = usuario(11L, "Ana", Rol.SOLICITANTE, null, true);
        Estado enAtencion = estado(4, Estado.EN_ATENCION, false);
        Solicitud s = solicitudReal(101L, sol, true, secArea, enAtencion);
        s.asignarResponsable(pa1);

        when(usuarios.findById(21L)).thenReturn(Optional.of(pa2));
        when(solicitudes.buscarDetalle(101L)).thenReturn(Optional.of(s));

        assertThat(service.transicionesDisponibles(21L, 101L)).isEmpty();
    }

    @Test
    void agregarEvidenciasValidatesAndSavesFiles() {
        Area soporte = area(1, "Soporte técnico");
        Usuario sol = usuario(11L, "Ana", Rol.SOLICITANTE, null, true);
        Estado reg = estado(1, Estado.REGISTRADA, false);
        Solicitud s = solicitudReal(100L, sol, false, soporte, reg);
        MockMultipartFile archivo = new MockMultipartFile("archivos", "foto.png", "image/png", new byte[] {1, 2, 3});
        ArchivoValidado validado = new ArchivoValidado(archivo, "foto.png", "image/png", "png");
        ArchivoGuardado guardado = new ArchivoGuardado("solicitudes/100/u.png", "foto.png", "image/png", 3);
        Evidencia entidad = new Evidencia(s, sol, "foto.png", "solicitudes/100/u.png", "image/png", 3,
                LocalDateTime.ofInstant(INSTANT, ZONA));

        when(usuarios.findById(11L)).thenReturn(Optional.of(sol));
        when(solicitudes.buscarDetalle(100L)).thenReturn(Optional.of(s));
        when(almacenamiento.validar(archivo)).thenReturn(validado);
        when(almacenamiento.guardar(100L, validado)).thenReturn(guardado);
        when(evidencias.save(any(Evidencia.class))).thenReturn(entidad);

        List<EvidenciaResponse> resultado = service.agregarEvidencias(11L, 100L, List.of(archivo));

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).nombreArchivo()).isEqualTo("foto.png");
    }
}
