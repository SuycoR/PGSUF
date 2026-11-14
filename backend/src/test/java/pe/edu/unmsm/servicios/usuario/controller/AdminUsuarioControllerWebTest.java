package pe.edu.unmsm.servicios.usuario.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import pe.edu.unmsm.servicios.usuario.repository.UsuarioRepository;
import pe.edu.unmsm.servicios.usuario.service.UsuarioService;

@WebMvcTest(controllers = AdminUsuarioController.class)
@ActiveProfiles("test")
@EnableConfigurationProperties(AppProperties.class)
@Import({SecurityConfig.class, JwtService.class, JwtAuthenticationFilter.class, ProblemDetailAuthHandlers.class,
        GlobalExceptionHandler.class, ClockConfig.class, WebConfig.class})
class AdminUsuarioControllerWebTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JwtService jwt;

    @MockitoBean
    private UsuarioService usuarioService;

    @MockitoBean
    private UsuarioRepository usuarioRepository;

    private String token(String rolNombre) {
        Rol rol = mock(Rol.class);
        when(rol.getNombre()).thenReturn(rolNombre);
        Usuario u = mock(Usuario.class);
        when(u.getId()).thenReturn(1L);
        when(u.getCorreo()).thenReturn("admin@unmsm.edu.pe");
        when(u.getRol()).thenReturn(rol);
        return jwt.generar(u);
    }

    private Usuario mockUsuario(Long id, String nombre, String correo, String rolNombre, boolean activo) {
        Rol rol = mock(Rol.class);
        when(rol.getNombre()).thenReturn(rolNombre);
        Usuario u = mock(Usuario.class);
        when(u.getId()).thenReturn(id);
        when(u.getNombre()).thenReturn(nombre);
        when(u.getCorreo()).thenReturn(correo);
        when(u.getRol()).thenReturn(rol);
        when(u.getArea()).thenReturn(null);
        when(u.isActivo()).thenReturn(activo);
        return u;
    }

    @Test
    @DisplayName("GET /api/admin/usuarios/{id} retorna usuario si rol es ADMIN")
    void obtener_retornaUsuario() throws Exception {
        Usuario u = mockUsuario(10L, "Carlos López", "carlos@unmsm.edu.pe", "TECNICO", true);
        when(usuarioService.obtenerPorId(10L)).thenReturn(u);

        mvc.perform(get("/api/admin/usuarios/10")
                        .header("Authorization", "Bearer " + token("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.nombre").value("Carlos López"))
                .andExpect(jsonPath("$.correo").value("carlos@unmsm.edu.pe"));
    }

    @Test
    @DisplayName("PUT /api/admin/usuarios/{id} actualiza usuario")
    void actualizar_actualizaUsuario() throws Exception {
        Usuario u = mockUsuario(10L, "Carlos Editado", "carlos@unmsm.edu.pe", "TECNICO", true);
        when(usuarioService.actualizar(eq(10L), eq("Carlos Editado"), eq("carlos@unmsm.edu.pe"), any(), eq("TECNICO"), any()))
                .thenReturn(u);

        mvc.perform(put("/api/admin/usuarios/10")
                        .header("Authorization", "Bearer " + token("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nombre": "Carlos Editado",
                                  "correo": "carlos@unmsm.edu.pe",
                                  "rol": "TECNICO"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Carlos Editado"));
    }

    @Test
    @DisplayName("PUT /api/admin/usuarios/{id}/activo desactiva usuario")
    void cambiarActivo_desactivaUsuario() throws Exception {
        Usuario u = mockUsuario(10L, "Carlos", "carlos@unmsm.edu.pe", "TECNICO", false);
        when(usuarioService.cambiarActivo(eq(10L), eq(false))).thenReturn(u);

        mvc.perform(put("/api/admin/usuarios/10/activo")
                        .header("Authorization", "Bearer " + token("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"activo\": false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activo").value(false));
    }
}
