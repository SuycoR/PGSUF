package pe.edu.unmsm.servicios.comentario.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import pe.edu.unmsm.servicios.auth.jwt.UsuarioAutenticado;
import pe.edu.unmsm.servicios.comentario.dto.ComentarioDtos.ComentarioResponse;
import pe.edu.unmsm.servicios.comentario.dto.ComentarioDtos.CrearComentarioRequest;
import pe.edu.unmsm.servicios.comentario.service.ComentarioService;

@RestController
@RequestMapping("/api/solicitudes/{solicitudId}/comentarios")
@Tag(name = "Comentarios", description = "HU-10: comentarios en solicitudes")
public class ComentarioController {

    private final ComentarioService service;

    public ComentarioController(ComentarioService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Lista los comentarios de una solicitud en orden cronológico (HU-10)")
    @ApiResponse(responseCode = "200", description = "Lista cronológica de comentarios")
    @ApiResponse(responseCode = "404", description = "Solicitud no encontrada o no visible")
    public List<ComentarioResponse> listar(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @PathVariable Long solicitudId) {
        return service.listar(usuario.id(), solicitudId);
    }

    @PostMapping
    @Operation(summary = "Agrega un comentario a una solicitud visible para el usuario (HU-10)")
    @ApiResponse(responseCode = "201", description = "Comentario registrado")
    @ApiResponse(responseCode = "400", description = "Texto vacío o fuera de longitud")
    @ApiResponse(responseCode = "404", description = "Solicitud no encontrada o no visible")
    public ResponseEntity<ComentarioResponse> crear(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @PathVariable Long solicitudId,
            @Valid @RequestBody CrearComentarioRequest datos) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.crear(usuario.id(), solicitudId, datos));
    }
}
