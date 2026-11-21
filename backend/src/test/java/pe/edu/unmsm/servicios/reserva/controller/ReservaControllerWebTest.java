package pe.edu.unmsm.servicios.reserva.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import pe.edu.unmsm.servicios.ambiente.entity.Pabellon;
import pe.edu.unmsm.servicios.ambiente.entity.TipoAmbiente;
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
import pe.edu.unmsm.servicios.reserva.dto.ReservaDtos.ReservaAmbienteResumen;
import pe.edu.unmsm.servicios.reserva.dto.ReservaDtos.ReservaEstadoResumen;
import pe.edu.unmsm.servicios.reserva.dto.ReservaDtos.ReservaResponse;
import pe.edu.unmsm.servicios.reserva.dto.ReservaDtos.ReservaResumen;
import pe.edu.unmsm.servicios.reserva.dto.ReservaDtos.ReservaUsuarioResumen;
import pe.edu.unmsm.servicios.reserva.service.ReservaService;
import pe.edu.unmsm.servicios.usuario.entity.Usuario;

@WebMvcTest(controllers = ReservaController.class)
@ActiveProfiles("test")
@EnableConfigurationProperties(AppProperties.class)
@Import({SecurityConfig.class, JwtService.class, JwtAuthenticationFilter.class, ProblemDetailAuthHandlers.class,
        GlobalExceptionHandler.class, ClockConfig.class, WebConfig.class})
class ReservaControllerWebTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JwtService jwt;

    @MockitoBean
    private ReservaService service;

    private String token(Long id, String correo, String rolNombre) {
        Rol rol = mock(Rol.class);
        when(rol.getNombre()).thenReturn(rolNombre);
        Usuario u = mock(Usuario.class);
        when(u.getId()).thenReturn(id);
        when(u.getCorreo()).thenReturn(correo);
        when(u.getRol()).thenReturn(rol);
        return jwt.generar(u);
    }

    private ReservaResponse reservaEjemplo(Long id, String codigo, String estado) {
        return new ReservaResponse(
                id,
                codigo,
                new ReservaAmbienteResumen(1, "Auditorio", TipoAmbiente.AUDITORIO, Pabellon.CENTRAL, null),
                new ReservaUsuarioResumen(10L, "Ana Solicitante", "ana@unmsm.edu.pe"),
                null,
                new ReservaEstadoResumen(1, estado),
                LocalDate.of(2026, 10, 25),
                LocalTime.of(10, 0),
                LocalTime.of(12, 0),
                "Conferencia de prueba",
                null,
                LocalDateTime.of(2026, 10, 20, 9, 0),
                null
        );
    }

    @Test
    @DisplayName("POST /api/reservas registra con 201 Created y Location header")
    void registraReserva() throws Exception {
        when(service.crear(eq(10L), any())).thenReturn(reservaEjemplo(15L, "RES-2026-0015", "Registrada"));

        String body = """
                {
                  "ambienteId": 1,
                  "fecha": "2026-10-25",
                  "horaInicio": "10:00",
                  "horaFin": "12:00",
                  "motivo": "Conferencia de prueba"
                }
                """;

        mvc.perform(post("/api/reservas")
                        .header("Authorization", "Bearer " + token(10L, "ana@unmsm.edu.pe", "SOLICITANTE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/reservas/15"))
                .andExpect(jsonPath("$.codigo").value("RES-2026-0015"))
                .andExpect(jsonPath("$.estado.nombre").value("Registrada"));
    }

    @Test
    @DisplayName("POST /api/reservas devuelve 409 Conflict ante cruce de horario (RN-04)")
    void cruceDevuelve409() throws Exception {
        when(service.crear(eq(10L), any()))
                .thenThrow(BusinessException.conflict("CRUCE_HORARIO", "Cruce de horario",
                        "El ambiente ya está reservado en ese horario."));

        String body = """
                {
                  "ambienteId": 1,
                  "fecha": "2026-10-25",
                  "horaInicio": "10:00",
                  "horaFin": "12:00",
                  "motivo": "Conferencia cruzada"
                }
                """;

        mvc.perform(post("/api/reservas")
                        .header("Authorization", "Bearer " + token(10L, "ana@unmsm.edu.pe", "SOLICITANTE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("CRUCE_HORARIO"))
                .andExpect(jsonPath("$.detail").value("El ambiente ya está reservado en ese horario."));
    }

    @Test
    @DisplayName("GET /api/reservas responde 403 Forbidden para Solicitante")
    void bandejaDenegadaParaSolicitante() throws Exception {
        mvc.perform(get("/api/reservas")
                        .header("Authorization", "Bearer " + token(10L, "ana@unmsm.edu.pe", "SOLICITANTE")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/reservas responde 200 OK para Supervisor")
    void bandejaPermitidaParaSupervisor() throws Exception {
        when(service.listarBandeja(any(), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(
                        new ReservaResumen(15L, "RES-2026-0015", 1, "Auditorio", 10L, "Ana", 1, "Registrada",
                                LocalDate.of(2026, 10, 25), LocalTime.of(10, 0), LocalTime.of(12, 0),
                                "Motivo", null, LocalDateTime.now(), null)
                )));

        mvc.perform(get("/api/reservas")
                        .header("Authorization", "Bearer " + token(20L, "sup@unmsm.edu.pe", "SUPERVISOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].codigo").value("RES-2026-0015"));
    }

    @Test
    @DisplayName("PUT /api/reservas/{id}/aprobar responde 200 para Supervisor")
    void aprobarPermitidoParaSupervisor() throws Exception {
        when(service.aprobar(eq(15L), eq(20L))).thenReturn(reservaEjemplo(15L, "RES-2026-0015", "Aprobada"));

        mvc.perform(put("/api/reservas/15/aprobar")
                        .header("Authorization", "Bearer " + token(20L, "sup@unmsm.edu.pe", "SUPERVISOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado.nombre").value("Aprobada"));
    }

    @Test
    @DisplayName("PUT /api/reservas/{id}/rechazar valida motivo obligatorio (400 si está en blanco)")
    void rechazarValidaMotivoObligatorio() throws Exception {
        mvc.perform(put("/api/reservas/15/rechazar")
                        .header("Authorization", "Bearer " + token(20L, "sup@unmsm.edu.pe", "SUPERVISOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"motivo\": \"   \"}"))
                .andExpect(status().isBadRequest());
    }
}
