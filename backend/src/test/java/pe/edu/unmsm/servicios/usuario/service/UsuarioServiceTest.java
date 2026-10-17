package pe.edu.unmsm.servicios.usuario.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import pe.edu.unmsm.servicios.catalogo.entity.Area;
import pe.edu.unmsm.servicios.catalogo.entity.Rol;
import pe.edu.unmsm.servicios.catalogo.repository.AreaRepository;
import pe.edu.unmsm.servicios.catalogo.repository.RolRepository;
import pe.edu.unmsm.servicios.common.BusinessException;
import pe.edu.unmsm.servicios.usuario.entity.Usuario;
import pe.edu.unmsm.servicios.usuario.repository.UsuarioRepository;

class UsuarioServiceTest {

    private UsuarioRepository usuarios;
    private RolRepository roles;
    private AreaRepository areas;
    private PasswordEncoder encoder;
    private UsuarioService servicio;

    @BeforeEach
    void setUp() {
        usuarios = mock(UsuarioRepository.class);
        roles = mock(RolRepository.class);
        areas = mock(AreaRepository.class);
        encoder = mock(PasswordEncoder.class);
        servicio = new UsuarioService(usuarios, roles, areas, encoder);
        when(usuarios.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));
        when(encoder.encode(any())).thenReturn("hash");
    }

    private Rol rol(String nombre, boolean requiereArea) {
        Rol rol = mock(Rol.class);
        when(rol.getNombre()).thenReturn(nombre);
        when(rol.requiereArea()).thenReturn(requiereArea);
        when(roles.findByNombre(nombre)).thenReturn(Optional.of(rol));
        return rol;
    }

    private Area area(int id, boolean activa) {
        Area area = mock(Area.class);
        when(area.isActivo()).thenReturn(activa);
        when(areas.findById(id)).thenReturn(Optional.of(area));
        return area;
    }

    private static void assertCodigo(Runnable accion, String codigo) {
        assertThatThrownBy(accion::run)
                .isInstanceOf(BusinessException.class)
                .extracting("codigo").isEqualTo(codigo);
    }

    @Test
    void storesTheEmailInLowercaseAndThePasswordHashed() {
        rol("SOLICITANTE", false);

        Usuario creado = servicio.crear(" Ana ", "  Ana@UNMSM.edu.pe ", "Secreto123", "SOLICITANTE", null);

        assertThat(creado.getCorreo()).isEqualTo("ana@unmsm.edu.pe");
        assertThat(creado.getNombre()).isEqualTo("Ana");
        assertThat(creado.getPasswordHash()).isEqualTo("hash");
    }

    @Test
    void rejectsADuplicateEmailIgnoringCase() {
        when(usuarios.existsByCorreo("ana@unmsm.edu.pe")).thenReturn(true);

        assertCodigo(() -> servicio.crear("Ana", "ANA@unmsm.edu.pe", "Secreto123", "SOLICITANTE", null), "CORREO_DUPLICADO");
        verify(usuarios, never()).save(any());
    }

    @Test
    void rn10TechnicianWithoutAreaIsRejected() {
        rol("TECNICO", true);

        assertCodigo(() -> servicio.crear("Tec", "tec@unmsm.edu.pe", "Secreto123", "TECNICO", null), "AREA_OBLIGATORIA");
    }

    @Test
    void rn10SupervisorWithAreaIsRejected() {
        rol("SUPERVISOR", false);

        assertCodigo(() -> servicio.crear("Sup", "sup@unmsm.edu.pe", "Secreto123", "SUPERVISOR", 1), "AREA_NO_PERMITIDA");
    }

    @Test
    void rn10TechnicianWithAnInactiveAreaIsRejected() {
        rol("TECNICO", true);
        area(1, false);

        assertCodigo(() -> servicio.crear("Tec", "tec@unmsm.edu.pe", "Secreto123", "TECNICO", 1), "AREA_INVALIDA");
    }

    @Test
    void rn10TechnicianWithAnActiveAreaIsCreated() {
        rol("TECNICO", true);
        Area soporte = area(1, true);

        Usuario creado = servicio.crear("Tec", "tec@unmsm.edu.pe", "Secreto123", "TECNICO", 1);

        assertThat(creado.getArea()).isSameAs(soporte);
    }

    @Test
    void unknownRoleIsRejected() {
        when(roles.findByNombre("JEFE")).thenReturn(Optional.empty());

        assertCodigo(() -> servicio.crear("X", "x@unmsm.edu.pe", "Secreto123", "JEFE", null), "ROL_INVALIDO");
    }
}
