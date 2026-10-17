package pe.edu.unmsm.servicios.usuario.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;

import pe.edu.unmsm.servicios.catalogo.entity.Rol;
import pe.edu.unmsm.servicios.config.AppProperties;
import pe.edu.unmsm.servicios.usuario.repository.UsuarioRepository;

/**
 * Creates the first administrator from ADMIN_EMAIL / ADMIN_PASSWORD when none exists.
 * No user is ever seeded in the migrations, and the password is never logged.
 */
@Component
public class AdminInicialRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminInicialRunner.class);
    private static final int LONGITUD_MINIMA_PROD = 12;

    private final AppProperties properties;
    private final UsuarioRepository usuarioRepository;
    private final UsuarioService usuarioService;
    private final Environment environment;

    public AdminInicialRunner(AppProperties properties, UsuarioRepository usuarioRepository,
            UsuarioService usuarioService, Environment environment) {
        this.properties = properties;
        this.usuarioRepository = usuarioRepository;
        this.usuarioService = usuarioService;
        this.environment = environment;
    }

    @Override
    public void run(ApplicationArguments args) {
        String email = properties.admin().email();
        String password = properties.admin().password();

        if (usuarioRepository.existsByRolNombre(Rol.ADMIN)) {
            return;
        }
        if (email == null || email.isBlank() || password == null || password.isBlank()) {
            log.warn("No administrator exists and ADMIN_EMAIL / ADMIN_PASSWORD are not set");
            return;
        }
        if (environment.acceptsProfiles(Profiles.of("prod")) && password.length() < LONGITUD_MINIMA_PROD) {
            throw new IllegalStateException(
                    "ADMIN_PASSWORD is too weak for production: use at least " + LONGITUD_MINIMA_PROD + " characters");
        }

        usuarioService.crear("Administrador", email, password, Rol.ADMIN, null);
        log.info("Initial administrator created: {}", email);
    }
}
