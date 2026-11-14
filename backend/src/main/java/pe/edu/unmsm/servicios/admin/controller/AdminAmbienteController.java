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
import pe.edu.unmsm.servicios.admin.dto.AdminDtos.AmbienteAdminResponse;
import pe.edu.unmsm.servicios.admin.dto.AdminDtos.CambiarActivoRequest;
import pe.edu.unmsm.servicios.admin.dto.AdminDtos.GuardarAmbienteRequest;
import pe.edu.unmsm.servicios.admin.service.AdminCatalogoService;
import pe.edu.unmsm.servicios.ambiente.entity.Ambiente;

@RestController
@RequestMapping("/api/admin/ambientes")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Administración de Ambientes", description = "HU-12")
public class AdminAmbienteController {

    private final AdminCatalogoService adminService;

    public AdminAmbienteController(AdminCatalogoService adminService) {
        this.adminService = adminService;
    }

    @GetMapping
    @Operation(summary = "Lista todos los ambientes registrados")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de ambientes obtenida"),
            @ApiResponse(responseCode = "403", description = "No autorizado (requiere ADMIN)")
    })
    public List<AmbienteAdminResponse> listar() {
        return adminService.listarAmbientes().stream().map(AmbienteAdminResponse::de).toList();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtiene un ambiente por su ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Ambiente obtenido"),
            @ApiResponse(responseCode = "403", description = "No autorizado (requiere ADMIN)"),
            @ApiResponse(responseCode = "404", description = "Ambiente no encontrado")
    })
    public AmbienteAdminResponse obtener(@PathVariable Integer id) {
        return AmbienteAdminResponse.de(adminService.obtenerAmbiente(id));
    }

    @PostMapping
    @Operation(summary = "Crea un nuevo ambiente de la facultad")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Ambiente creado con éxito"),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos"),
            @ApiResponse(responseCode = "403", description = "No autorizado (requiere ADMIN)"),
            @ApiResponse(responseCode = "409", description = "Nombre de ambiente ya existe")
    })
    public ResponseEntity<AmbienteAdminResponse> crear(@Valid @RequestBody GuardarAmbienteRequest request) {
        Ambiente a = adminService.crearAmbiente(
                request.nombre(),
                request.tipo(),
                request.pabellon(),
                request.piso(),
                request.capacidad()
        );
        return ResponseEntity.created(URI.create("/api/admin/ambientes/" + a.getId()))
                .body(AmbienteAdminResponse.de(a));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualiza un ambiente existente")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Ambiente actualizado con éxito"),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos"),
            @ApiResponse(responseCode = "403", description = "No autorizado (requiere ADMIN)"),
            @ApiResponse(responseCode = "404", description = "Ambiente no encontrado"),
            @ApiResponse(responseCode = "409", description = "Nombre de ambiente ya existe")
    })
    public AmbienteAdminResponse actualizar(@PathVariable Integer id, @Valid @RequestBody GuardarAmbienteRequest request) {
        return AmbienteAdminResponse.de(adminService.actualizarAmbiente(
                id,
                request.nombre(),
                request.tipo(),
                request.pabellon(),
                request.piso(),
                request.capacidad()
        ));
    }

    @PutMapping("/{id}/activo")
    @Operation(summary = "Activa o desactiva un ambiente")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Estado de ambiente actualizado"),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos"),
            @ApiResponse(responseCode = "403", description = "No autorizado (requiere ADMIN)"),
            @ApiResponse(responseCode = "404", description = "Ambiente no encontrado")
    })
    public AmbienteAdminResponse cambiarActivo(@PathVariable Integer id, @Valid @RequestBody CambiarActivoRequest request) {
        return AmbienteAdminResponse.de(adminService.cambiarActivoAmbiente(id, request.activo()));
    }
}
