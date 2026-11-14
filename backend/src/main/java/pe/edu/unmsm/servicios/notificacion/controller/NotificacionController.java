package pe.edu.unmsm.servicios.notificacion.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.SortDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

import pe.edu.unmsm.servicios.auth.jwt.UsuarioAutenticado;
import pe.edu.unmsm.servicios.notificacion.dto.NotificacionDtos.CantidadNoLeidasResponse;
import pe.edu.unmsm.servicios.notificacion.dto.NotificacionDtos.NotificacionResponse;
import pe.edu.unmsm.servicios.notificacion.service.NotificacionService;

@RestController
@RequestMapping("/api/notificaciones")
@Tag(name = "Notificaciones", description = "HU-17: notificaciones del usuario")
public class NotificacionController {

    private final NotificacionService service;

    public NotificacionController(NotificacionService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Lista las notificaciones del usuario autenticado, las más recientes primero (HU-17)")
    @ApiResponse(responseCode = "200", description = "Listado paginado de notificaciones propias")
    public Page<NotificacionResponse> listar(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @RequestParam(required = false) Boolean leida,
            @PageableDefault(size = 20)
            @SortDefault.SortDefaults({
                    @SortDefault(sort = "fechaCreacion", direction = Sort.Direction.DESC),
                    @SortDefault(sort = "id", direction = Sort.Direction.DESC)
            }) Pageable pageable) {
        return service.listar(usuario.id(), leida, pageable);
    }

    @GetMapping("/no-leidas/cantidad")
    @Operation(summary = "Cantidad de notificaciones sin leer del usuario autenticado (HU-17)")
    @ApiResponse(responseCode = "200", description = "Contador para la campana de notificaciones")
    public CantidadNoLeidasResponse cantidadNoLeidas(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return service.cantidadNoLeidas(usuario.id());
    }

    @PutMapping("/{id}/leida")
    @Operation(summary = "Marca una notificación propia como leída (HU-17)")
    @ApiResponse(responseCode = "204", description = "Notificación marcada como leída")
    @ApiResponse(responseCode = "403", description = "La notificación pertenece a otro usuario")
    @ApiResponse(responseCode = "404", description = "Notificación no encontrada")
    public ResponseEntity<Void> marcarLeida(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @PathVariable Long id) {
        service.marcarLeida(usuario.id(), id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/leer-todas")
    @Operation(summary = "Marca todas las notificaciones del usuario autenticado como leídas (HU-17)")
    @ApiResponse(responseCode = "204", description = "Notificaciones marcadas como leídas")
    public ResponseEntity<Void> marcarTodasLeidas(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        service.marcarTodasLeidas(usuario.id());
        return ResponseEntity.noContent().build();
    }
}
