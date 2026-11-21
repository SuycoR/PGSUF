package pe.edu.unmsm.servicios.reserva.controller;

import java.net.URI;
import java.time.LocalDate;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import pe.edu.unmsm.servicios.auth.jwt.UsuarioAutenticado;
import pe.edu.unmsm.servicios.reserva.dto.ReservaDtos.CrearReservaRequest;
import pe.edu.unmsm.servicios.reserva.dto.ReservaDtos.RechazarReservaRequest;
import pe.edu.unmsm.servicios.reserva.dto.ReservaDtos.ReservaResponse;
import pe.edu.unmsm.servicios.reserva.dto.ReservaDtos.ReservaResumen;
import pe.edu.unmsm.servicios.reserva.service.ReservaService;

@RestController
@RequestMapping("/api/reservas")
@Tag(name = "Reservas", description = "HU-13 y HU-14: Gestión y aprobación de reservas de ambientes")
public class ReservaController {

    private final ReservaService service;

    public ReservaController(ReservaService service) {
        this.service = service;
    }

    @PostMapping
    @Operation(summary = "Registra una nueva reserva de ambiente (RN-04, RN-17)")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Reserva registrada con éxito"),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos", content = @Content),
            @ApiResponse(responseCode = "409", description = "Cruce de horario con otra reserva aprobada (RN-04)", content = @Content),
            @ApiResponse(responseCode = "422", description = "Regla de negocio no cumplida (fecha pasada, horario invertido, ambiente inactivo)", content = @Content),
            @ApiResponse(responseCode = "401", description = "No autenticado", content = @Content)
    })
    public ResponseEntity<ReservaResponse> registrar(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @Valid @RequestBody CrearReservaRequest request) {
        ReservaResponse creada = service.crear(usuario.id(), request);
        return ResponseEntity.created(URI.create("/api/reservas/" + creada.id())).body(creada);
    }

    @GetMapping("/mis-reservas")
    @Operation(summary = "Lista las reservas del usuario autenticado (paginadas)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Página de reservas del usuario obtenida con éxito"),
            @ApiResponse(responseCode = "401", description = "No autenticado", content = @Content)
    })
    public Page<ReservaResumen> misReservas(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @PageableDefault(size = 10, sort = "fecha", direction = Sort.Direction.DESC) Pageable pageable) {
        return service.listarMisReservas(usuario.id(), pageable);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPERVISOR', 'ADMIN')")
    @Operation(summary = "Bandeja general de reservas para supervisor y administrador con filtros opcionales")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Bandeja de reservas obtenida con éxito"),
            @ApiResponse(responseCode = "401", description = "No autenticado", content = @Content),
            @ApiResponse(responseCode = "403", description = "Acceso denegado (requiere rol SUPERVISOR o ADMIN)", content = @Content)
    })
    public Page<ReservaResumen> bandeja(
            @Parameter(description = "ID del estado a filtrar")
            @RequestParam(required = false) Integer estadoId,
            @Parameter(description = "ID del ambiente a filtrar")
            @RequestParam(required = false) Integer ambienteId,
            @Parameter(description = "Fecha en formato YYYY-MM-DD")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @PageableDefault(size = 10, sort = "fecha", direction = Sort.Direction.DESC) Pageable pageable) {
        return service.listarBandeja(estadoId, ambienteId, fecha, pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtiene el detalle de una reserva según reglas de visibilidad (RN-20)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Detalle de la reserva"),
            @ApiResponse(responseCode = "404", description = "Reserva no encontrada o no visible para el usuario (RN-20)", content = @Content),
            @ApiResponse(responseCode = "401", description = "No autenticado", content = @Content)
    })
    public ReservaResponse obtenerPorId(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @Parameter(description = "ID de la reserva") @PathVariable Long id) {
        return service.obtenerPorId(id, usuario.id(), usuario.rol());
    }

    @PutMapping("/{id}/aprobar")
    @PreAuthorize("hasAnyRole('SUPERVISOR', 'ADMIN')")
    @Operation(summary = "Aprueba una reserva pendiente, revalidando el cruce de horarios pesimistamente (RN-04)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Reserva aprobada con éxito"),
            @ApiResponse(responseCode = "404", description = "Reserva no encontrada", content = @Content),
            @ApiResponse(responseCode = "409", description = "Cruce de horario detectado al momento de aprobar (RN-04)", content = @Content),
            @ApiResponse(responseCode = "422", description = "Estado no válido para aprobación", content = @Content),
            @ApiResponse(responseCode = "403", description = "Acceso denegado", content = @Content)
    })
    public ReservaResponse aprobar(
            @AuthenticationPrincipal UsuarioAutenticado supervisor,
            @Parameter(description = "ID de la reserva") @PathVariable Long id) {
        return service.aprobar(id, supervisor.id());
    }

    @PutMapping("/{id}/rechazar")
    @PreAuthorize("hasAnyRole('SUPERVISOR', 'ADMIN')")
    @Operation(summary = "Rechaza una reserva pendiente indicando el motivo obligatorio (RN-13)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Reserva rechazada con éxito"),
            @ApiResponse(responseCode = "404", description = "Reserva no encontrada", content = @Content),
            @ApiResponse(responseCode = "400", description = "Motivo ausente o inválido", content = @Content),
            @ApiResponse(responseCode = "422", description = "Estado no válido para rechazo", content = @Content),
            @ApiResponse(responseCode = "403", description = "Acceso denegado", content = @Content)
    })
    public ReservaResponse rechazar(
            @AuthenticationPrincipal UsuarioAutenticado supervisor,
            @Parameter(description = "ID de la reserva") @PathVariable Long id,
            @Valid @RequestBody RechazarReservaRequest request) {
        return service.rechazar(id, supervisor.id(), request.motivo());
    }
}
