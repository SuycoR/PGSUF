package pe.edu.unmsm.servicios.auth.jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.Test;

import pe.edu.unmsm.servicios.catalogo.entity.Rol;
import pe.edu.unmsm.servicios.config.AppProperties;
import pe.edu.unmsm.servicios.usuario.entity.Usuario;

class JwtServiceTest {

    private static final String SECRETO = "dGVzdC1vbmx5LXNlY3JldC1rZXktZm9yLWludGVncmF0aW9uLXRlc3RzLTEyMzQ1Njc4OTA=";
    private static final String OTRO_SECRETO = "b3RyYS1jbGF2ZS1kaXN0aW50YS1wYXJhLXByb2Jhci1maXJtYXMtaW52YWxpZGFzLTEyMzQ=";
    private static final Instant AHORA = Instant.parse("2026-10-17T14:00:00Z");

    private static JwtService servicio(String secreto, Instant instante) {
        AppProperties props = new AppProperties(new AppProperties.Jwt(secreto, 60),
                new AppProperties.Cors(List.of()), "./uploads", new AppProperties.Admin("", ""));
        return new JwtService(props, Clock.fixed(instante, ZoneOffset.UTC));
    }

    private static Usuario usuario() {
        Rol rol = mock(Rol.class);
        when(rol.getNombre()).thenReturn("SUPERVISOR");
        Usuario usuario = mock(Usuario.class);
        when(usuario.getId()).thenReturn(7L);
        when(usuario.getCorreo()).thenReturn("sup@unmsm.edu.pe");
        when(usuario.getRol()).thenReturn(rol);
        return usuario;
    }

    @Test
    void aValidTokenCarriesIdEmailAndRole() {
        JwtService jwt = servicio(SECRETO, AHORA);

        UsuarioAutenticado leido = jwt.validar(jwt.generar(usuario())).orElseThrow();

        assertThat(leido).isEqualTo(new UsuarioAutenticado(7L, "sup@unmsm.edu.pe", "SUPERVISOR"));
    }

    @Test
    void anExpiredTokenIsRejected() {
        String token = servicio(SECRETO, AHORA).generar(usuario());

        JwtService dosHorasDespues = servicio(SECRETO, AHORA.plusSeconds(2 * 3600));

        assertThat(dosHorasDespues.validar(token)).isEmpty();
    }

    @Test
    void aTokenSignedWithAnotherKeyIsRejected() {
        String token = servicio(OTRO_SECRETO, AHORA).generar(usuario());

        assertThat(servicio(SECRETO, AHORA).validar(token)).isEmpty();
    }

    @Test
    void garbageIsRejected() {
        assertThat(servicio(SECRETO, AHORA).validar("no-es-un-token")).isEmpty();
    }

    @Test
    void roleConstantsMatchTheSeededRoles() {
        assertThat(List.of(Rol.SOLICITANTE, Rol.TECNICO, Rol.PERSONAL_AREA, Rol.SUPERVISOR, Rol.ADMIN))
                .containsExactly("SOLICITANTE", "TECNICO", "PERSONAL_AREA", "SUPERVISOR", "ADMIN");
    }
}
