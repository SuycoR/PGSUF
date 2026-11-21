package pe.edu.unmsm.servicios.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import pe.edu.unmsm.servicios.auth.controller.AuthController;
import pe.edu.unmsm.servicios.auth.dto.LoginRequest;
import pe.edu.unmsm.servicios.auth.dto.LoginResponse;
import pe.edu.unmsm.servicios.auth.jwt.JwtAuthenticationFilter;
import pe.edu.unmsm.servicios.auth.jwt.JwtService;
import pe.edu.unmsm.servicios.auth.service.AuthService;
import pe.edu.unmsm.servicios.auth.service.LoginRateLimiter;
import pe.edu.unmsm.servicios.catalogo.entity.Rol;
import pe.edu.unmsm.servicios.common.BusinessException;
import pe.edu.unmsm.servicios.common.GlobalExceptionHandler;
import pe.edu.unmsm.servicios.common.ProblemDetailAuthHandlers;
import pe.edu.unmsm.servicios.usuario.controller.AdminUsuarioController;
import pe.edu.unmsm.servicios.usuario.dto.UsuarioResponse;
import pe.edu.unmsm.servicios.usuario.entity.Usuario;
import pe.edu.unmsm.servicios.usuario.repository.UsuarioRepository;
import pe.edu.unmsm.servicios.usuario.service.UsuarioService;

@WebMvcTest(controllers = {AuthController.class, AdminUsuarioController.class})
@Import({
        SecurityConfig.class,
        WebConfig.class,
        JwtService.class,
        JwtAuthenticationFilter.class,
        ProblemDetailAuthHandlers.class,
        GlobalExceptionHandler.class,
        LoginRateLimiter.class,
        SecurityHardeningWebMvcTest.TestConfig.class
})
@TestPropertySource(properties = {
        "app.jwt.secret=dGVzdC1vbmx5LXNlY3JldC1rZXktZm9yLWludGVncmF0aW9uLXRlc3RzLTEyMzQ1Njc4OTA=",
        "app.jwt.expiration-minutes=60",
        "app.cors.allowed-origins=http://localhost:5173,https://servicios.unmsm.edu.pe",
        "app.cors.allow-credentials=false",
        "app.upload-dir=target/test-uploads",
        "app.admin.email=",
        "app.admin.password=",
        "app.login-rate-limit.max-attempts=3",
        "app.login-rate-limit.window-minutes=15",
        "spring.autoconfigure.exclude="
                + "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,"
                + "org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration,"
                + "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration,"
                + "org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration"
})
class SecurityHardeningWebMvcTest {

    private static final Instant INICIO = Instant.parse("2026-11-13T12:00:00Z");

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private MutableClock mutableClock;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private UsuarioService usuarioService;

    @MockitoBean
    private UsuarioRepository usuarioRepository;

    private String tokenSolicitante;
    private String tokenAdmin;

    @BeforeEach
    void setUp() {
        mutableClock.reset(INICIO);
        tokenSolicitante = jwtService.generar(mockUsuario(1L, "ana@unmsm.edu.pe", "SOLICITANTE"));
        tokenAdmin = jwtService.generar(mockUsuario(2L, "admin@unmsm.edu.pe", "ADMIN"));

        when(authService.perfil(1L))
                .thenReturn(new UsuarioResponse(1L, "Ana", "ana@unmsm.edu.pe", "SOLICITANTE", null, true));
        when(usuarioRepository.findAll(any(Pageable.class))).thenReturn(Page.empty());
    }

    @Test
    void publicEndpointReturnsOwaspSecurityHeaders() throws Exception {
        when(authService.login(any(LoginRequest.class)))
                .thenReturn(new LoginResponse("jwt-token",
                        new UsuarioResponse(1L, "Ana", "ana.headers@unmsm.edu.pe", "SOLICITANTE", null, true)));

        ResultActions result = mvc.perform(post("/api/auth/login")
                        .secure(true)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\":\"ana.headers@unmsm.edu.pe\",\"password\":\"Clave12345\"}"))
                .andExpect(status().isOk());

        assertSecurityHeaders(result)
                .andExpect(header().exists("Strict-Transport-Security"))
                .andExpect(header().string("Content-Security-Policy", containsString("script-src 'self'")))
                .andExpect(header().string("Content-Security-Policy", containsString("style-src 'self' 'unsafe-inline'")))
                .andExpect(header().string("Content-Security-Policy", containsString("img-src 'self' data:")));
    }

    @Test
    void authenticatedEndpointReturnsOwaspSecurityHeaders() throws Exception {
        ResultActions result = mvc.perform(get("/api/auth/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenSolicitante))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.correo").value("ana@unmsm.edu.pe"));

        assertSecurityHeaders(result);
    }

    @Test
    void corsPreflightAllowsConfiguredOriginAndRejectsUnknownOrigin() throws Exception {
        mvc.perform(options("/api/auth/login")
                        .header(HttpHeaders.ORIGIN, "http://localhost:5173")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:5173"));

        mvc.perform(options("/api/admin/usuarios")
                        .header(HttpHeaders.ORIGIN, "https://servicios.unmsm.edu.pe")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "https://servicios.unmsm.edu.pe"));

        mvc.perform(options("/api/auth/login")
                        .header(HttpHeaders.ORIGIN, "https://evil.example")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
    }

    @Test
    void returns401WithoutTokenOrWithInvalidToken() throws Exception {
        String withoutToken = mvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("NO_AUTENTICADO"))
                .andReturn().getResponse().getContentAsString();
        assertThat(withoutToken).doesNotContain("java.").doesNotContain("pe.edu.unmsm");

        String invalidToken = mvc.perform(get("/api/auth/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer token.manipulado.invalido"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("NO_AUTENTICADO"))
                .andReturn().getResponse().getContentAsString();
        assertThat(invalidToken).doesNotContain("JwtException").doesNotContain("pe.edu.unmsm");
    }

    @Test
    void returns403ForWrongRoleOnAdminEndpointAnd200ForAdmin() throws Exception {
        String forbiddenBody = mvc.perform(get("/api/admin/usuarios")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenSolicitante))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("ACCESO_DENEGADO"))
                .andReturn().getResponse().getContentAsString();
        assertThat(forbiddenBody).doesNotContain("AccessDeniedException").doesNotContain("pe.edu.unmsm");

        mvc.perform(get("/api/admin/usuarios")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAdmin))
                .andExpect(status().isOk());
    }

    @Test
    void unexpectedExceptionReturnsGeneric500WithoutStacktrace() throws Exception {
        String body = mvc.perform(get("/api/test/boom")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenSolicitante))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.codigo").value("ERROR_INTERNO"))
                .andExpect(jsonPath("$.detail").value("Ocurrió un error inesperado. Intente nuevamente."))
                .andReturn().getResponse().getContentAsString();

        assertThat(body)
                .doesNotContain("internal secret message")
                .doesNotContain("IllegalStateException")
                .doesNotContain("pe.edu.unmsm")
                .doesNotContain("stackTrace");
    }

    @Test
    void loginRateLimiterReturns429AfterConfiguredFailuresAndResetsAfterWindow() throws Exception {
        String correo = "ratelimit.window@unmsm.edu.pe";
        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new BusinessException(HttpStatus.UNAUTHORIZED, "CREDENCIALES_INVALIDAS",
                        "Credenciales inválidas", "Correo o contraseña incorrectos."));

        for (int i = 0; i < 3; i++) {
            performLogin(correo, "mala")
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.codigo").value("CREDENCIALES_INVALIDAS"));
        }

        performLogin(correo, "mala")
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists(HttpHeaders.RETRY_AFTER))
                .andExpect(jsonPath("$.codigo").value("DEMASIADOS_INTENTOS"));

        mutableClock.advance(Duration.ofMinutes(15));

        when(authService.login(any(LoginRequest.class)))
                .thenReturn(new LoginResponse("jwt-token",
                        new UsuarioResponse(1L, "Ana", correo, "SOLICITANTE", null, true)));
        performLogin(correo, "buena")
                .andExpect(status().isOk());
    }

    @Test
    void loginRateLimiterResetsCounterOnSuccessfulLogin() throws Exception {
        String correo = "ratelimit.reset@unmsm.edu.pe";
        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new BusinessException(HttpStatus.UNAUTHORIZED, "CREDENCIALES_INVALIDAS",
                        "Credenciales inválidas", "Correo o contraseña incorrectos."))
                .thenThrow(new BusinessException(HttpStatus.UNAUTHORIZED, "CREDENCIALES_INVALIDAS",
                        "Credenciales inválidas", "Correo o contraseña incorrectos."))
                .thenReturn(new LoginResponse("jwt-token",
                        new UsuarioResponse(1L, "Ana", correo, "SOLICITANTE", null, true)))
                .thenThrow(new BusinessException(HttpStatus.UNAUTHORIZED, "CREDENCIALES_INVALIDAS",
                        "Credenciales inválidas", "Correo o contraseña incorrectos."))
                .thenThrow(new BusinessException(HttpStatus.UNAUTHORIZED, "CREDENCIALES_INVALIDAS",
                        "Credenciales inválidas", "Correo o contraseña incorrectos."))
                .thenThrow(new BusinessException(HttpStatus.UNAUTHORIZED, "CREDENCIALES_INVALIDAS",
                        "Credenciales inválidas", "Correo o contraseña incorrectos."));

        performLogin(correo, "mala").andExpect(status().isUnauthorized());
        performLogin(correo, "mala").andExpect(status().isUnauthorized());
        performLogin(correo, "buena").andExpect(status().isOk());

        // Counter was reset by the successful login, so 3 more failures are permitted before 429
        performLogin(correo, "mala").andExpect(status().isUnauthorized());
        performLogin(correo, "mala").andExpect(status().isUnauthorized());
        performLogin(correo, "mala").andExpect(status().isUnauthorized());
        performLogin(correo, "mala").andExpect(status().isTooManyRequests());
    }

    private ResultActions performLogin(String correo, String password) throws Exception {
        return mvc.perform(post("/api/auth/login")
                .with(req -> {
                    req.setRemoteAddr("192.0.2.10");
                    return req;
                })
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"correo\":\"" + correo + "\",\"password\":\"" + password + "\"}"));
    }

    private static ResultActions assertSecurityHeaders(ResultActions result) throws Exception {
        return result
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("X-Frame-Options", "DENY"))
                .andExpect(header().string("Referrer-Policy", "strict-origin-when-cross-origin"))
                .andExpect(header().string("Content-Security-Policy", containsString("default-src 'self'")))
                .andExpect(header().string("Content-Security-Policy", containsString("frame-ancestors 'none'")))
                .andExpect(header().string("Content-Security-Policy", containsString("object-src 'none'")));
    }

    private static Usuario mockUsuario(Long id, String correo, String rolNombre) {
        Rol rol = mock(Rol.class);
        when(rol.getNombre()).thenReturn(rolNombre);
        Usuario usuario = mock(Usuario.class);
        when(usuario.getId()).thenReturn(id);
        when(usuario.getCorreo()).thenReturn(correo);
        when(usuario.getRol()).thenReturn(rol);
        return usuario;
    }

    @TestConfiguration
    @EnableConfigurationProperties(AppProperties.class)
    static class TestConfig {

        @Bean
        @Primary
        MutableClock mutableClock() {
            return new MutableClock(INICIO);
        }

        @RestController
        static class ProbeController {
            @GetMapping("/api/test/boom")
            void boom() {
                throw new IllegalStateException("internal secret message");
            }
        }
    }

    static final class MutableClock extends Clock {
        private final AtomicReference<Instant> current;

        MutableClock(Instant initial) {
            this.current = new AtomicReference<>(initial);
        }

        void reset(Instant instant) {
            current.set(instant);
        }

        void advance(Duration duration) {
            current.updateAndGet(now -> now.plus(duration));
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return current.get();
        }
    }
}
