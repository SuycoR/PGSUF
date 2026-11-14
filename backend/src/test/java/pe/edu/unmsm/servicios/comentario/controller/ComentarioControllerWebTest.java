package pe.edu.unmsm.servicios.comentario.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import pe.edu.unmsm.servicios.auth.jwt.JwtAuthenticationFilter;
import pe.edu.unmsm.servicios.auth.jwt.JwtService;
import pe.edu.unmsm.servicios.catalogo.entity.Rol;
import pe.edu.unmsm.servicios.comentario.dto.ComentarioDtos.ComentarioResponse;
import pe.edu.unmsm.servicios.comentario.service.ComentarioService;
import pe.edu.unmsm.servicios.common.BusinessException;
import pe.edu.unmsm.servicios.common.GlobalExceptionHandler;
import pe.edu.unmsm.servicios.common.ProblemDetailAuthHandlers;
import pe.edu.unmsm.servicios.config.AppProperties;
import pe.edu.unmsm.servicios.config.ClockConfig;
import pe.edu.unmsm.servicios.config.SecurityConfig;
import pe.edu.unmsm.servicios.config.WebConfig;
import pe.edu.unmsm.servicios.usuario.entity.Usuario;

@WebMvcTest(controllers = ComentarioController.class)
@ActiveProfiles("test")
@EnableConfigurationProperties(AppProperties.class)
@Import({SecurityConfig.class, JwtService.class, JwtAuthenticationFilter.class, ProblemDetailAuthHandlers.class,
        GlobalExceptionHandler.class, ClockConfig.class, WebConfig.class})
class ComentarioControllerWebTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JwtService jwt;

    @MockitoBean
    private ComentarioService comentarioService;

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
        mvc.perform(get("/api/solicitudes/1/comentarios"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("NO_AUTENTICADO"));
        mvc.perform(post("/api/solicitudes/1/comentarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"texto\":\"Hola\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void blankCommentReturns400AndValidCommentReturns201() throws Exception {
        String token = tokenPara(1L, "ana@unmsm.edu.pe", Rol.SOLICITANTE);
        LocalDateTime fecha = LocalDateTime.of(2026, 10, 20, 10, 0);
        when(comentarioService.crear(eq(1L), eq(10L), any()))
                .thenReturn(new ComentarioResponse(5L, 1L, "Ana", Rol.SOLICITANTE, "Adjunto el detalle", fecha));

        mvc.perform(post("/api/solicitudes/10/comentarios")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"texto\":\"   \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("DATOS_INVALIDOS"));

        mvc.perform(post("/api/solicitudes/10/comentarios")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"texto\":\"Adjunto el detalle\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.autor").value("Ana"))
                .andExpect(jsonPath("$.autorRol").value("SOLICITANTE"))
                .andExpect(jsonPath("$.texto").value("Adjunto el detalle"));
    }

    @Test
    void hiddenRequestReturns404AndVisibleRequestReturnsList() throws Exception {
        String ana = tokenPara(1L, "ana@unmsm.edu.pe", Rol.SOLICITANTE);
        String beto = tokenPara(2L, "beto@unmsm.edu.pe", Rol.SOLICITANTE);
        LocalDateTime fecha = LocalDateTime.of(2026, 10, 20, 10, 0);

        when(comentarioService.listar(1L, 10L))
                .thenReturn(List.of(new ComentarioResponse(1L, 1L, "Ana", Rol.SOLICITANTE, "Primer comentario", fecha)));
        when(comentarioService.listar(2L, 10L))
                .thenThrow(BusinessException.notFound("Solicitud no encontrada", "La solicitud no existe."));

        mvc.perform(get("/api/solicitudes/10/comentarios")
                        .header("Authorization", "Bearer " + ana))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].texto").value("Primer comentario"));

        mvc.perform(get("/api/solicitudes/10/comentarios")
                        .header("Authorization", "Bearer " + beto))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("NO_ENCONTRADO"));
    }
}
