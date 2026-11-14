package pe.edu.unmsm.servicios.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import pe.edu.unmsm.servicios.common.DemasiadosIntentosException;
import pe.edu.unmsm.servicios.config.AppProperties;

class LoginRateLimiterTest {

    private MutableClock clock;
    private LoginRateLimiter limiter;

    @BeforeEach
    void setUp() {
        clock = new MutableClock(Instant.parse("2026-11-13T12:00:00Z"));
        AppProperties props = new AppProperties(
                new AppProperties.Jwt("x", 60),
                new AppProperties.Cors(List.of()),
                "./uploads",
                new AppProperties.Admin("", ""),
                new AppProperties.LoginRateLimit(5, 15));
        limiter = new LoginRateLimiter(props, clock);
    }

    @Test
    void blocksAfterMaxFailedAttemptsWithRetryAfter() {
        for (int i = 0; i < 5; i++) {
            limiter.verificarPermitido("10.0.0.1", "Ana@unmsm.edu.pe");
            limiter.registrarFallo("10.0.0.1", "ana@unmsm.edu.pe");
            clock.advance(Duration.ofSeconds(10));
        }

        assertThatThrownBy(() -> limiter.verificarPermitido("10.0.0.1", "ANA@UNMSM.EDU.PE"))
                .isInstanceOf(DemasiadosIntentosException.class)
                .satisfies(ex -> assertThat(((DemasiadosIntentosException) ex).getRetryAfterSeconds())
                        .isEqualTo(15 * 60 - 50));
    }

    @Test
    void isolatesBucketsByIpAndEmail() {
        for (int i = 0; i < 5; i++) {
            limiter.registrarFallo("10.0.0.1", "ana@unmsm.edu.pe");
        }

        assertThatCode(() -> limiter.verificarPermitido("10.0.0.2", "ana@unmsm.edu.pe"))
                .doesNotThrowAnyException();
        assertThatCode(() -> limiter.verificarPermitido("10.0.0.1", "beto@unmsm.edu.pe"))
                .doesNotThrowAnyException();
    }

    @Test
    void resetsCounterOnSuccess() {
        for (int i = 0; i < 4; i++) {
            limiter.registrarFallo("10.0.0.1", "ana@unmsm.edu.pe");
        }
        limiter.registrarExito("10.0.0.1", "ana@unmsm.edu.pe");

        for (int i = 0; i < 5; i++) {
            limiter.verificarPermitido("10.0.0.1", "ana@unmsm.edu.pe");
            limiter.registrarFallo("10.0.0.1", "ana@unmsm.edu.pe");
        }

        assertThatThrownBy(() -> limiter.verificarPermitido("10.0.0.1", "ana@unmsm.edu.pe"))
                .isInstanceOf(DemasiadosIntentosException.class);
    }

    @Test
    void unblocksAfterWindowExpires() {
        for (int i = 0; i < 5; i++) {
            limiter.registrarFallo("10.0.0.1", "ana@unmsm.edu.pe");
        }
        assertThatThrownBy(() -> limiter.verificarPermitido("10.0.0.1", "ana@unmsm.edu.pe"))
                .isInstanceOf(DemasiadosIntentosException.class);

        clock.advance(Duration.ofMinutes(15));

        assertThatCode(() -> limiter.verificarPermitido("10.0.0.1", "ana@unmsm.edu.pe"))
                .doesNotThrowAnyException();
    }

    private static final class MutableClock extends Clock {
        private final AtomicReference<Instant> current;

        MutableClock(Instant initial) {
            this.current = new AtomicReference<>(initial);
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
