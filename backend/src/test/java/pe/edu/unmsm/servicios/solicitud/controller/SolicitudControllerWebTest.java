package pe.edu.unmsm.servicios.solicitud.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import pe.edu.unmsm.servicios.auth.jwt.JwtAuthenticationFilter;
import pe.edu.unmsm.servicios.auth.jwt.JwtService;
import pe.edu.unmsm.servicios.catalogo.entity.Area;
import pe.edu.unmsm.servicios.catalogo.entity.Rol;
import pe.edu.unmsm.servicios.common.BusinessException;
import pe.edu.unmsm.servicios.common.GlobalExceptionHandler;
import pe.edu.unmsm.servicios.common.ProblemDetailAuthHandlers;
import pe.edu.unmsm.servicios.config.AppProperties;
import pe.edu.unmsm.servicios.config.ClockConfig;
import pe.edu.unmsm.servicios.config.SecurityConfig;
import pe.edu.unmsm.servicios.config.WebConfig;
import pe.edu.unmsm.servicios.solicitud.dto.SolicitudDtos.PersonaResumen;
import pe.edu.unmsm.servicios.solicitud.dto.SolicitudDtos.SolicitudDetalle;
import pe.edu.unmsm.servicios.solicitud.dto.SolicitudDtos.TransicionResponse;
import pe.edu.unmsm.servicios.solicitud.service.SolicitudService;
import pe.edu.unmsm.servicios.usuario.controller.UsuarioController;
import pe.edu.unmsm.servicios.usuario.entity.Usuario;
import pe.edu.unmsm.servicios.usuario.repository.UsuarioRepository;

@WebMvcTest(controllers = {SolicitudController.class, UsuarioController.class})
@ActiveProfiles("test")
@EnableConfigurationProperties(AppProperties.class)
@Import({SecurityConfig.class, JwtService.class, JwtAuthenticationFilter.class, ProblemDetailAuthHandlers.class,
        GlobalExceptionHandler.class, ClockConfig.class, WebConfig.class})
class SolicitudControllerWebTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JwtService jwt;

    @MockitoBean
    private SolicitudService solicitudService;

    @MockitoBean
    private UsuarioRepository usuarioRepository;

    private String tokenPara(long id, String correo, String rol) {
        Rol r = org.mockito.Mockito.mock(Rol.class);
        when(r.getNombre()).thenReturn(rol);
        Usuario u = org.mockito.Mockito.mock(Usuario.class);
        when(u.getId()).thenReturn(id);
        when(u.getCorreo()).thenReturn(correo);
        when(u.getRol()).thenReturn(r);
        return jwt.generar(u);
    }

    private static SolicitudDetalle detalleEjemplo(String estado) {
        LocalDateTime ahora = LocalDateTime.of(2026, 10, 20, 10, 0);
        return new SolicitudDetalle(1L, "SOL-2026-0001", "Soporte técnico e infraestructura", "Proyector",
                "Soporte técnico", 1, false, "Alta", 1, estado, false, "No enciende", "A-101", null,
                new PersonaResumen(1L, "Ana"), new PersonaResumen(3L, "Luis"), ahora, ahora.plusHours(24), null, false);
    }

    @Test
    void unauthenticatedRequestsReturn401() throws Exception {
        mvc.perform(get("/api/solicitudes"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("NO_AUTENTICADO"));
        mvc.perform(get("/api/solicitudes/asignadas"))
                .andExpect(status().isUnauthorized());
        mvc.perform(put("/api/solicitudes/1/asignar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"responsableId\":2}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void roleGuardsReturn403ForWrongRoles() throws Exception {
        String solToken = tokenPara(1L, "ana@unmsm.edu.pe", Rol.SOLICITANTE);
        String tecToken = tokenPara(2L, "tec@unmsm.edu.pe", Rol.TECNICO);
        String supToken = tokenPara(3L, "sup@unmsm.edu.pe", Rol.SUPERVISOR);

        mvc.perform(get("/api/solicitudes").header("Authorization", "Bearer " + solToken))
                .andExpect(status().isForbidden());
        mvc.perform(put("/api/solicitudes/1/asignar")
                        .header("Authorization", "Bearer " + solToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"responsableId\":2}"))
                .andExpect(status().isForbidden());
        mvc.perform(put("/api/solicitudes/1/prioridad")
                        .header("Authorization", "Bearer " + tecToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"prioridadId\":1}"))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/solicitudes/asignadas").header("Authorization", "Bearer " + supToken))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/usuarios/tecnicos").header("Authorization", "Bearer " + solToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void supervisorCanQueryBandejaAndTechnicians() throws Exception {
        String supToken = tokenPara(3L, "sup@unmsm.edu.pe", Rol.SUPERVISOR);
        when(solicitudService.bandeja(eq(3L), eq(2), eq(1), eq(1), eq(true), any()))
                .thenReturn(new PageImpl<>(List.of()));
        Area area = org.mockito.Mockito.mock(Area.class);
        when(area.getId()).thenReturn(1);
        Rol r = org.mockito.Mockito.mock(Rol.class);
        when(r.getNombre()).thenReturn(Rol.TECNICO);
        Usuario tec = new Usuario("Luis", "luis@unmsm.edu.pe", "hash", r, area);
        org.springframework.test.util.ReflectionTestUtils.setField(tec, "id", 7L);
        when(usuarioRepository.buscarActivosPorRolYArea(Rol.TECNICO, 1)).thenReturn(List.of(tec));

        mvc.perform(get("/api/solicitudes?estadoId=2&categoriaId=1&prioridadId=1&vencidas=true")
                        .header("Authorization", "Bearer " + supToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements").value(0));

        mvc.perform(get("/api/usuarios/tecnicos?areaId=1")
                        .header("Authorization", "Bearer " + supToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(7))
                .andExpect(jsonPath("$[0].nombre").value("Luis"));
    }

    @Test
    void cambiarEstadoSupportsEstadoIdAndEstadoDestinoIdAliases() throws Exception {
        String tecToken = tokenPara(2L, "tec@unmsm.edu.pe", Rol.TECNICO);
        when(solicitudService.cambiarEstado(eq(2L), eq(1L), any()))
                .thenReturn(detalleEjemplo("En atención"));

        mvc.perform(put("/api/solicitudes/1/estado")
                        .header("Authorization", "Bearer " + tecToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estadoId\":4,\"comentario\":\"Iniciando\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("En atención"));

        mvc.perform(put("/api/solicitudes/1/estado")
                        .header("Authorization", "Bearer " + tecToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estadoDestinoId\":4,\"motivo\":\"Iniciando\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("En atención"));
    }

    @Test
    void invalidTransitionReturns422ProblemDetailAndMissingBodyFieldReturns400() throws Exception {
        String tecToken = tokenPara(2L, "tec@unmsm.edu.pe", Rol.TECNICO);
        when(solicitudService.cambiarEstado(eq(2L), eq(1L), any()))
                .thenThrow(BusinessException.unprocessable("TRANSICION_NO_PERMITIDA",
                        "Transición de estado no permitida", "No se puede pasar de 'Registrada' a 'Resuelta'."));
        when(solicitudService.transicionesDisponibles(2L, 1L))
                .thenReturn(List.of(new TransicionResponse(4, "En atención", false, false)));

        mvc.perform(get("/api/solicitudes/1/transiciones").header("Authorization", "Bearer " + tecToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nombre").value("En atención"));

        mvc.perform(put("/api/solicitudes/1/estado")
                        .header("Authorization", "Bearer " + tecToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estadoId\":5}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("TRANSICION_NO_PERMITIDA"));

        mvc.perform(put("/api/solicitudes/1/estado")
                        .header("Authorization", "Bearer " + tecToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("DATOS_INVALIDOS"));
    }
}
