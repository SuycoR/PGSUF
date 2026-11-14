package pe.edu.unmsm.servicios.solicitud;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Clock;
import java.time.LocalDateTime;

import jakarta.persistence.EntityManager;

import org.hamcrest.Matchers;
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
class GestionSolicitudesIntegrationTest {

    private static final byte[] PDF = "%PDF-1.4\n%sustento".getBytes();
    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 13};

    @Autowired
    private Clock clock;

    @Autowired
    private EntityManager em;

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
    private String supervisor;
    private String tecnico1;
    private Long tecnico1Id;
    private String tecnico2;
    private String secretaria;
    private String matricula;
    private String admin;

    private int soporteAreaId;
    private int proyector;
    private int rectificacionNota;
    private int alta;
    private int media;
    private int enEvaluacion;
    private int enAtencion;
    private int resuelta;
    private int cerrada;
    private int rechazada;

    @BeforeEach
    void setUp() {
        soporteAreaId = jdbc.queryForObject("SELECT id FROM area WHERE nombre = 'Soporte técnico'", Integer.class);
        int secAreaId = jdbc.queryForObject("SELECT id FROM area WHERE nombre = 'Secretaría académica'", Integer.class);
        int matAreaId = jdbc.queryForObject("SELECT id FROM area WHERE nombre = 'Oficina de matrícula'", Integer.class);

        ana = jwt.generar(usuarioService.crear("Ana", "ana.s3@unmsm.edu.pe", "Clave12345", "SOLICITANTE", null));
        supervisor = jwt.generar(usuarioService.crear("Sup", "sup.s3@unmsm.edu.pe", "Clave12345", "SUPERVISOR", null));
        Usuario tec1 = usuarioService.crear("Luis Tec", "tec1.s3@unmsm.edu.pe", "Clave12345", "TECNICO", soporteAreaId);
        tecnico1Id = tec1.getId();
        tecnico1 = jwt.generar(tec1);
        tecnico2 = jwt.generar(usuarioService.crear("Pedro Tec", "tec2.s3@unmsm.edu.pe", "Clave12345", "TECNICO", soporteAreaId));
        secretaria = jwt.generar(usuarioService.crear("Sec", "sec.s3@unmsm.edu.pe", "Clave12345", "PERSONAL_AREA", secAreaId));
        matricula = jwt.generar(usuarioService.crear("Mat", "mat.s3@unmsm.edu.pe", "Clave12345", "PERSONAL_AREA", matAreaId));
        admin = jwt.generar(usuarioService.crear("Adm", "adm.s3@unmsm.edu.pe", "Clave12345", "ADMIN", null));

        proyector = jdbc.queryForObject("SELECT id FROM tipo_solicitud WHERE nombre LIKE 'Equipo audiovisual%'", Integer.class);
        rectificacionNota = jdbc.queryForObject("SELECT id FROM tipo_solicitud WHERE nombre = 'Rectificación de nota'", Integer.class);
        alta = jdbc.queryForObject("SELECT id FROM prioridad WHERE nombre = 'Alta'", Integer.class);
        media = jdbc.queryForObject("SELECT id FROM prioridad WHERE nombre = 'Media'", Integer.class);

        enEvaluacion = estadoId("En evaluación");
        enAtencion = estadoId("En atención");
        resuelta = estadoId("Resuelta");
        cerrada = estadoId("Cerrada");
        rechazada = estadoId("Rechazada");
    }

    private int estadoId(String nombre) {
        return jdbc.queryForObject("SELECT id FROM estado WHERE nombre = ?", Integer.class, nombre);
    }

    private Long crearSolicitud(String token, int tipoId, int prioridadId, MockMultipartFile... archivos) throws Exception {
        String datos = json.writeValueAsString(new Datos(tipoId, prioridadId, "Descripción prueba", "Pabellón Nuevo"));
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

    record Datos(Integer tipoSolicitudId, Integer prioridadId, String descripcion, String ubicacion) {
    }

    @Test
    void endpointsRequireAuthentication() throws Exception {
        mvc.perform(get("/api/solicitudes")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/solicitudes/asignadas")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/usuarios/tecnicos")).andExpect(status().isUnauthorized());
    }

    @Test
    void wrongRolesAreRejectedWith403() throws Exception {
        Long idTecnica = crearSolicitud(ana, proyector, media);
        Long idTramite = crearSolicitud(ana, rectificacionNota, media,
                new MockMultipartFile("archivos", "sustento.pdf", "application/pdf", PDF));

        // SOL calling /asignar
        mvc.perform(put("/api/solicitudes/" + idTecnica + "/asignar")
                        .header("Authorization", "Bearer " + ana)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"responsableId\":" + tecnico1Id + "}"))
                .andExpect(status().isForbidden());

        // SUP moves to En evaluación and assigns to tecnico1
        mvc.perform(put("/api/solicitudes/" + idTecnica + "/estado")
                        .header("Authorization", "Bearer " + supervisor)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estadoId\":" + enEvaluacion + "}"))
                .andExpect(status().isOk());
        mvc.perform(put("/api/solicitudes/" + idTecnica + "/asignar")
                        .header("Authorization", "Bearer " + supervisor)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"responsableId\":" + tecnico1Id + "}"))
                .andExpect(status().isOk());

        // Another TEC cannot change state (RN-06 -> 403)
        mvc.perform(put("/api/solicitudes/" + idTecnica + "/estado")
                        .header("Authorization", "Bearer " + tecnico2)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estadoId\":" + enAtencion + "}"))
                .andExpect(status().isForbidden());

        // PA of another area cannot take/change trámite (RN-06 -> 403)
        mvc.perform(put("/api/solicitudes/" + idTramite + "/estado")
                        .header("Authorization", "Bearer " + matricula)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estadoId\":" + enAtencion + "}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void completeTechnicalFlowAssignTransitionPriorityAndEvidence() throws Exception {
        Long id = crearSolicitud(ana, proyector, media);

        // Technicians list filtered by area
        mvc.perform(get("/api/usuarios/tecnicos?areaId=" + soporteAreaId)
                        .header("Authorization", "Bearer " + supervisor))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));

        // Priority update
        mvc.perform(put("/api/solicitudes/" + id + "/prioridad")
                        .header("Authorization", "Bearer " + supervisor)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"prioridadId\":" + alta + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.prioridad").value("Alta"));

        // Invalid transition: Registrada -> Resuelta
        mvc.perform(put("/api/solicitudes/" + id + "/estado")
                        .header("Authorization", "Bearer " + supervisor)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estadoId\":" + resuelta + "}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("TRANSICION_NO_PERMITIDA"));

        // SUP: Registrada -> En evaluación
        mvc.perform(put("/api/solicitudes/" + id + "/estado")
                        .header("Authorization", "Bearer " + supervisor)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estadoId\":" + enEvaluacion + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("En evaluación"));

        // SUP: Asignar to tecnico1 -> Asignada
        mvc.perform(put("/api/solicitudes/" + id + "/asignar")
                        .header("Authorization", "Bearer " + supervisor)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"responsableId\":" + tecnico1Id + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("Asignada"))
                .andExpect(jsonPath("$.responsable.id").value(tecnico1Id));

        // TEC sees it in /asignadas and /transiciones
        mvc.perform(get("/api/solicitudes/asignadas").header("Authorization", "Bearer " + tecnico1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements").value(1));
        mvc.perform(get("/api/solicitudes/" + id + "/transiciones").header("Authorization", "Bearer " + tecnico1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nombre").value("En atención"));

        // TEC: Asignada -> En atención -> Resuelta
        mvc.perform(put("/api/solicitudes/" + id + "/estado")
                        .header("Authorization", "Bearer " + tecnico1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estadoId\":" + enAtencion + "}"))
                .andExpect(status().isOk());
        mvc.perform(put("/api/solicitudes/" + id + "/estado")
                        .header("Authorization", "Bearer " + tecnico1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estadoId\":" + resuelta + ",\"comentario\":\"Proyector reparado\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.respuesta").value("Proyector reparado"));

        // Involved user uploads extra evidence
        mvc.perform(multipart("/api/solicitudes/" + id + "/evidencias")
                        .file(new MockMultipartFile("archivos", "evidencia.png", "image/png", PNG))
                        .header("Authorization", "Bearer " + tecnico1))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$[0].nombreArchivo").value("evidencia.png"));

        // SOL closes request -> fechaCierre is set (RN-15)
        mvc.perform(put("/api/solicitudes/" + id + "/estado")
                        .header("Authorization", "Bearer " + ana)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estadoId\":" + cerrada + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estadoFinal").value(true))
                .andExpect(jsonPath("$.fechaCierre").value(Matchers.notNullValue()));
    }

    @Test
    void bandejaFiltersAndAreaRestrictionWork() throws Exception {
        Long tecnicaVencida = crearSolicitud(ana, proyector, alta);
        Long tramiteSec = crearSolicitud(ana, rectificacionNota, media,
                new MockMultipartFile("archivos", "sustento.pdf", "application/pdf", PDF));
        // Use the application clock (America/Lima), not MySQL NOW(), so the test does not depend on the DB timezone.
        jdbc.update("UPDATE solicitud SET fecha_limite = ? WHERE id = ?",
                LocalDateTime.now(clock).minusHours(2), tecnicaVencida);
        em.clear(); // drop the stale managed entity so the JDBC update is visible

        // SUP sees only technical, filter vencidas=true
        mvc.perform(get("/api/solicitudes?vencidas=true&prioridadId=" + alta)
                        .header("Authorization", "Bearer " + supervisor))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(tecnicaVencida))
                .andExpect(jsonPath("$.content[0].vencida").value(true));

        // PA of Secretaría sees only its area's trámite
        mvc.perform(get("/api/solicitudes").header("Authorization", "Bearer " + secretaria))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(tramiteSec));

        // PA of Matrícula sees nothing
        mvc.perform(get("/api/solicitudes").header("Authorization", "Bearer " + matricula))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements").value(0));

        // Admin sees both
        mvc.perform(get("/api/solicitudes").header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements").value(2));
    }

    @Test
    void rejectionRequiresMotivo() throws Exception {
        Long id = crearSolicitud(ana, proyector, media);
        mvc.perform(put("/api/solicitudes/" + id + "/estado")
                        .header("Authorization", "Bearer " + supervisor)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estadoId\":" + enEvaluacion + "}"))
                .andExpect(status().isOk());

        mvc.perform(put("/api/solicitudes/" + id + "/estado")
                        .header("Authorization", "Bearer " + supervisor)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estadoId\":" + rechazada + "}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("MOTIVO_OBLIGATORIO"));
    }
}
