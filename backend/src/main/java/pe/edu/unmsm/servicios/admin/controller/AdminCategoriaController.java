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
import pe.edu.unmsm.servicios.admin.dto.AdminDtos.CategoriaAdminResponse;
import pe.edu.unmsm.servicios.admin.dto.AdminDtos.GuardarCategoriaRequest;
import pe.edu.unmsm.servicios.admin.service.AdminCatalogoService;
import pe.edu.unmsm.servicios.catalogo.entity.Categoria;

@RestController
@RequestMapping("/api/admin/categorias")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Administración de Categorías", description = "HU-12")
public class AdminCategoriaController {

    private final AdminCatalogoService adminService;

    public AdminCategoriaController(AdminCatalogoService adminService) {
        this.adminService = adminService;
    }

    @GetMapping
    @Operation(summary = "Lista todas las categorías")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de categorías obtenida"),
            @ApiResponse(responseCode = "403", description = "No autorizado (requiere ADMIN)")
    })
    public List<CategoriaAdminResponse> listar() {
        return adminService.listarCategorias().stream().map(CategoriaAdminResponse::de).toList();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtiene una categoría por su ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Categoría obtenida"),
            @ApiResponse(responseCode = "403", description = "No autorizado (requiere ADMIN)"),
            @ApiResponse(responseCode = "404", description = "Categoría no encontrada")
    })
    public CategoriaAdminResponse obtener(@PathVariable Integer id) {
        return CategoriaAdminResponse.de(adminService.obtenerCategoria(id));
    }

    @PostMapping
    @Operation(summary = "Crea una nueva categoría")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Categoría creada con éxito"),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos"),
            @ApiResponse(responseCode = "403", description = "No autorizado (requiere ADMIN)"),
            @ApiResponse(responseCode = "409", description = "Nombre de categoría ya existe")
    })
    public ResponseEntity<CategoriaAdminResponse> crear(@Valid @RequestBody GuardarCategoriaRequest request) {
        Categoria cat = adminService.crearCategoria(request.nombre());
        return ResponseEntity.created(URI.create("/api/admin/categorias/" + cat.getId()))
                .body(CategoriaAdminResponse.de(cat));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualiza una categoría existente")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Categoría actualizada con éxito"),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos"),
            @ApiResponse(responseCode = "403", description = "No autorizado (requiere ADMIN)"),
            @ApiResponse(responseCode = "404", description = "Categoría no encontrada"),
            @ApiResponse(responseCode = "409", description = "Nombre de categoría ya existe")
    })
    public CategoriaAdminResponse actualizar(@PathVariable Integer id, @Valid @RequestBody GuardarCategoriaRequest request) {
        return CategoriaAdminResponse.de(adminService.actualizarCategoria(id, request.nombre()));
    }

    @PutMapping("/{id}/activo")
    @Operation(summary = "Activa o desactiva una categoría")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Estado de categoría actualizado"),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos"),
            @ApiResponse(responseCode = "403", description = "No autorizado (requiere ADMIN)"),
            @ApiResponse(responseCode = "404", description = "Categoría no encontrada")
    })
    public CategoriaAdminResponse cambiarActivo(@PathVariable Integer id, @Valid @RequestBody CambiarActivoRequest request) {
        return CategoriaAdminResponse.de(adminService.cambiarActivoCategoria(id, request.activo()));
    }
}
