package pe.edu.unmsm.servicios.comentario;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;

import com.fasterxml.jackson.databind.ObjectMapper;

import pe.edu.unmsm.servicios.IntegrationTest;
import pe.edu.unmsm.servicios.auth.jwt.JwtService;
import pe.edu.unmsm.servicios.usuario.entity.Usuario;
import pe.edu.unmsm.servicios.usuario.service.UsuarioService;

@IntegrationTest
class ComentarioIntegrationTest {

    private static final byte[] PDF = "%PDF-1.4\n%sustento".getBytes();

    @Autowired
    private MockMvc mvc;
    @Autowired
    private ObjectMapper json;
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private UsuarioService usuarioService;
    @Autowired
    private JwtService jwt;

    private String ana;
    private String beto;
    private String supervisor;
    private String tecnico1;
    private Long tecnico1Id;
    private String tecnico2;
    private String secretaria;
    private String matricula;
    private String admin;

    private int proyector;
    private int rectificacionNota;
    private int media;
    private int enEvaluacion;

    @BeforeEach
    void setUp() {
        int soporteAreaId = jdbc.queryForObject("SELECT id FROM area WHERE nombre = 'Soporte técnico'", Integer.class);
        int secAreaId = jdbc.queryForObject("SELECT id FROM area WHERE nombre = 'Secretaría académica'", Integer.class);
        int matAreaId = jdbc.queryForObject("SELECT id FROM area WHERE nombre = 'Oficina de matrícula'", Integer.class);

        ana = jwt.generar(usuarioService.crear("Ana", "ana.com@unmsm.edu.pe", "Clave12345", "SOLICITANTE", null));
        beto = jwt.generar(usuarioService.crear("Beto", "beto.com@unmsm.edu.pe", "Clave12345", "SOLICITANTE", null));
        supervisor = jwt.generar(usuarioService.crear("Sup", "sup.com@unmsm.edu.pe", "Clave12345", "SUPERVISOR", null));
        Usuario tec1 = usuarioService.crear("Luis Tec", "tec1.com@unmsm.edu.pe", "Clave12345", "TECNICO", soporteAreaId);
        tecnico1Id = tec1.getId();
        tecnico1 = jwt.generar(tec1);
        tecnico2 = jwt.generar(usuarioService.crear("Pedro Tec", "tec2.com@unmsm.edu.pe", "Clave12345", "TECNICO", soporteAreaId));
        secretaria = jwt.generar(usuarioService.crear("Sec", "sec.com@unmsm.edu.pe", "Clave12345", "PERSONAL_AREA", secAreaId));
        matricula = jwt.generar(usuarioService.crear("Mat", "mat.com@unmsm.edu.pe", "Clave12345", "PERSONAL_AREA", matAreaId));
        admin = jwt.generar(usuarioService.crear("Adm", "adm.com@unmsm.edu.pe", "Clave12345", "ADMIN", null));

        proyector = jdbc.queryForObject("SELECT id FROM tipo_solicitud WHERE nombre LIKE 'Equipo audiovisual%'", Integer.class);
        rectificacionNota = jdbc.queryForObject("SELECT id FROM tipo_solicitud WHERE nombre = 'Rectificación de nota'", Integer.class);
        media = jdbc.queryForObject("SELECT id FROM prioridad WHERE nombre = 'Media'", Integer.class);
        enEvaluacion = jdbc.queryForObject("SELECT id FROM estado WHERE nombre = 'En evaluación'", Integer.class);
    }

    record Datos(Integer tipoSolicitudId, Integer prioridadId, String descripcion, String ubicacion) {
    }

    private Long crearSolicitud(String token, int tipoId, MockMultipartFile... archivos) throws Exception {
        String datos = json.writeValueAsString(new Datos(tipoId, media, "Descripción prueba", "Pabellón Nuevo"));
        MockMultipartHttpServletRequestBuilder request = multipart("/api/solicitudes");
        request.file(new MockMultipartFile("datos", "", MediaType.APPLICATION_JSON_VALUE, datos.getBytes()));
        for (MockMultipartFile archivo : archivos) {
            request.file(archivo);
        }
        String body = mvc.perform(request.header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return json.readTree(body).get("id").asLong();
    }

    @Test
    void commentsRequireAuthenticationAndVisibility() throws Exception {
        Long tecnicaId = crearSolicitud(ana, proyector);
        Long tramiteId = crearSolicitud(ana, rectificacionNota,
                new MockMultipartFile("archivos", "sustento.pdf", "application/pdf", PDF));

        // 401 when unauthenticated
        mvc.perform(get("/api/solicitudes/" + tecnicaId + "/comentarios"))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/solicitudes/" + tecnicaId + "/comentarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"texto\":\"Hola\"}"))
                .andExpect(status().isUnauthorized());

        // Unrelated requester, unassigned technician, supervisor on procedure, and other area staff get 404 (RN-20)
        for (String token : new String[]{beto, tecnico2}) {
            mvc.perform(get("/api/solicitudes/" + tecnicaId + "/comentarios")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isNotFound());
            mvc.perform(post("/api/solicitudes/" + tecnicaId + "/comentarios")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"texto\":\"Intruso\"}"))
                    .andExpect(status().isNotFound());
        }

        for (String token : new String[]{supervisor, matricula}) {
            mvc.perform(get("/api/solicitudes/" + tramiteId + "/comentarios")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isNotFound());
            mvc.perform(post("/api/solicitudes/" + tramiteId + "/comentarios")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"texto\":\"Fuera de área\"}"))
                    .andExpect(status().isNotFound());
        }

        // Area staff of the matching area and admin can comment on procedure
        mvc.perform(post("/api/solicitudes/" + tramiteId + "/comentarios")
                        .header("Authorization", "Bearer " + secretaria)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"texto\":\"Revisando expediente\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.autorRol").value("PERSONAL_AREA"));

        mvc.perform(get("/api/solicitudes/" + tramiteId + "/comentarios")
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void involvedUsersPostAndReadCommentsInChronologicalOrder() throws Exception {
        Long id = crearSolicitud(ana, proyector);

        // Assign technician so Ana, Supervisor, and Luis Tec are all involved
        mvc.perform(put("/api/solicitudes/" + id + "/estado")
                        .header("Authorization", "Bearer " + supervisor)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estadoId\":" + enEvaluacion + "}"))
                .andExpect(status().isOk());
        mvc.perform(put("/api/solicitudes/" + id + "/asignar")
                        .header("Authorization", "Bearer " + supervisor)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"responsableId\":" + tecnico1Id + "}"))
                .andExpect(status().isOk());

        // Empty comment rejected with 400
        mvc.perform(post("/api/solicitudes/" + id + "/comentarios")
                        .header("Authorization", "Bearer " + ana)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"texto\":\"   \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("DATOS_INVALIDOS"));

        mvc.perform(post("/api/solicitudes/" + id + "/comentarios")
                        .header("Authorization", "Bearer " + ana)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"texto\":\"El cable HDMI también falla\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.autor").value("Ana"))
                .andExpect(jsonPath("$.autorRol").value("SOLICITANTE"));

        mvc.perform(post("/api/solicitudes/" + id + "/comentarios")
                        .header("Authorization", "Bearer " + tecnico1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"texto\":\"Llevaré repuesto mañana\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.autor").value("Luis Tec"))
                .andExpect(jsonPath("$.autorRol").value("TECNICO"));

        mvc.perform(get("/api/solicitudes/" + id + "/comentarios")
                        .header("Authorization", "Bearer " + supervisor))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].texto").value("El cable HDMI también falla"))
                .andExpect(jsonPath("$[1].texto").value("Llevaré repuesto mañana"));
    }
}
