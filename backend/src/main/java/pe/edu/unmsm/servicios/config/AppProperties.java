package pe.edu.unmsm.servicios.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Application settings bound from the {@code app.*} keys of application.yml.
 * Every secret comes from an environment variable; nothing is hardcoded.
 */
@ConfigurationProperties(prefix = "app")
public record AppProperties(Jwt jwt, Cors cors, String uploadDir, Admin admin) {

    public record Jwt(String secret, long expirationMinutes) {
    }

    public record Cors(List<String> allowedOrigins) {
    }

    public record Admin(String email, String password) {
    }
}
