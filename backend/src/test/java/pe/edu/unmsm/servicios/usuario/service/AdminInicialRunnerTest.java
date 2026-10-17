package pe.edu.unmsm.servicios.usuario.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import pe.edu.unmsm.servicios.config.AppProperties;
import pe.edu.unmsm.servicios.usuario.repository.UsuarioRepository;

class AdminInicialRunnerTest {

    private final UsuarioRepository usuarios = mock(UsuarioRepository.class);
    private final UsuarioService usuarioService = mock(UsuarioService.class);

    private AdminInicialRunner runner(String email, String password, String... perfiles) {
        AppProperties props = new AppProperties(new AppProperties.Jwt("x", 60), new AppProperties.Cors(List.of()),
                "./uploads", new AppProperties.Admin(email, password));
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles(perfiles);
        return new AdminInicialRunner(props, usuarios, usuarioService, env);
    }

    @Test
    void createsTheAdministratorWhenNoneExists() {
        runner("admin@unmsm.edu.pe", "admin", "dev").run(null);

        verify(usuarioService).crear("Administrador", "admin@unmsm.edu.pe", "admin", "ADMIN", null);
    }

    @Test
    void doesNothingWhenAnAdministratorAlreadyExists() {
        when(usuarios.existsByRolNombre("ADMIN")).thenReturn(true);

        runner("admin@unmsm.edu.pe", "admin", "dev").run(null);

        verify(usuarioService, never()).crear(any(), any(), any(), any(), any());
    }

    @Test
    void doesNothingWhenTheVariablesAreMissing() {
        runner("", "", "dev").run(null);

        verify(usuarioService, never()).crear(any(), any(), any(), any(), any());
    }

    @Test
    void refusesAWeakPasswordInProduction() {
        assertThatThrownBy(() -> runner("admin@unmsm.edu.pe", "admin", "prod").run(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("too weak");
        verify(usuarioService, never()).crear(any(), any(), any(), any(), any());
    }
}
