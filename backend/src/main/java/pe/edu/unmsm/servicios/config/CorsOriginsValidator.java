package pe.edu.unmsm.servicios.config;

import java.util.List;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;

/**
 * Normalizes and validates {@code app.cors.allowed-origins} at application startup.
 */
public final class CorsOriginsValidator {

    private static final Logger log = LoggerFactory.getLogger(CorsOriginsValidator.class);

    private CorsOriginsValidator() {
    }

    public static List<String> validate(AppProperties.Cors cors, Environment environment) {
        boolean isProd = environment.acceptsProfiles(Profiles.of("prod"));
        List<String> raw = cors != null ? cors.allowedOrigins() : List.of();
        boolean allowCredentials = cors != null && cors.allowCredentials();
        return validate(raw, allowCredentials, isProd);
    }

    public static List<String> validate(List<String> rawOrigins, boolean allowCredentials, boolean isProd) {
        List<String> normalized = (rawOrigins == null ? List.<String>of() : rawOrigins).stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(origin -> !origin.isEmpty())
                .distinct()
                .toList();

        boolean hasWildcard = normalized.contains("*");
        if (hasWildcard && allowCredentials) {
            throw new IllegalStateException(
                    "CORS misconfiguration: wildcard '*' cannot be combined with allowCredentials=true");
        }
        if (isProd && hasWildcard) {
            throw new IllegalStateException(
                    "CORS misconfiguration: wildcard '*' is not allowed in production");
        }
        if (normalized.isEmpty()) {
            if (isProd) {
                throw new IllegalStateException(
                        "CORS misconfiguration: CORS_ALLOWED_ORIGINS must define at least one origin in production");
            }
            log.warn("No CORS allowed origins configured; cross-origin browser requests will be rejected");
        }
        return normalized;
    }
}
