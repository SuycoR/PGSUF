package pe.edu.unmsm.servicios.usuario.controller;

import java.net.URI;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import pe.edu.unmsm.servicios.usuario.dto.CrearUsuarioRequest;
import pe.edu.unmsm.servicios.usuario.dto.UsuarioResponse;
import pe.edu.unmsm.servicios.usuario.entity.Usuario;
import pe.edu.unmsm.servicios.usuario.repository.UsuarioRepository;
import pe.edu.unmsm.servicios.usuario.service.UsuarioService;

@RestController
@RequestMapping("/api/admin/usuarios")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Administración de usuarios", description = "HU-12")
public class AdminUsuarioController {

    private final UsuarioService usuarioService;
    private final UsuarioRepository usuarioRepository;

    public AdminUsuarioController(UsuarioService usuarioService, UsuarioRepository usuarioRepository) {
        this.usuarioService = usuarioService;
        this.usuarioRepository = usuarioRepository;
    }

    @PostMapping
    @Operation(summary = "Crea un usuario de cualquier rol (RN-10: área según rol)")
    public ResponseEntity<UsuarioResponse> crear(@Valid @RequestBody CrearUsuarioRequest request) {
        Usuario usuario = usuarioService.crear(request.nombre(), request.correo(), request.password(),
                request.rol(), request.areaId());
        return ResponseEntity.created(URI.create("/api/admin/usuarios/" + usuario.getId()))
                .body(UsuarioResponse.de(usuario));
    }

    @GetMapping
    @Transactional(readOnly = true)
    @Operation(summary = "Lista paginada de usuarios")
    public Page<UsuarioResponse> listar(@PageableDefault(size = 20, sort = "nombre", direction = Sort.Direction.ASC) Pageable pageable) {
        return usuarioRepository.findAll(pageable).map(UsuarioResponse::de);
    }
}
