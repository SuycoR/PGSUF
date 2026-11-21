package pe.edu.unmsm.servicios.admin.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import pe.edu.unmsm.servicios.admin.service.AdminCatalogoService;
import pe.edu.unmsm.servicios.auth.jwt.JwtAuthenticationFilter;
import pe.edu.unmsm.servicios.auth.jwt.JwtService;
import pe.edu.unmsm.servicios.catalogo.entity.Area;
import pe.edu.unmsm.servicios.catalogo.entity.Rol;
import pe.edu.unmsm.servicios.common.GlobalExceptionHandler;
import pe.edu.unmsm.servicios.common.ProblemDetailAuthHandlers;
import pe.edu.unmsm.servicios.config.AppProperties;
import pe.edu.unmsm.servicios.config.ClockConfig;
import pe.edu.unmsm.servicios.config.SecurityConfig;
import pe.edu.unmsm.servicios.config.WebConfig;
import pe.edu.unmsm.servicios.usuario.entity.Usuario;

@WebMvcTest(controllers = AdminAreaController.class)
@ActiveProfiles("test")
@EnableConfigurationProperties(AppProperties.class)
@Import({SecurityConfig.class, JwtService.class, JwtAuthenticationFilter.class, ProblemDetailAuthHandlers.class,
        GlobalExceptionHandler.class, ClockConfig.class, WebConfig.class})
class AdminAreaControllerWebTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JwtService jwt;

    @MockitoBean
    private AdminCatalogoService service;

    private String token(String rolNombre) {
        Rol rol = mock(Rol.class);
        when(rol.getNombre()).thenReturn(rolNombre);
        Usuario u = mock(Usuario.class);
        when(u.getId()).thenReturn(1L);
        when(u.getCorreo()).thenReturn("admin@unmsm.edu.pe");
        when(u.getRol()).thenReturn(rol);
        return jwt.generar(u);
    }

    @Test
    @DisplayName("GET /api/admin/areas requiere rol ADMIN (403 para TECNICO)")
    void listar_denegadoParaNoAdmin() throws Exception {
        mvc.perform(get("/api/admin/areas")
                        .header("Authorization", "Bearer " + token("TECNICO")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/admin/areas permite listar a ADMIN")
    void listar_permitidoParaAdmin() throws Exception {
        Area area = new Area("Área Sistemas", true);
        when(service.listarAreas()).thenReturn(List.of(area));

        mvc.perform(get("/api/admin/areas")
                        .header("Authorization", "Bearer " + token("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nombre").value("Área Sistemas"));
    }

    @Test
    @DisplayName("POST /api/admin/areas crea área con 201")
    void crear_creaAreaConExito() throws Exception {
        Area area = new Area("Nueva Área", true);
        when(service.crearArea(eq("Nueva Área"))).thenReturn(area);

        mvc.perform(post("/api/admin/areas")
                        .header("Authorization", "Bearer " + token("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\": \"Nueva Área\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nombre").value("Nueva Área"));
    }

    @Test
    @DisplayName("PUT /api/admin/areas/{id}/activo cambia estado a inactivo")
    void cambiarActivo_actualiza() throws Exception {
        Area area = new Area("Área Inactiva", false);
        when(service.cambiarActivoArea(eq(1), eq(false))).thenReturn(area);

        mvc.perform(put("/api/admin/areas/1/activo")
                        .header("Authorization", "Bearer " + token("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"activo\": false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activo").value(false));
    }
}
