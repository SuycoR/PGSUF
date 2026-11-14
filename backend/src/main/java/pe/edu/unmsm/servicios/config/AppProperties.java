package pe.edu.unmsm.servicios.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.ConstructorBinding;

/**
 * Application settings bound from the {@code app.*} keys of application.yml.
 * Every secret comes from an environment variable; nothing is hardcoded.
 */
@ConfigurationProperties(prefix = "app")
public record AppProperties(Jwt jwt, Cors cors, String uploadDir, Admin admin, LoginRateLimit loginRateLimit) {

    @ConstructorBinding
    public AppProperties {
        if (cors == null) {
            cors = new Cors(List.of(), false);
        }
        if (loginRateLimit == null) {
            loginRateLimit = new LoginRateLimit(5, 15);
        }
    }

    public AppProperties(Jwt jwt, Cors cors, String uploadDir, Admin admin) {
        this(jwt, cors, uploadDir, admin, new LoginRateLimit(5, 15));
    }

    public record Jwt(String secret, long expirationMinutes) {
    }

    public record Cors(List<String> allowedOrigins, boolean allowCredentials) {

        @ConstructorBinding
        public Cors {
            if (allowedOrigins == null) {
                allowedOrigins = List.of();
            }
        }

        public Cors(List<String> allowedOrigins) {
            this(allowedOrigins, false);
        }
    }

    public record Admin(String email, String password) {
    }

    public record LoginRateLimit(int maxAttempts, long windowMinutes) {
    }
}
