package pe.edu.unmsm.servicios.usuario;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import pe.edu.unmsm.servicios.IntegrationTest;
import pe.edu.unmsm.servicios.auth.jwt.JwtService;
import pe.edu.unmsm.servicios.usuario.service.UsuarioService;

@IntegrationTest
class AdminUsuarioIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private JwtService jwtService;

    private String tokenAdmin;
    private String tokenSolicitante;

    @BeforeEach
    void setUp() {
        tokenAdmin = jwtService.generar(usuarioService.crear("Admin", "admin.test@unmsm.edu.pe", "Admin12345", "ADMIN", null));
        tokenSolicitante = jwtService.generar(usuarioService.crear("Ana", "ana@unmsm.edu.pe", "Estudiante1", "SOLICITANTE", null));
    }

    private ResultActions crear(String token, String body) throws Exception {
        var request = post("/api/admin/usuarios").contentType(MediaType.APPLICATION_JSON).content(body);
        if (token != null) {
            request.header("Authorization", "Bearer " + token);
        }
        return mvc.perform(request);
    }

    private static String usuario(String correo, String rol, Integer areaId) {
        return """
                {"nombre":"Usuario %s","correo":"%s","password":"Clave12345","rol":"%s","areaId":%s}"""
                .formatted(rol, correo, rol, areaId);
    }

    @Test
    void adminCreatesATechnicianOfAnArea() throws Exception {
        crear(tokenAdmin, usuario("tec@unmsm.edu.pe", "TECNICO", 1))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.rol").value("TECNICO"))
                .andExpect(jsonPath("$.areaId").value(1));
    }

    @Test
    void adminCreatesAreaStaffAndASupervisor() throws Exception {
        crear(tokenAdmin, usuario("sec@unmsm.edu.pe", "PERSONAL_AREA", 2)).andExpect(status().isCreated());
        crear(tokenAdmin, usuario("sup@unmsm.edu.pe", "SUPERVISOR", null)).andExpect(status().isCreated());
    }

    @Test
    void hu12Scenario3TechnicianWithoutAreaIsRejected() throws Exception {
        crear(tokenAdmin, usuario("tec@unmsm.edu.pe", "TECNICO", null))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("AREA_OBLIGATORIA"));
    }

    @Test
    void supervisorWithAreaIsRejected() throws Exception {
        crear(tokenAdmin, usuario("sup@unmsm.edu.pe", "SUPERVISOR", 1))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("AREA_NO_PERMITIDA"));
    }

    @Test
    void hu03Scenario2ARequesterCannotCreateUsers() throws Exception {
        crear(tokenSolicitante, usuario("tec@unmsm.edu.pe", "TECNICO", 1))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("ACCESO_DENEGADO"));
    }

    @Test
    void withoutTokenTheAdminApiIsUnauthorized() throws Exception {
        crear(null, usuario("tec@unmsm.edu.pe", "TECNICO", 1)).andExpect(status().isUnauthorized());
    }

    @Test
    void adminListsUsersPaginated() throws Exception {
        mvc.perform(get("/api/admin/usuarios").header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements").value(2))
                .andExpect(jsonPath("$.content[0].passwordHash").doesNotExist());
    }
}
