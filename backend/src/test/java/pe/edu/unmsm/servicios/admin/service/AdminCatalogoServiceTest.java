package pe.edu.unmsm.servicios.admin.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import pe.edu.unmsm.servicios.ambiente.entity.Ambiente;
import pe.edu.unmsm.servicios.ambiente.entity.Pabellon;
import pe.edu.unmsm.servicios.ambiente.entity.TipoAmbiente;
import pe.edu.unmsm.servicios.ambiente.repository.AmbienteRepository;
import pe.edu.unmsm.servicios.catalogo.entity.Area;
import pe.edu.unmsm.servicios.catalogo.entity.Categoria;
import pe.edu.unmsm.servicios.catalogo.entity.Estado;
import pe.edu.unmsm.servicios.catalogo.entity.EstadoTransicion;
import pe.edu.unmsm.servicios.catalogo.entity.Prioridad;
import pe.edu.unmsm.servicios.catalogo.entity.Rol;
import pe.edu.unmsm.servicios.catalogo.entity.TipoSolicitud;
import pe.edu.unmsm.servicios.catalogo.repository.AreaRepository;
import pe.edu.unmsm.servicios.catalogo.repository.CategoriaRepository;
import pe.edu.unmsm.servicios.catalogo.repository.EstadoRepository;
import pe.edu.unmsm.servicios.catalogo.repository.EstadoTransicionRepository;
import pe.edu.unmsm.servicios.catalogo.repository.PrioridadRepository;
import pe.edu.unmsm.servicios.catalogo.repository.RolRepository;
import pe.edu.unmsm.servicios.catalogo.repository.TipoSolicitudRepository;
import pe.edu.unmsm.servicios.common.BusinessException;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AdminCatalogoServiceTest {

    private AreaRepository areaRepo;
    private CategoriaRepository categoriaRepo;
    private TipoSolicitudRepository tipoRepo;
    private PrioridadRepository prioridadRepo;
    private EstadoRepository estadoRepo;
    private EstadoTransicionRepository transicionRepo;
    private AmbienteRepository ambienteRepo;
    private RolRepository rolRepo;

    private AdminCatalogoService service;

    @BeforeEach
    void setUp() {
        areaRepo = mock(AreaRepository.class);
        categoriaRepo = mock(CategoriaRepository.class);
        tipoRepo = mock(TipoSolicitudRepository.class);
        prioridadRepo = mock(PrioridadRepository.class);
        estadoRepo = mock(EstadoRepository.class);
        transicionRepo = mock(EstadoTransicionRepository.class);
        ambienteRepo = mock(AmbienteRepository.class);
        rolRepo = mock(RolRepository.class);

        service = new AdminCatalogoService(
                areaRepo, categoriaRepo, tipoRepo, prioridadRepo,
                estadoRepo, transicionRepo, ambienteRepo, rolRepo
        );

        when(areaRepo.save(any())).thenAnswer(i -> i.getArgument(0));
        when(categoriaRepo.save(any())).thenAnswer(i -> i.getArgument(0));
        when(tipoRepo.save(any())).thenAnswer(i -> i.getArgument(0));
        when(prioridadRepo.save(any())).thenAnswer(i -> i.getArgument(0));
        when(estadoRepo.save(any())).thenAnswer(i -> i.getArgument(0));
        when(transicionRepo.save(any())).thenAnswer(i -> i.getArgument(0));
        when(ambienteRepo.save(any())).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    void crearArea_conNombreNuevo_guardaYRetornaArea() {
        when(areaRepo.existsByNombre("Finanzas")).thenReturn(false);

        Area guardada = service.crearArea("Finanzas");

        assertThat(guardada.getNombre()).isEqualTo("Finanzas");
        assertThat(guardada.isActivo()).isTrue();
        verify(areaRepo).save(any(Area.class));
    }

    @Test
    void crearArea_conNombreDuplicado_lanzaConflict() {
        when(areaRepo.existsByNombre("Soporte técnico")).thenReturn(true);

        assertThatThrownBy(() -> service.crearArea("Soporte técnico"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Ya existe");
    }

    @Test
    void cambiarActivoArea_actualizaEstado() {
        Area area = new Area("Área Test", true);
        when(areaRepo.findById(1)).thenReturn(Optional.of(area));

        Area resultado = service.cambiarActivoArea(1, false);

        assertThat(resultado.isActivo()).isFalse();
        verify(areaRepo).save(area);
    }

    @Test
    void crearTipoSolicitud_validaAreaYCategoria() {
        Categoria cat = new Categoria("Soporte", true);
        Area area = new Area("Sistemas", true);
        when(categoriaRepo.findById(10)).thenReturn(Optional.of(cat));
        when(areaRepo.findById(20)).thenReturn(Optional.of(area));
        when(tipoRepo.existsByCategoriaIdAndNombre(10, "Reparación")).thenReturn(false);

        TipoSolicitud tipo = service.crearTipoSolicitud(10, 20, "Reparación", false, 48);

        assertThat(tipo.getNombre()).isEqualTo("Reparación");
        assertThat(tipo.getSlaHoras()).isEqualTo(48);
        assertThat(tipo.getCategoria()).isEqualTo(cat);
        assertThat(tipo.getArea()).isEqualTo(area);
    }

    @Test
    void crearAmbiente_conNombreNuevo_guardaAmbiente() {
        when(ambienteRepo.existsByNombre("Laboratorio N-301")).thenReturn(false);

        Ambiente amb = service.crearAmbiente("Laboratorio N-301", TipoAmbiente.LABORATORIO, Pabellon.NUEVO, 3, 30);

        assertThat(amb.getNombre()).isEqualTo("Laboratorio N-301");
        assertThat(amb.getCapacidad()).isEqualTo(30);
        assertThat(amb.isActivo()).isTrue();
    }

    @Test
    void crearTransicion_validaEstadosYRol() {
        Estado e1 = new Estado("Registrada", "SOLICITUD", 1, false);
        Estado e2 = new Estado("En evaluación", "SOLICITUD", 2, false);
        Rol rol = mock(Rol.class);

        when(estadoRepo.findById(1)).thenReturn(Optional.of(e1));
        when(estadoRepo.findById(2)).thenReturn(Optional.of(e2));
        when(rolRepo.findById(5)).thenReturn(Optional.of(rol));
        when(transicionRepo.existsByEstadoOrigenIdAndEstadoDestinoIdAndRolIdAndAmbito(1, 2, 5, "SOLICITUD")).thenReturn(false);

        EstadoTransicion t = service.crearTransicion(1, 2, 5, "SOLICITUD", false);

        assertThat(t.getEstadoOrigen()).isEqualTo(e1);
        assertThat(t.getEstadoDestino()).isEqualTo(e2);
        assertThat(t.getRol()).isEqualTo(rol);
    }
}
