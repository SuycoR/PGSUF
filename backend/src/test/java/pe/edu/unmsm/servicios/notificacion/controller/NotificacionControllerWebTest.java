package pe.edu.unmsm.servicios.notificacion.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import pe.edu.unmsm.servicios.auth.jwt.JwtAuthenticationFilter;
import pe.edu.unmsm.servicios.auth.jwt.JwtService;
import pe.edu.unmsm.servicios.catalogo.entity.Rol;
import pe.edu.unmsm.servicios.common.BusinessException;
import pe.edu.unmsm.servicios.common.GlobalExceptionHandler;
import pe.edu.unmsm.servicios.common.ProblemDetailAuthHandlers;
import pe.edu.unmsm.servicios.config.AppProperties;
import pe.edu.unmsm.servicios.config.ClockConfig;
import pe.edu.unmsm.servicios.config.SecurityConfig;
import pe.edu.unmsm.servicios.config.WebConfig;
import pe.edu.unmsm.servicios.notificacion.dto.NotificacionDtos.CantidadNoLeidasResponse;
import pe.edu.unmsm.servicios.notificacion.dto.NotificacionDtos.NotificacionResponse;
import pe.edu.unmsm.servicios.notificacion.entity.TipoNotificacion;
import pe.edu.unmsm.servicios.notificacion.service.NotificacionService;
import pe.edu.unmsm.servicios.usuario.entity.Usuario;

@WebMvcTest(controllers = NotificacionController.class)
@ActiveProfiles("test")
@EnableConfigurationProperties(AppProperties.class)
@Import({SecurityConfig.class, JwtService.class, JwtAuthenticationFilter.class, ProblemDetailAuthHandlers.class,
        GlobalExceptionHandler.class, ClockConfig.class, WebConfig.class})
class NotificacionControllerWebTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JwtService jwt;

    @MockitoBean
    private NotificacionService notificacionService;

    private String tokenPara(long id, String correo, String rol) {
        Rol r = org.mockito.Mockito.mock(Rol.class);
        when(r.getNombre()).thenReturn(rol);
        Usuario u = org.mockito.Mockito.mock(Usuario.class);
        when(u.getId()).thenReturn(id);
        when(u.getCorreo()).thenReturn(correo);
        when(u.getRol()).thenReturn(r);
        return jwt.generar(u);
    }

    @Test
    void unauthenticatedRequestsReturn401() throws Exception {
        mvc.perform(get("/api/notificaciones")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/notificaciones/no-leidas/cantidad")).andExpect(status().isUnauthorized());
        mvc.perform(put("/api/notificaciones/1/leida")).andExpect(status().isUnauthorized());
        mvc.perform(put("/api/notificaciones/leer-todas")).andExpect(status().isUnauthorized());
    }

    @Test
    void listsUnreadCountAndMarksRead() throws Exception {
        String token = tokenPara(2L, "tec@unmsm.edu.pe", Rol.TECNICO);
        LocalDateTime fecha = LocalDateTime.of(2026, 10, 20, 11, 0);
        when(notificacionService.listar(eq(2L), eq(false), any()))
                .thenReturn(new PageImpl<>(List.of(new NotificacionResponse(
                        7L, TipoNotificacion.SOLICITUD_ASIGNADA, "Solicitud asignada: SOL-2026-0001",
                        "Se te asignó la solicitud SOL-2026-0001.", 10L, null, false, fecha, null))));
        when(notificacionService.cantidadNoLeidas(2L)).thenReturn(new CantidadNoLeidasResponse(3));

        mvc.perform(get("/api/notificaciones?leida=false").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(7))
                .andExpect(jsonPath("$.content[0].solicitudId").value(10))
                .andExpect(jsonPath("$.content[0].leida").value(false));

        mvc.perform(get("/api/notificaciones/no-leidas/cantidad").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cantidad").value(3));

        mvc.perform(put("/api/notificaciones/7/leida").header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
        verify(notificacionService).marcarLeida(2L, 7L);

        mvc.perform(put("/api/notificaciones/leer-todas").header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
        verify(notificacionService).marcarTodasLeidas(2L);
    }

    @Test
    void markingForeignNotificationReturns403() throws Exception {
        String token = tokenPara(1L, "ana@unmsm.edu.pe", Rol.SOLICITANTE);
        doThrow(BusinessException.forbidden("No tiene permiso para modificar esta notificación."))
                .when(notificacionService).marcarLeida(1L, 99L);

        mvc.perform(put("/api/notificaciones/99/leida").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("ACCESO_DENEGADO"));
    }
}
