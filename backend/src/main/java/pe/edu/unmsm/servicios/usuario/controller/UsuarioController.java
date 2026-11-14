package pe.edu.unmsm.servicios.usuario.controller;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

import pe.edu.unmsm.servicios.catalogo.entity.Rol;
import pe.edu.unmsm.servicios.usuario.dto.UsuarioResponse;
import pe.edu.unmsm.servicios.usuario.repository.UsuarioRepository;

@RestController
@RequestMapping("/api/usuarios")
@Tag(name = "Usuarios", description = "Consultas operativas de usuarios (HU-08)")
public class UsuarioController {

    private final UsuarioRepository usuarioRepository;

    public UsuarioController(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @GetMapping("/tecnicos")
    @PreAuthorize("hasAnyRole('SUPERVISOR', 'ADMIN')")
    @Transactional(readOnly = true)
    @Operation(summary = "Lista técnicos activos, opcionalmente filtrados por área (HU-08)")
    @ApiResponse(responseCode = "200", description = "Listado de técnicos activos")
    @ApiResponse(responseCode = "403", description = "Solo disponible para supervisores y administradores")
    public List<UsuarioResponse> tecnicos(@RequestParam(required = false) Integer areaId) {
        return usuarioRepository.buscarActivosPorRolYArea(Rol.TECNICO, areaId).stream()
                .map(UsuarioResponse::de)
                .toList();
    }
}
