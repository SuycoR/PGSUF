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
import pe.edu.unmsm.servicios.admin.dto.AdminDtos.AreaAdminResponse;
import pe.edu.unmsm.servicios.admin.dto.AdminDtos.CambiarActivoRequest;
import pe.edu.unmsm.servicios.admin.dto.AdminDtos.GuardarAreaRequest;
import pe.edu.unmsm.servicios.admin.service.AdminCatalogoService;
import pe.edu.unmsm.servicios.catalogo.entity.Area;

@RestController
@RequestMapping("/api/admin/areas")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Administración de Áreas", description = "HU-12")
public class AdminAreaController {

    private final AdminCatalogoService adminService;

    public AdminAreaController(AdminCatalogoService adminService) {
        this.adminService = adminService;
    }

    @GetMapping
    @Operation(summary = "Lista todas las áreas ordenadas por nombre")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de áreas obtenida"),
            @ApiResponse(responseCode = "403", description = "No autorizado (requiere ADMIN)")
    })
    public List<AreaAdminResponse> listar() {
        return adminService.listarAreas().stream().map(AreaAdminResponse::de).toList();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtiene un área por su ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Área obtenida"),
            @ApiResponse(responseCode = "403", description = "No autorizado (requiere ADMIN)"),
            @ApiResponse(responseCode = "404", description = "Área no encontrada")
    })
    public AreaAdminResponse obtener(@PathVariable Integer id) {
        return AreaAdminResponse.de(adminService.obtenerArea(id));
    }

    @PostMapping
    @Operation(summary = "Crea una nueva área")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Área creada con éxito"),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos"),
            @ApiResponse(responseCode = "403", description = "No autorizado (requiere ADMIN)"),
            @ApiResponse(responseCode = "409", description = "Nombre de área ya existe")
    })
    public ResponseEntity<AreaAdminResponse> crear(@Valid @RequestBody GuardarAreaRequest request) {
        Area area = adminService.crearArea(request.nombre());
        return ResponseEntity.created(URI.create("/api/admin/areas/" + area.getId()))
                .body(AreaAdminResponse.de(area));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualiza un área existente")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Área actualizada con éxito"),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos"),
            @ApiResponse(responseCode = "403", description = "No autorizado (requiere ADMIN)"),
            @ApiResponse(responseCode = "404", description = "Área no encontrada"),
            @ApiResponse(responseCode = "409", description = "Nombre de área ya existe")
    })
    public AreaAdminResponse actualizar(@PathVariable Integer id, @Valid @RequestBody GuardarAreaRequest request) {
        return AreaAdminResponse.de(adminService.actualizarArea(id, request.nombre()));
    }

    @PutMapping("/{id}/activo")
    @Operation(summary = "Activa o desactiva un área")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Estado de área actualizado"),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos"),
            @ApiResponse(responseCode = "403", description = "No autorizado (requiere ADMIN)"),
            @ApiResponse(responseCode = "404", description = "Área no encontrada")
    })
    public AreaAdminResponse cambiarActivo(@PathVariable Integer id, @Valid @RequestBody CambiarActivoRequest request) {
        return AreaAdminResponse.de(adminService.cambiarActivoArea(id, request.activo()));
    }
}
