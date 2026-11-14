package pe.edu.unmsm.servicios.ambiente.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
import pe.edu.unmsm.servicios.ambiente.dto.AmbienteDtos.AmbienteResponse;
import pe.edu.unmsm.servicios.ambiente.dto.AmbienteDtos.DisponibilidadResponse;
import pe.edu.unmsm.servicios.ambiente.entity.Pabellon;
import pe.edu.unmsm.servicios.ambiente.entity.TipoAmbiente;
import pe.edu.unmsm.servicios.ambiente.service.AmbienteService;

@RestController
@RequestMapping("/api/ambientes")
@Tag(name = "Ambientes", description = "HU-13: Catálogo y consulta de disponibilidad de ambientes")
public class AmbienteController {

    private final AmbienteService service;

    public AmbienteController(AmbienteService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Lista los ambientes activos con filtros opcionales de tipo, pabellón y piso")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de ambientes activos obtenida con éxito"),
            @ApiResponse(responseCode = "401", description = "No autenticado", content = @Content)
    })
    public List<AmbienteResponse> listar(
            @Parameter(description = "Tipo de ambiente: AUDITORIO, LABORATORIO, SALON")
            @RequestParam(required = false) TipoAmbiente tipo,
            @Parameter(description = "Pabellón: ANTIGUO, NUEVO, CENTRAL")
            @RequestParam(required = false) Pabellon pabellon,
            @Parameter(description = "Número de piso (1 al 4)")
            @RequestParam(required = false) Integer piso) {
        return service.listar(tipo, pabellon, piso);
    }

    @GetMapping("/{id}/disponibilidad")
    @Operation(summary = "Consulta los horarios ocupados por reservas aprobadas de un ambiente en una fecha")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Disponibilidad del ambiente obtenida con éxito"),
            @ApiResponse(responseCode = "404", description = "Ambiente no encontrado", content = @Content),
            @ApiResponse(responseCode = "401", description = "No autenticado", content = @Content)
    })
    public DisponibilidadResponse obtenerDisponibilidad(
            @Parameter(description = "ID del ambiente", required = true)
            @PathVariable Integer id,
            @Parameter(description = "Fecha de consulta en formato YYYY-MM-DD", required = true)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        return service.obtenerDisponibilidad(id, fecha);
    }
}
