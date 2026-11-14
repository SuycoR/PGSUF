package pe.edu.unmsm.servicios.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

class CorsOriginsValidatorTest {

    @Test
    void trimsIgnoresBlanksAndDeduplicatesOrigins() {
        List<String> origins = Arrays.asList(" http://localhost:5173 ", " ", null, "https://app.unmsm.edu.pe",
                "http://localhost:5173");

        assertThat(CorsOriginsValidator.validate(origins, false, false))
                .containsExactly("http://localhost:5173", "https://app.unmsm.edu.pe");
    }

    @Test
    void rejectsWildcardWhenCredentialsAreAllowed() {
        assertThatThrownBy(() -> CorsOriginsValidator.validate(List.of("*"), true, false))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("allowCredentials=true");
    }

    @Test
    void rejectsWildcardInProduction() {
        assertThatThrownBy(() -> CorsOriginsValidator.validate(List.of("*"), false, true))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("production");
    }

    @Test
    void rejectsEmptyOriginsInProduction() {
        assertThatThrownBy(() -> CorsOriginsValidator.validate(List.of("   ", ""), false, true))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("CORS_ALLOWED_ORIGINS");
    }

    @Test
    void allowsEmptyOriginsOutsideProduction() {
        assertThat(CorsOriginsValidator.validate(List.of(), false, false)).isEmpty();
    }
}
