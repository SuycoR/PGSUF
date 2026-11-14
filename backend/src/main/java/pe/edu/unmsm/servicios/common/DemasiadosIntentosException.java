package pe.edu.unmsm.servicios.common;

import org.springframework.http.HttpStatus;

/**
 * Raised when a client exceeds the allowed rate of failed login attempts.
 */
public class DemasiadosIntentosException extends BusinessException {

    private final long retryAfterSeconds;

    public DemasiadosIntentosException(long retryAfterSeconds) {
        super(HttpStatus.TOO_MANY_REQUESTS, "DEMASIADOS_INTENTOS", "Demasiados intentos",
                "Ha superado el límite de intentos de inicio de sesión. Intente nuevamente más tarde.");
        this.retryAfterSeconds = Math.max(1L, retryAfterSeconds);
    }

    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
