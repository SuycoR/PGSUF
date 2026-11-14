package pe.edu.unmsm.servicios.dashboard.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import pe.edu.unmsm.servicios.auth.jwt.JwtAuthenticationFilter;
import pe.edu.unmsm.servicios.auth.jwt.JwtService;
import pe.edu.unmsm.servicios.catalogo.entity.Rol;
import pe.edu.unmsm.servicios.common.GlobalExceptionHandler;
import pe.edu.unmsm.servicios.common.ProblemDetailAuthHandlers;
import pe.edu.unmsm.servicios.config.AppProperties;
import pe.edu.unmsm.servicios.config.ClockConfig;
import pe.edu.unmsm.servicios.config.SecurityConfig;
import pe.edu.unmsm.servicios.config.WebConfig;
import pe.edu.unmsm.servicios.dashboard.dto.DashboardDtos.DashboardResponse;
import pe.edu.unmsm.servicios.dashboard.dto.DashboardDtos.KpiItem;
import pe.edu.unmsm.servicios.dashboard.service.DashboardService;
import pe.edu.unmsm.servicios.usuario.entity.Usuario;

@WebMvcTest(controllers = DashboardController.class)
@ActiveProfiles("test")
@EnableConfigurationProperties(AppProperties.class)
@Import({SecurityConfig.class, JwtService.class, JwtAuthenticationFilter.class, ProblemDetailAuthHandlers.class,
        GlobalExceptionHandler.class, ClockConfig.class, WebConfig.class})
class DashboardControllerWebTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JwtService jwt;

    @MockitoBean
    private DashboardService service;

    private String token(Long id, String correo, String rolNombre) {
        Rol rol = mock(Rol.class);
        when(rol.getNombre()).thenReturn(rolNombre);
        Usuario u = mock(Usuario.class);
        when(u.getId()).thenReturn(id);
        when(u.getCorreo()).thenReturn(correo);
        when(u.getRol()).thenReturn(rol);
        return jwt.generar(u);
    }

    @Test
    @DisplayName("GET /api/dashboard responde 403 Forbidden para rol Solicitante")
    void denegadoParaSolicitante() throws Exception {
        mvc.perform(get("/api/dashboard")
                        .header("Authorization", "Bearer " + token(10L, "sol@unmsm.edu.pe", "SOLICITANTE")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/dashboard responde 200 OK con indicadores para rol Supervisor")
    void permitidoParaSupervisor() throws Exception {
        DashboardResponse mockResp = new DashboardResponse(
                30, 10, 20, 2, 66.7, 5.2,
                List.of(new KpiItem(1L, "Soporte técnico", 20)),
                List.of(new KpiItem(1L, "Alta", 15)),
                List.of(new KpiItem(100L, "Técnico Juan", 20))
        );
        when(service.obtenerIndicadores(any(), any())).thenReturn(mockResp);

        mvc.perform(get("/api/dashboard?desde=2026-10-01&hasta=2026-10-31")
                        .header("Authorization", "Bearer " + token(20L, "sup@unmsm.edu.pe", "SUPERVISOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRegistradas").value(30))
                .andExpect(jsonPath("$.totalVencidas").value(2))
                .andExpect(jsonPath("$.porcentajeResueltas").value(66.7))
                .andExpect(jsonPath("$.tiempoPromedioAtencionHoras").value(5.2))
                .andExpect(jsonPath("$.porCategoria[0].nombre").value("Soporte técnico"));
    }
}
