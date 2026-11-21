package pe.edu.unmsm.servicios.admin.controller;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
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
import pe.edu.unmsm.servicios.admin.dto.AdminDtos.GuardarTransicionRequest;
import pe.edu.unmsm.servicios.admin.dto.AdminDtos.TransicionAdminResponse;
import pe.edu.unmsm.servicios.admin.service.AdminCatalogoService;
import pe.edu.unmsm.servicios.catalogo.entity.EstadoTransicion;

@RestController
@RequestMapping("/api/admin/transiciones")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Administración de Transiciones", description = "HU-12")
public class AdminTransicionController {

    private final AdminCatalogoService adminService;

    public AdminTransicionController(AdminCatalogoService adminService) {
        this.adminService = adminService;
    }

    @GetMapping
    @Operation(summary = "Lista todas las reglas de transición de estados")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de transiciones obtenida"),
            @ApiResponse(responseCode = "403", description = "No autorizado (requiere ADMIN)")
    })
    public List<TransicionAdminResponse> listar() {
        return adminService.listarTransiciones().stream().map(TransicionAdminResponse::de).toList();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtiene una transición por su ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Transición obtenida"),
            @ApiResponse(responseCode = "403", description = "No autorizado (requiere ADMIN)"),
            @ApiResponse(responseCode = "404", description = "Transición no encontrada")
    })
    public TransicionAdminResponse obtener(@PathVariable Integer id) {
        return TransicionAdminResponse.de(adminService.obtenerTransicion(id));
    }

    @PostMapping
    @Operation(summary = "Crea una nueva regla de transición entre estados")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Transición creada con éxito"),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos"),
            @ApiResponse(responseCode = "403", description = "No autorizado (requiere ADMIN)"),
            @ApiResponse(responseCode = "404", description = "Estado origen o destino no encontrado"),
            @ApiResponse(responseCode = "409", description = "Regla de transición ya existe"),
            @ApiResponse(responseCode = "422", description = "Rol especificado inválido")
    })
    public ResponseEntity<TransicionAdminResponse> crear(@Valid @RequestBody GuardarTransicionRequest request) {
        EstadoTransicion t = adminService.crearTransicion(
                request.estadoOrigenId(),
                request.estadoDestinoId(),
                request.rolId(),
                request.ambito(),
                request.requiereMotivo()
        );
        return ResponseEntity.created(URI.create("/api/admin/transiciones/" + t.getId()))
                .body(TransicionAdminResponse.de(t));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualiza una regla de transición existente")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Transición actualizada con éxito"),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos"),
            @ApiResponse(responseCode = "403", description = "No autorizado (requiere ADMIN)"),
            @ApiResponse(responseCode = "404", description = "Transición o estado no encontrado"),
            @ApiResponse(responseCode = "422", description = "Rol especificado inválido")
    })
    public TransicionAdminResponse actualizar(@PathVariable Integer id, @Valid @RequestBody GuardarTransicionRequest request) {
        return TransicionAdminResponse.de(adminService.actualizarTransicion(
                id,
                request.estadoOrigenId(),
                request.estadoDestinoId(),
                request.rolId(),
                request.ambito(),
                request.requiereMotivo()
        ));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Elimina una regla de transición")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Transición eliminada con éxito"),
            @ApiResponse(responseCode = "403", description = "No autorizado (requiere ADMIN)"),
            @ApiResponse(responseCode = "404", description = "Transición no encontrada")
    })
    public ResponseEntity<Void> eliminar(@PathVariable Integer id) {
        adminService.eliminarTransicion(id);
        return ResponseEntity.noContent().build();
    }
}
