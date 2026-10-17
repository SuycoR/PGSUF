package pe.edu.unmsm.servicios.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import pe.edu.unmsm.servicios.IntegrationTest;
import pe.edu.unmsm.servicios.usuario.repository.UsuarioRepository;

@IntegrationTest
class AuthIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper json;

    @Autowired
    private UsuarioRepository usuarios;

    private ResultActions registrar(String nombre, String correo, String password) throws Exception {
        return mvc.perform(post("/api/auth/registro").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(new Body(nombre, correo, password))));
    }

    private ResultActions login(String correo, String password) throws Exception {
        return mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"correo\":\"" + correo + "\",\"password\":\"" + password + "\"}"));
    }

    record Body(String nombre, String correo, String password) {
    }

    @Test
    void hu02Scenario1RegistersARequesterThatCanLogIn() throws Exception {
        registrar("Ana Estudiante", "Ana@UNMSM.edu.pe", "Estudiante1")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.rol").value("SOLICITANTE"))
                .andExpect(jsonPath("$.correo").value("ana@unmsm.edu.pe"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());

        String body = login("ana@unmsm.edu.pe", "Estudiante1")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usuario.nombre").value("Ana Estudiante"))
                .andReturn().getResponse().getContentAsString();
        String token = json.readTree(body).get("token").asText();

        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.correo").value("ana@unmsm.edu.pe"));
    }

    @Test
    void hu02Scenario2RejectsADuplicateEmailIgnoringCase() throws Exception {
        registrar("Ana", "ana@unmsm.edu.pe", "Estudiante1").andExpect(status().isCreated());

        registrar("Otra Ana", "ANA@unmsm.edu.pe", "Estudiante1")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("CORREO_DUPLICADO"));
    }

    @Test
    void hu02Scenario3RejectsAShortPassword() throws Exception {
        registrar("Ana", "ana@unmsm.edu.pe", "corta")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores[0].campo").value("password"));
    }

    @Test
    void hu01Scenario2SameErrorForWrongPasswordAndUnknownEmail() throws Exception {
        registrar("Ana", "ana@unmsm.edu.pe", "Estudiante1");

        login("ana@unmsm.edu.pe", "incorrecta")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("CREDENCIALES_INVALIDAS"));
        login("nadie@unmsm.edu.pe", "incorrecta")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("CREDENCIALES_INVALIDAS"));
    }

    @Test
    void hu01Scenario4AnInactiveUserCannotLogIn() throws Exception {
        registrar("Ana", "ana@unmsm.edu.pe", "Estudiante1");
        usuarios.findByCorreo("ana@unmsm.edu.pe").orElseThrow().setActivo(false);

        login("ana@unmsm.edu.pe", "Estudiante1")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("CREDENCIALES_INVALIDAS"));
    }

    @Test
    void hu03Scenario3ProfileWithoutTokenIsUnauthorized() throws Exception {
        mvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("NO_AUTENTICADO"));
    }

    @Test
    void theTokenOfOneUserDoesNotGiveAccessToAnotherProfile() throws Exception {
        registrar("Ana", "ana@unmsm.edu.pe", "Estudiante1");
        registrar("Beto", "beto@unmsm.edu.pe", "Estudiante1");
        JsonNode sesion = json.readTree(login("beto@unmsm.edu.pe", "Estudiante1").andReturn().getResponse().getContentAsString());

        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + sesion.get("token").asText()))
                .andExpect(jsonPath("$.correo").value("beto@unmsm.edu.pe"));
    }
}
