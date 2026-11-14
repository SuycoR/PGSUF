package pe.edu.unmsm.servicios.notificacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import pe.edu.unmsm.servicios.IntegrationTest;
import pe.edu.unmsm.servicios.auth.jwt.JwtService;
import pe.edu.unmsm.servicios.notificacion.entity.Notificacion;
import pe.edu.unmsm.servicios.notificacion.entity.TipoNotificacion;
import pe.edu.unmsm.servicios.notificacion.repository.NotificacionRepository;
import pe.edu.unmsm.servicios.usuario.entity.Usuario;
import pe.edu.unmsm.servicios.usuario.service.UsuarioService;

@IntegrationTest
class NotificacionIntegrationTest {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private UsuarioService usuarioService;
    @Autowired
    private JwtService jwt;
    @Autowired
    private NotificacionRepository notificaciones;

    private Usuario ana;
    private Usuario beto;
    private String tokenAna;
    private String tokenBeto;

    @BeforeEach
    void setUp() {
        ana = usuarioService.crear("Ana", "ana.noti@unmsm.edu.pe", "Clave12345", "SOLICITANTE", null);
        beto = usuarioService.crear("Beto", "beto.noti@unmsm.edu.pe", "Clave12345", "SOLICITANTE", null);
        tokenAna = jwt.generar(ana);
        tokenBeto = jwt.generar(beto);
    }

    @Test
    void listsOwnNotificationsFilteredNewestFirstAndCountsUnread() throws Exception {
        LocalDateTime base = LocalDateTime.of(2026, 10, 20, 9, 0);
        Notificacion antigua = notificaciones.save(Notificacion.paraSolicitud(
                ana, TipoNotificacion.SOLICITUD_ASIGNADA, "Primera", "Mensaje 1", null, base));
        Notificacion leida = Notificacion.paraSolicitud(
                ana, TipoNotificacion.ESTADO_CAMBIADO, "Segunda", "Mensaje 2", null, base.plusMinutes(5));
        leida.marcarLeida(base.plusMinutes(6));
        notificaciones.save(leida);
        Notificacion reciente = notificaciones.save(Notificacion.paraSolicitud(
                ana, TipoNotificacion.COMENTARIO_NUEVO, "Tercera", "Mensaje 3", null, base.plusMinutes(10)));
        notificaciones.save(Notificacion.paraSolicitud(
                beto, TipoNotificacion.COMENTARIO_NUEVO, "Ajena", "No debe verse", null, base.plusMinutes(15)));

        // Unauthenticated -> 401
        mvc.perform(get("/api/notificaciones")).andExpect(status().isUnauthorized());

        // Newest first, only Ana's notifications
        mvc.perform(get("/api/notificaciones").header("Authorization", "Bearer " + tokenAna))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements").value(3))
                .andExpect(jsonPath("$.content[0].id").value(reciente.getId()))
                .andExpect(jsonPath("$.content[2].id").value(antigua.getId()));

        // Filter leida=false
        mvc.perform(get("/api/notificaciones?leida=false").header("Authorization", "Bearer " + tokenAna))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements").value(2));

        // Filter leida=true
        mvc.perform(get("/api/notificaciones?leida=true").header("Authorization", "Bearer " + tokenAna))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].titulo").value("Segunda"));

        // Unread count
        mvc.perform(get("/api/notificaciones/no-leidas/cantidad").header("Authorization", "Bearer " + tokenAna))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cantidad").value(2));
    }

    @Test
    void markOneIsOwnerOnlyAndMarkAllUpdatesOnlyCurrentUser() throws Exception {
        LocalDateTime base = LocalDateTime.of(2026, 10, 20, 10, 0);
        Notificacion n1 = notificaciones.save(Notificacion.paraSolicitud(
                ana, TipoNotificacion.ESTADO_CAMBIADO, "N1", "M1", null, base));
        Notificacion n2 = notificaciones.save(Notificacion.paraSolicitud(
                ana, TipoNotificacion.COMENTARIO_NUEVO, "N2", "M2", null, base.plusMinutes(1)));
        Notificacion ajena = notificaciones.save(Notificacion.paraSolicitud(
                beto, TipoNotificacion.COMENTARIO_NUEVO, "Ajena", "M3", null, base.plusMinutes(2)));

        // Beto cannot mark Ana's notification -> 403; unknown id -> 404
        mvc.perform(put("/api/notificaciones/" + n1.getId() + "/leida")
                        .header("Authorization", "Bearer " + tokenBeto))
                .andExpect(status().isForbidden());
        mvc.perform(put("/api/notificaciones/999999/leida")
                        .header("Authorization", "Bearer " + tokenAna))
                .andExpect(status().isNotFound());

        // Ana marks one
        mvc.perform(put("/api/notificaciones/" + n1.getId() + "/leida")
                        .header("Authorization", "Bearer " + tokenAna))
                .andExpect(status().isNoContent());

        mvc.perform(get("/api/notificaciones/no-leidas/cantidad").header("Authorization", "Bearer " + tokenAna))
                .andExpect(jsonPath("$.cantidad").value(1));

        // Ana marks all -> Ana has 0, Beto still has 1 unread
        mvc.perform(put("/api/notificaciones/leer-todas")
                        .header("Authorization", "Bearer " + tokenAna))
                .andExpect(status().isNoContent());

        mvc.perform(get("/api/notificaciones/no-leidas/cantidad").header("Authorization", "Bearer " + tokenAna))
                .andExpect(jsonPath("$.cantidad").value(0));
        assertThat(notificaciones.countByUsuarioIdAndLeidaFalse(beto.getId())).isEqualTo(1);
        assertThat(notificaciones.findById(ajena.getId())).get().satisfies(n -> assertThat(n.isLeida()).isFalse());
    }
}
