package pe.edu.unmsm.servicios.auth.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.stereotype.Component;

import pe.edu.unmsm.servicios.common.DemasiadosIntentosException;
import pe.edu.unmsm.servicios.config.AppProperties;
import pe.edu.unmsm.servicios.usuario.Correos;

/**
 * In-memory sliding-window rate limiter for failed login attempts keyed by client IP and email.
 */
@Component
public class LoginRateLimiter {

    private static final int MAX_ENTRADAS_EN_MEMORIA = 10_000;

    private record Key(String ip, String correo) {
    }

    private final ConcurrentHashMap<Key, Deque<Instant>> intentosFallidos = new ConcurrentHashMap<>();
    private final int maxIntentos;
    private final Duration ventana;
    private final Clock clock;

    public LoginRateLimiter(AppProperties properties, Clock clock) {
        AppProperties.LoginRateLimit config = properties.loginRateLimit();
        int max = config != null ? config.maxAttempts() : 5;
        long minutos = config != null ? config.windowMinutes() : 15;
        if (max <= 0 || minutos <= 0) {
            throw new IllegalArgumentException("loginRateLimit maxAttempts and windowMinutes must be positive");
        }
        this.maxIntentos = max;
        this.ventana = Duration.ofMinutes(minutos);
        this.clock = clock;
    }

    /** Throws {@link DemasiadosIntentosException} if the IP+email pair is currently blocked. */
    public void verificarPermitido(String ip, String correo) {
        Instant ahora = clock.instant();
        Instant limite = ahora.minus(ventana);
        AtomicLong retryAfter = new AtomicLong(0L);

        intentosFallidos.computeIfPresent(key(ip, correo), (k, cola) -> {
            evictExpired(cola, limite);
            if (cola.isEmpty()) {
                return null;
            }
            if (cola.size() >= maxIntentos) {
                Instant desbloqueo = cola.peekFirst().plus(ventana);
                long millisRestantes = Math.max(1L, Duration.between(ahora, desbloqueo).toMillis());
                retryAfter.set((millisRestantes + 999L) / 1000L);
            }
            return cola;
        });

        if (retryAfter.get() > 0L) {
            throw new DemasiadosIntentosException(retryAfter.get());
        }
    }

    /** Records a failed login attempt for the IP+email pair. */
    public void registrarFallo(String ip, String correo) {
        Instant ahora = clock.instant();
        Instant limite = ahora.minus(ventana);
        if (intentosFallidos.size() >= MAX_ENTRADAS_EN_MEMORIA) {
            limpiarExpirados(limite);
        }
        intentosFallidos.compute(key(ip, correo), (k, existente) -> {
            Deque<Instant> cola = existente == null ? new ArrayDeque<>() : existente;
            evictExpired(cola, limite);
            cola.addLast(ahora);
            return cola;
        });
    }

    /** Clears recorded failures after a successful login. */
    public void registrarExito(String ip, String correo) {
        intentosFallidos.remove(key(ip, correo));
    }

    private Key key(String ip, String correo) {
        String ipNormalizada = (ip == null || ip.isBlank()) ? "unknown" : ip.trim();
        String correoNormalizado = correo == null ? "" : Correos.normalizar(correo);
        return new Key(ipNormalizada, correoNormalizado);
    }

    private static void evictExpired(Deque<Instant> cola, Instant limite) {
        while (!cola.isEmpty() && !cola.peekFirst().isAfter(limite)) {
            cola.pollFirst();
        }
    }

    private void limpiarExpirados(Instant limite) {
        intentosFallidos.forEach((key, value) -> intentosFallidos.computeIfPresent(key, (k, cola) -> {
            evictExpired(cola, limite);
            return cola.isEmpty() ? null : cola;
        }));
    }
}
