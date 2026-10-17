package pe.edu.unmsm.servicios.solicitud;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.file.Files;
import java.nio.file.Path;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;

import com.fasterxml.jackson.databind.ObjectMapper;

import pe.edu.unmsm.servicios.IntegrationTest;
import pe.edu.unmsm.servicios.auth.jwt.JwtService;
import pe.edu.unmsm.servicios.usuario.service.UsuarioService;

@IntegrationTest
class SolicitudIntegrationTest {

    private static final byte[] PDF = "%PDF-1.4\n%sustento".getBytes();
    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 13};
    private static final byte[] EXE = {'M', 'Z', (byte) 0x90, 0, 3, 0, 0, 0};

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
    private String secretaria;
    private int proyector;
    private int rectificacionNota;
    private int media;

    @BeforeEach
    void setUp() {
        ana = jwt.generar(usuarioService.crear("Ana", "ana@unmsm.edu.pe", "Clave12345", "SOLICITANTE", null));
        beto = jwt.generar(usuarioService.crear("Beto", "beto@unmsm.edu.pe", "Clave12345", "SOLICITANTE", null));
        supervisor = jwt.generar(usuarioService.crear("Sup", "sup@unmsm.edu.pe", "Clave12345", "SUPERVISOR", null));
        int secretariaAcademica = jdbc.queryForObject("SELECT id FROM area WHERE nombre = 'Secretaría académica'", Integer.class);
        secretaria = jwt.generar(usuarioService.crear("Sec", "sec@unmsm.edu.pe", "Clave12345", "PERSONAL_AREA", secretariaAcademica));
        proyector = jdbc.queryForObject("SELECT id FROM tipo_solicitud WHERE nombre LIKE 'Equipo audiovisual%'", Integer.class);
        rectificacionNota = jdbc.queryForObject("SELECT id FROM tipo_solicitud WHERE nombre = 'Rectificación de nota'", Integer.class);
        media = jdbc.queryForObject("SELECT id FROM prioridad WHERE nombre = 'Media'", Integer.class);
    }

    private ResultActions registrar(String token, int tipoId, MockMultipartFile... archivos) throws Exception {
        String datos = json.writeValueAsString(new Datos(tipoId, media, "El proyector no enciende", "Pabellón Antiguo, Salón A-204"));
        MockMultipartHttpServletRequestBuilder request = multipart("/api/solicitudes");
        request.file(new MockMultipartFile("datos", "", MediaType.APPLICATION_JSON_VALUE, datos.getBytes()));
        for (MockMultipartFile archivo : archivos) {
            request.file(archivo);
        }
        return mvc.perform(request.header("Authorization", "Bearer " + token));
    }

    private Long registrarId(String token, int tipoId, MockMultipartFile... archivos) throws Exception {
        String body = registrar(token, tipoId, archivos).andReturn().getResponse().getContentAsString();
        return json.readTree(body).get("id").asLong();
    }

    private static MockMultipartFile archivo(String nombre, byte[] contenido) {
        return new MockMultipartFile("archivos", nombre, "application/octet-stream", contenido);
    }

    private ResultActions getCon(String token, String url) throws Exception {
        return mvc.perform(get(url).header("Authorization", "Bearer " + token));
    }

    record Datos(Integer tipoSolicitudId, Integer prioridadId, String descripcion, String ubicacion) {
    }

    @Test
    void hu04Scenario1RegistersATechnicalRequestWithCodeAndDeadline() throws Exception {
        registrar(ana, proyector)
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", Matchers.startsWith("/api/solicitudes/")))
                .andExpect(jsonPath("$.codigo").value(Matchers.matchesPattern("SOL-\\d{4}-\\d{4,}")))
                .andExpect(jsonPath("$.estado").value("Registrada"))
                // whole seconds, exactly as the DATETIME column stores it
                .andExpect(jsonPath("$.fechaRegistro").value(Matchers.matchesPattern("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}(:\\d{2})?")));
    }

    @Test
    void hu04TheHistoryStartsWithTheRegistration() throws Exception {
        Long id = registrarId(ana, proyector);

        getCon(ana, "/api/solicitudes/" + id + "/historial")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].estadoAnterior").doesNotExist())
                .andExpect(jsonPath("$[0].estadoNuevo").value("Registrada"))
                .andExpect(jsonPath("$[0].usuario").value("Ana"));
    }

    @Test
    void hu04Scenario2MissingFieldsAreRejected() throws Exception {
        MockMultipartHttpServletRequestBuilder request = multipart("/api/solicitudes");
        request.file(new MockMultipartFile("datos", "", MediaType.APPLICATION_JSON_VALUE,
                "{\"tipoSolicitudId\":null,\"descripcion\":\"\"}".getBytes()));

        mvc.perform(request.header("Authorization", "Bearer " + ana))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("DATOS_INVALIDOS"));
    }

    @Test
    void hu15Scenario1AProcedureIsRoutedToItsAreaAutomatically() throws Exception {
        Long id = registrarId(ana, rectificacionNota, archivo("sustento.pdf", PDF));

        getCon(ana, "/api/solicitudes/" + id)
                .andExpect(jsonPath("$.estado").value("En evaluación"))
                .andExpect(jsonPath("$.area").value("Secretaría académica"));
        getCon(ana, "/api/solicitudes/" + id + "/historial")
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[1].estadoNuevo").value("En evaluación"))
                .andExpect(jsonPath("$[1].usuario").value("Sistema"));
    }

    @Test
    void hu15Scenario2AProcedureWithoutSupportingDocumentIsRejected() throws Exception {
        registrar(ana, rectificacionNota)
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("ADJUNTO_OBLIGATORIO"));
    }

    @Test
    void hu15Scenario3AnExecutableRenamedAsPdfIsRejected() throws Exception {
        registrar(ana, proyector, archivo("factura.pdf", EXE))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("ARCHIVO_INVALIDO"));
    }

    @Test
    void moreThanFiveFilesAreRejected() throws Exception {
        MockMultipartFile[] seis = new MockMultipartFile[6];
        for (int i = 0; i < 6; i++) {
            seis[i] = archivo("foto" + i + ".png", PNG);
        }

        registrar(ana, proyector, seis)
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("DEMASIADOS_ARCHIVOS"));
    }

    @Test
    void evidenceIsListedAndDownloadedAsAnAttachment() throws Exception {
        Long id = registrarId(ana, proyector, archivo("C:\\fotos\\proyector.png", PNG));

        String lista = getCon(ana, "/api/solicitudes/" + id + "/evidencias")
                .andExpect(jsonPath("$[0].nombreArchivo").value("proyector.png"))
                .andExpect(jsonPath("$[0].tipoMime").value("image/png"))
                .andReturn().getResponse().getContentAsString();
        long evidenciaId = json.readTree(lista).get(0).get("id").asLong();

        getCon(ana, "/api/evidencias/" + evidenciaId + "/descarga")
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", Matchers.startsWith("attachment")))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(content().bytes(PNG));
        getCon(beto, "/api/evidencias/" + evidenciaId + "/descarga").andExpect(status().isNotFound());
    }

    @Test
    void aRejectedRegistrationLeavesNoFilesOnDisk() throws Exception {
        long antes = contarArchivos();

        // the first file is valid, the second is not: nothing must be written
        registrar(ana, proyector, archivo("ok.png", PNG), archivo("malo.pdf", EXE))
                .andExpect(status().isUnprocessableEntity());

        org.assertj.core.api.Assertions.assertThat(contarArchivos()).isEqualTo(antes);
    }

    @Test
    void hu05Scenario3TheListShowsOnlyMyRequests() throws Exception {
        registrar(ana, proyector);
        registrar(ana, proyector);
        registrar(beto, proyector);

        getCon(ana, "/api/solicitudes/mis-solicitudes")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements").value(2))
                .andExpect(jsonPath("$.content[0].estado").value("Registrada"))
                .andExpect(jsonPath("$.content[0].vencida").value(false));
    }

    @Test
    void hu05Scenario2AnEmptyListForANewUser() throws Exception {
        getCon(beto, "/api/solicitudes/mis-solicitudes")
                .andExpect(jsonPath("$.page.totalElements").value(0));
    }

    @Test
    void hu06Scenario3AnotherRequesterGetsNotFound() throws Exception {
        Long id = registrarId(ana, proyector);

        getCon(beto, "/api/solicitudes/" + id)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("NO_ENCONTRADO"));
    }

    @Test
    void rn20SupervisorSeesTechnicalButNotProcedures() throws Exception {
        Long tecnica = registrarId(ana, proyector);
        Long tramite = registrarId(ana, rectificacionNota, archivo("sustento.pdf", PDF));

        getCon(supervisor, "/api/solicitudes/" + tecnica).andExpect(status().isOk());
        getCon(supervisor, "/api/solicitudes/" + tramite).andExpect(status().isNotFound());
        getCon(secretaria, "/api/solicitudes/" + tramite).andExpect(status().isOk());
        getCon(secretaria, "/api/solicitudes/" + tecnica).andExpect(status().isNotFound());
    }

    @Test
    void hu06Scenario2AnUnknownIdIsNotFound() throws Exception {
        getCon(ana, "/api/solicitudes/999999").andExpect(status().isNotFound());
    }

    @Test
    void catalogsListTypesOfACategoryWithTheAttachmentRule() throws Exception {
        int rectificaciones = jdbc.queryForObject("SELECT id FROM categoria WHERE nombre = 'Rectificaciones'", Integer.class);

        getCon(ana, "/api/catalogos/tipos-solicitud?categoriaId=" + rectificaciones)
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].requiereAdjunto").value(true));
        getCon(ana, "/api/catalogos/estados")
                .andExpect(jsonPath("$[?(@.nombre == 'Aprobada')]").isEmpty());
    }

    private static long contarArchivos() throws Exception {
        Path dir = Path.of("target/test-uploads");
        if (!Files.exists(dir)) {
            return 0;
        }
        try (var archivos = Files.walk(dir)) {
            return archivos.filter(Files::isRegularFile).count();
        }
    }
}
