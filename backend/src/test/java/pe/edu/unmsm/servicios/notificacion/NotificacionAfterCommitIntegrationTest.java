package pe.edu.unmsm.servicios.notificacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;

import com.fasterxml.jackson.databind.ObjectMapper;

import pe.edu.unmsm.servicios.auth.jwt.JwtService;
import pe.edu.unmsm.servicios.usuario.entity.Usuario;
import pe.edu.unmsm.servicios.usuario.service.UsuarioService;

/**
 * Verifies RN-16 end-to-end with real transaction commits so @TransactionalEventListener(AFTER_COMMIT)
 * and @Transactional(REQUIRES_NEW) execute against MySQL.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class NotificacionAfterCommitIntegrationTest {

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

    private final List<Long> solicitudesCreadas = new ArrayList<>();
    private final List<Long> usuariosCreados = new ArrayList<>();

    @AfterEach
    void limpiar() {
        for (Long solicitudId : solicitudesCreadas) {
            jdbc.update("DELETE FROM notificacion WHERE solicitud_id = ?", solicitudId);
            jdbc.update("DELETE FROM comentario WHERE solicitud_id = ?", solicitudId);
            jdbc.update("DELETE FROM historial_estado WHERE solicitud_id = ?", solicitudId);
            jdbc.update("DELETE FROM evidencia WHERE solicitud_id = ?", solicitudId);
            jdbc.update("DELETE FROM solicitud WHERE id = ?", solicitudId);
        }
        for (Long usuarioId : usuariosCreados) {
            jdbc.update("DELETE FROM notificacion WHERE usuario_id = ?", usuarioId);
            jdbc.update("DELETE FROM usuario WHERE id = ?", usuarioId);
        }
        solicitudesCreadas.clear();
        usuariosCreados.clear();
    }

    private Usuario crearUsuario(String nombre, String correo, String rol, Integer areaId) {
        Usuario u = usuarioService.crear(nombre, correo, "Clave12345", rol, areaId);
        usuariosCreados.add(u.getId());
        return u;
    }

    @Test
    void afterCommitListenerCreatesNotificationsForAssignStateChangeAndComment() throws Exception {
        int soporteAreaId = jdbc.queryForObject("SELECT id FROM area WHERE nombre = 'Soporte técnico'", Integer.class);
        int matAreaId = jdbc.queryForObject("SELECT id FROM area WHERE nombre = 'Oficina de matrícula'", Integer.class);
        int proyector = jdbc.queryForObject("SELECT id FROM tipo_solicitud WHERE nombre LIKE 'Equipo audiovisual%'", Integer.class);
        int media = jdbc.queryForObject("SELECT id FROM prioridad WHERE nombre = 'Media'", Integer.class);
        int enEvaluacion = jdbc.queryForObject("SELECT id FROM estado WHERE nombre = 'En evaluación'", Integer.class);
        int enAtencion = jdbc.queryForObject("SELECT id FROM estado WHERE nombre = 'En atención'", Integer.class);

        Usuario ana = crearUsuario("Ana", "ana.commit@unmsm.edu.pe", "SOLICITANTE", null);
        Usuario sup = crearUsuario("Sup", "sup.commit@unmsm.edu.pe", "SUPERVISOR", null);
        Usuario tec = crearUsuario("Luis Tec", "tec.commit@unmsm.edu.pe", "TECNICO", soporteAreaId);
        Usuario otroTec = crearUsuario("Otro Tec", "otrotec.commit@unmsm.edu.pe", "TECNICO", matAreaId);

        String tokenAna = jwt.generar(ana);
        String tokenSup = jwt.generar(sup);
        String tokenTec = jwt.generar(tec);

        // 1. Create request
        MockMultipartHttpServletRequestBuilder req = multipart("/api/solicitudes");
        req.file(new MockMultipartFile("datos", "", MediaType.APPLICATION_JSON_VALUE,
                ("{\"tipoSolicitudId\":" + proyector + ",\"prioridadId\":" + media
                        + ",\"descripcion\":\"Proyector sin imagen\",\"ubicacion\":\"A-101\"}").getBytes()));
        String createdBody = mvc.perform(req.header("Authorization", "Bearer " + tokenAna))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long solicitudId = json.readTree(createdBody).get("id").asLong();
        solicitudesCreadas.add(solicitudId);

        // 2. Supervisor moves to En evaluación -> notifies Ana
        mvc.perform(put("/api/solicitudes/" + solicitudId + "/estado")
                        .header("Authorization", "Bearer " + tokenSup)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estadoId\":" + enEvaluacion + "}"))
                .andExpect(status().isOk());

        // 3. Failed assignment (wrong area -> 422) must NOT create notifications
        mvc.perform(put("/api/solicitudes/" + solicitudId + "/asignar")
                        .header("Authorization", "Bearer " + tokenSup)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"responsableId\":" + otroTec.getId() + "}"))
                .andExpect(status().isUnprocessableEntity());
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM notificacion WHERE usuario_id = ?",
                Integer.class, otroTec.getId())).isZero();

        // 4. Valid assignment -> notifies both tecnico and Ana
        mvc.perform(put("/api/solicitudes/" + solicitudId + "/asignar")
                        .header("Authorization", "Bearer " + tokenSup)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"responsableId\":" + tec.getId() + "}"))
                .andExpect(status().isOk());

        // 5. Ana posts a comment -> notifies tecnico (not Ana)
        mvc.perform(post("/api/solicitudes/" + solicitudId + "/comentarios")
                        .header("Authorization", "Bearer " + tokenAna)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"texto\":\"Estaré en el salón hasta las 5 pm\"}"))
                .andExpect(status().isCreated());

        // 6. Technician changes state -> notifies Ana (not tecnico)
        mvc.perform(put("/api/solicitudes/" + solicitudId + "/estado")
                        .header("Authorization", "Bearer " + tokenTec)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estadoId\":" + enAtencion + "}"))
                .andExpect(status().isOk());

        // Verify counts:
        // Ana received: ESTADO_CAMBIADO (Sup) + SOLICITUD_ASIGNADA + ESTADO_CAMBIADO (Tec) = 3
        // Tec received: SOLICITUD_ASIGNADA + COMENTARIO_NUEVO (Ana) = 2
        // Sup received: 0
        mvc.perform(get("/api/notificaciones/no-leidas/cantidad").header("Authorization", "Bearer " + tokenAna))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cantidad").value(3));

        mvc.perform(get("/api/notificaciones/no-leidas/cantidad").header("Authorization", "Bearer " + tokenTec))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cantidad").value(2));

        mvc.perform(get("/api/notificaciones/no-leidas/cantidad").header("Authorization", "Bearer " + tokenSup))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cantidad").value(0));

        // Verify notification payload includes solicitudId
        mvc.perform(get("/api/notificaciones").header("Authorization", "Bearer " + tokenTec))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].solicitudId").value(solicitudId));
    }
}
