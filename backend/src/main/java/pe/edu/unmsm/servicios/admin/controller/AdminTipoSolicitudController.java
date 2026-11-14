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
import pe.edu.unmsm.servicios.admin.dto.AdminDtos.CambiarActivoRequest;
import pe.edu.unmsm.servicios.admin.dto.AdminDtos.GuardarTipoSolicitudRequest;
import pe.edu.unmsm.servicios.admin.dto.AdminDtos.TipoSolicitudAdminResponse;
import pe.edu.unmsm.servicios.admin.service.AdminCatalogoService;
import pe.edu.unmsm.servicios.catalogo.entity.TipoSolicitud;

@RestController
@RequestMapping("/api/admin/tipos-solicitud")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Administración de Tipos de Solicitud", description = "HU-12")
public class AdminTipoSolicitudController {

    private final AdminCatalogoService adminService;

    public AdminTipoSolicitudController(AdminCatalogoService adminService) {
        this.adminService = adminService;
    }

    @GetMapping
    @Operation(summary = "Lista todos los tipos de solicitud")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de tipos obtenida"),
            @ApiResponse(responseCode = "403", description = "No autorizado (requiere ADMIN)")
    })
    public List<TipoSolicitudAdminResponse> listar() {
        return adminService.listarTiposSolicitud().stream().map(TipoSolicitudAdminResponse::de).toList();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtiene un tipo de solicitud por su ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tipo de solicitud obtenido"),
            @ApiResponse(responseCode = "403", description = "No autorizado (requiere ADMIN)"),
            @ApiResponse(responseCode = "404", description = "Tipo de solicitud no encontrado")
    })
    public TipoSolicitudAdminResponse obtener(@PathVariable Integer id) {
        return TipoSolicitudAdminResponse.de(adminService.obtenerTipoSolicitud(id));
    }

    @PostMapping
    @Operation(summary = "Crea un nuevo tipo de solicitud")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Tipo de solicitud creado con éxito"),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos"),
            @ApiResponse(responseCode = "403", description = "No autorizado (requiere ADMIN)"),
            @ApiResponse(responseCode = "404", description = "Categoría o Área no encontrada"),
            @ApiResponse(responseCode = "409", description = "Nombre de tipo duplicado en la misma categoría")
    })
    public ResponseEntity<TipoSolicitudAdminResponse> crear(@Valid @RequestBody GuardarTipoSolicitudRequest request) {
        TipoSolicitud tipo = adminService.crearTipoSolicitud(
                request.categoriaId(),
                request.areaId(),
                request.nombre(),
                request.requiereAdjunto(),
                request.slaHoras()
        );
        return ResponseEntity.created(URI.create("/api/admin/tipos-solicitud/" + tipo.getId()))
                .body(TipoSolicitudAdminResponse.de(tipo));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualiza un tipo de solicitud existente")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tipo de solicitud actualizado con éxito"),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos"),
            @ApiResponse(responseCode = "403", description = "No autorizado (requiere ADMIN)"),
            @ApiResponse(responseCode = "404", description = "Tipo de solicitud, Categoría o Área no encontrada"),
            @ApiResponse(responseCode = "409", description = "Nombre de tipo duplicado en la misma categoría")
    })
    public TipoSolicitudAdminResponse actualizar(@PathVariable Integer id, @Valid @RequestBody GuardarTipoSolicitudRequest request) {
        return TipoSolicitudAdminResponse.de(adminService.actualizarTipoSolicitud(
                id,
                request.categoriaId(),
                request.areaId(),
                request.nombre(),
                request.requiereAdjunto(),
                request.slaHoras()
        ));
    }

    @PutMapping("/{id}/activo")
    @Operation(summary = "Activa o desactiva un tipo de solicitud")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Estado de tipo de solicitud actualizado"),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos"),
            @ApiResponse(responseCode = "403", description = "No autorizado (requiere ADMIN)"),
            @ApiResponse(responseCode = "404", description = "Tipo de solicitud no encontrado")
    })
    public TipoSolicitudAdminResponse cambiarActivo(@PathVariable Integer id, @Valid @RequestBody CambiarActivoRequest request) {
        return TipoSolicitudAdminResponse.de(adminService.cambiarActivoTipoSolicitud(id, request.activo()));
    }
}
