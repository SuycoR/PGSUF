package pe.edu.unmsm.servicios.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

class GlobalExceptionHandlerTest {

    record Payload(@NotBlank(message = "es obligatorio") String descripcion) {
    }

    @RestController
    static class FakeController {

        @GetMapping("/conflict")
        void conflict() {
            throw BusinessException.conflict("CRUCE_HORARIO", "Cruce de horario",
                    "El ambiente ya está reservado en ese horario.");
        }

        @GetMapping("/boom")
        void boom() {
            throw new IllegalStateException("secret internal detail");
        }

        @GetMapping("/rate-limited")
        void rateLimited() {
            throw new DemasiadosIntentosException(120);
        }

        @GetMapping("/items/{id}")
        void byId(@PathVariable Long id) {
        }

        @PostMapping("/validate")
        void validate(@Valid @RequestBody Payload payload) {
        }
    }

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new FakeController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void businessExceptionBecomesProblemDetailWithCode() throws Exception {
        mockMvc.perform(get("/conflict"))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Cruce de horario"))
                .andExpect(jsonPath("$.codigo").value("CRUCE_HORARIO"));
    }

    @Test
    void validationErrorsListEachField() throws Exception {
        mockMvc.perform(post("/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"descripcion\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("DATOS_INVALIDOS"))
                .andExpect(jsonPath("$.errores[0].campo").value("descripcion"))
                .andExpect(jsonPath("$.errores[0].mensaje").value("es obligatorio"));
    }

    @Test
    void unexpectedErrorDoesNotLeakInternalDetail() throws Exception {
        String body = mockMvc.perform(get("/boom"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.codigo").value("ERROR_INTERNO"))
                .andExpect(jsonPath("$.detail").value("Ocurrió un error inesperado. Intente nuevamente."))
                .andReturn().getResponse().getContentAsString();

        assertThat(body)
                .doesNotContain("secret internal detail")
                .doesNotContain("IllegalStateException")
                .doesNotContain("pe.edu.unmsm")
                .doesNotContain("stackTrace");
    }

    @Test
    void malformedJsonAndTypeMismatchDoNotLeakClassNames() throws Exception {
        String badJson = mockMvc.perform(post("/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{invalid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("SOLICITUD_INVALIDA"))
                .andReturn().getResponse().getContentAsString();
        assertThat(badJson).doesNotContain("jackson").doesNotContain("com.fasterxml");

        String badParam = mockMvc.perform(get("/items/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("PARAMETRO_INVALIDO"))
                .andReturn().getResponse().getContentAsString();
        assertThat(badParam).doesNotContain("java.lang.Long");
    }

    @Test
    void rateLimitExceptionSets429AndRetryAfterHeader() throws Exception {
        mockMvc.perform(get("/rate-limited"))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("Retry-After", "120"))
                .andExpect(jsonPath("$.codigo").value("DEMASIADOS_INTENTOS"));
    }
}
