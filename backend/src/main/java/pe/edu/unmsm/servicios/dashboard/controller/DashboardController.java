package pe.edu.unmsm.servicios.dashboard.controller;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import pe.edu.unmsm.servicios.dashboard.dto.DashboardDtos.DashboardResponse;
import pe.edu.unmsm.servicios.dashboard.service.DashboardService;

@RestController
@RequestMapping("/api/dashboard")
@Tag(name = "Dashboard", description = "HU-11: Indicadores gerenciales y métricas de atención")
public class DashboardController {

    private final DashboardService service;

    public DashboardController(DashboardService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPERVISOR', 'ADMIN')")
    @Operation(summary = "Obtiene los indicadores de atención agregados para un rango de fechas opcional (HU-11, RN-03)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Métricas e indicadores calculados con éxito"),
            @ApiResponse(responseCode = "400", description = "Rango de fechas inválido o mal formado", content = @Content),
            @ApiResponse(responseCode = "401", description = "No autenticado", content = @Content),
            @ApiResponse(responseCode = "403", description = "Acceso denegado (requiere rol SUPERVISOR o ADMIN)", content = @Content)
    })
    public DashboardResponse obtenerDashboard(
            @Parameter(description = "Fecha inicial del período (YYYY-MM-DD)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @Parameter(description = "Fecha final del período (YYYY-MM-DD)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return service.obtenerIndicadores(desde, hasta);
    }
}
