package pe.edu.unmsm.servicios.common;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
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
        mockMvc.perform(get("/boom"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.codigo").value("ERROR_INTERNO"))
                .andExpect(jsonPath("$.detail").value("Ocurrió un error inesperado. Intente nuevamente."));
    }
}
