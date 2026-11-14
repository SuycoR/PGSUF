package pe.edu.unmsm.servicios.admin.controller;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import pe.edu.unmsm.servicios.admin.dto.AdminDtos.GuardarPrioridadRequest;
import pe.edu.unmsm.servicios.admin.dto.AdminDtos.PrioridadAdminResponse;
import pe.edu.unmsm.servicios.admin.service.AdminCatalogoService;
import pe.edu.unmsm.servicios.catalogo.entity.Prioridad;

@RestController
@RequestMapping("/api/admin/prioridades")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Administración de Prioridades", description = "HU-12")
public class AdminPrioridadController {

    private final AdminCatalogoService adminService;

    public AdminPrioridadController(AdminCatalogoService adminService) {
        this.adminService = adminService;
    }

    @GetMapping
    @Operation(summary = "Lista todas las prioridades ordenadas por su nivel")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de prioridades obtenida"),
            @ApiResponse(responseCode = "403", description = "No autorizado (requiere ADMIN)")
    })
    public List<PrioridadAdminResponse> listar() {
        return adminService.listarPrioridades().stream().map(PrioridadAdminResponse::de).toList();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtiene una prioridad por su ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Prioridad obtenida"),
            @ApiResponse(responseCode = "403", description = "No autorizado (requiere ADMIN)"),
            @ApiResponse(responseCode = "404", description = "Prioridad no encontrada")
    })
    public PrioridadAdminResponse obtener(@PathVariable Integer id) {
        return PrioridadAdminResponse.de(adminService.obtenerPrioridad(id));
    }

    @PostMapping
    @Operation(summary = "Crea una nueva prioridad")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Prioridad creada con éxito"),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos"),
            @ApiResponse(responseCode = "403", description = "No autorizado (requiere ADMIN)"),
            @ApiResponse(responseCode = "409", description = "Nombre de prioridad ya existe")
    })
    public ResponseEntity<PrioridadAdminResponse> crear(@Valid @RequestBody GuardarPrioridadRequest request) {
        Prioridad p = adminService.crearPrioridad(request.nombre(), request.orden());
        return ResponseEntity.created(URI.create("/api/admin/prioridades/" + p.getId()))
                .body(PrioridadAdminResponse.de(p));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualiza una prioridad existente")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Prioridad actualizada con éxito"),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos"),
            @ApiResponse(responseCode = "403", description = "No autorizado (requiere ADMIN)"),
            @ApiResponse(responseCode = "404", description = "Prioridad no encontrada"),
            @ApiResponse(responseCode = "409", description = "Nombre de prioridad ya existe")
    })
    public PrioridadAdminResponse actualizar(@PathVariable Integer id, @Valid @RequestBody GuardarPrioridadRequest request) {
        return PrioridadAdminResponse.de(adminService.actualizarPrioridad(id, request.nombre(), request.orden()));
    }
}
