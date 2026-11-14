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
import pe.edu.unmsm.servicios.admin.dto.AdminDtos.EstadoAdminResponse;
import pe.edu.unmsm.servicios.admin.dto.AdminDtos.GuardarEstadoRequest;
import pe.edu.unmsm.servicios.admin.service.AdminCatalogoService;
import pe.edu.unmsm.servicios.catalogo.entity.Estado;

@RestController
@RequestMapping("/api/admin/estados")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Administración de Estados", description = "HU-12")
public class AdminEstadoController {

    private final AdminCatalogoService adminService;

    public AdminEstadoController(AdminCatalogoService adminService) {
        this.adminService = adminService;
    }

    @GetMapping
    @Operation(summary = "Lista todos los estados del sistema ordenados")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de estados obtenida"),
            @ApiResponse(responseCode = "403", description = "No autorizado (requiere ADMIN)")
    })
    public List<EstadoAdminResponse> listar() {
        return adminService.listarEstados().stream().map(EstadoAdminResponse::de).toList();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtiene un estado por su ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Estado obtenido"),
            @ApiResponse(responseCode = "403", description = "No autorizado (requiere ADMIN)"),
            @ApiResponse(responseCode = "404", description = "Estado no encontrado")
    })
    public EstadoAdminResponse obtener(@PathVariable Integer id) {
        return EstadoAdminResponse.de(adminService.obtenerEstado(id));
    }

    @PostMapping
    @Operation(summary = "Crea un nuevo estado de solicitud o reserva")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Estado creado con éxito"),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos"),
            @ApiResponse(responseCode = "403", description = "No autorizado (requiere ADMIN)"),
            @ApiResponse(responseCode = "409", description = "Nombre de estado ya existe")
    })
    public ResponseEntity<EstadoAdminResponse> crear(@Valid @RequestBody GuardarEstadoRequest request) {
        Estado e = adminService.crearEstado(request.nombre(), request.ambito(), request.orden(), request.esFinal());
        return ResponseEntity.created(URI.create("/api/admin/estados/" + e.getId()))
                .body(EstadoAdminResponse.de(e));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualiza un estado existente")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Estado actualizado con éxito"),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos"),
            @ApiResponse(responseCode = "403", description = "No autorizado (requiere ADMIN)"),
            @ApiResponse(responseCode = "404", description = "Estado no encontrado"),
            @ApiResponse(responseCode = "409", description = "Nombre de estado ya existe")
    })
    public EstadoAdminResponse actualizar(@PathVariable Integer id, @Valid @RequestBody GuardarEstadoRequest request) {
        return EstadoAdminResponse.de(adminService.actualizarEstado(id, request.nombre(), request.ambito(), request.orden(), request.esFinal()));
    }
}
