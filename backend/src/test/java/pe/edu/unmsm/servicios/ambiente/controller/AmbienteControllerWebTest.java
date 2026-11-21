package pe.edu.unmsm.servicios.ambiente.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.LocalTime;
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

import pe.edu.unmsm.servicios.ambiente.dto.AmbienteDtos.AmbienteResponse;
import pe.edu.unmsm.servicios.ambiente.dto.AmbienteDtos.DisponibilidadResponse;
import pe.edu.unmsm.servicios.ambiente.dto.AmbienteDtos.IntervaloHorario;
import pe.edu.unmsm.servicios.ambiente.entity.Pabellon;
import pe.edu.unmsm.servicios.ambiente.entity.TipoAmbiente;
import pe.edu.unmsm.servicios.ambiente.service.AmbienteService;
import pe.edu.unmsm.servicios.auth.jwt.JwtAuthenticationFilter;
import pe.edu.unmsm.servicios.auth.jwt.JwtService;
import pe.edu.unmsm.servicios.catalogo.entity.Rol;
import pe.edu.unmsm.servicios.common.GlobalExceptionHandler;
import pe.edu.unmsm.servicios.common.ProblemDetailAuthHandlers;
import pe.edu.unmsm.servicios.config.AppProperties;
import pe.edu.unmsm.servicios.config.ClockConfig;
import pe.edu.unmsm.servicios.config.SecurityConfig;
import pe.edu.unmsm.servicios.config.WebConfig;
import pe.edu.unmsm.servicios.usuario.entity.Usuario;

@WebMvcTest(controllers = AmbienteController.class)
@ActiveProfiles("test")
@EnableConfigurationProperties(AppProperties.class)
@Import({SecurityConfig.class, JwtService.class, JwtAuthenticationFilter.class, ProblemDetailAuthHandlers.class,
        GlobalExceptionHandler.class, ClockConfig.class, WebConfig.class})
class AmbienteControllerWebTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JwtService jwt;

    @MockitoBean
    private AmbienteService service;

    private String token(String rolNombre) {
        Rol rol = mock(Rol.class);
        when(rol.getNombre()).thenReturn(rolNombre);
        Usuario u = mock(Usuario.class);
        when(u.getId()).thenReturn(1L);
        when(u.getCorreo()).thenReturn("user@unmsm.edu.pe");
        when(u.getRol()).thenReturn(rol);
        return jwt.generar(u);
    }

    @Test
    @DisplayName("GET /api/ambientes devuelve lista 200 para cualquier usuario autenticado")
    void listaAmbientes() throws Exception {
        when(service.listar(any(), any(), any())).thenReturn(List.of(
                new AmbienteResponse(1, "Auditorio", TipoAmbiente.AUDITORIO, Pabellon.CENTRAL, null, 300, true)
        ));

        mvc.perform(get("/api/ambientes")
                        .header("Authorization", "Bearer " + token("SOLICITANTE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nombre").value("Auditorio"))
                .andExpect(jsonPath("$[0].capacidad").value(300));
    }

    @Test
    @DisplayName("GET /api/ambientes/{id}/disponibilidad devuelve intervalos ocupados")
    void consultaDisponibilidad() throws Exception {
        LocalDate fecha = LocalDate.of(2026, 10, 24);
        when(service.obtenerDisponibilidad(eq(1), eq(fecha))).thenReturn(
                new DisponibilidadResponse(1, "Auditorio", fecha, List.of(
                        new IntervaloHorario(LocalTime.of(10, 0), LocalTime.of(12, 0), "RES-2026-0001")
                ))
        );

        mvc.perform(get("/api/ambientes/1/disponibilidad?fecha=2026-10-24")
                        .header("Authorization", "Bearer " + token("SOLICITANTE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ambienteNombre").value("Auditorio"))
                .andExpect(jsonPath("$.reservasAprobadas[0].codigo").value("RES-2026-0001"));
    }
}
