package pe.edu.unmsm.servicios.solicitud.controller;

import java.net.URI;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import pe.edu.unmsm.servicios.auth.jwt.UsuarioAutenticado;
import pe.edu.unmsm.servicios.solicitud.dto.SolicitudDtos.AsignarTecnicoRequest;
import pe.edu.unmsm.servicios.solicitud.dto.SolicitudDtos.CambiarEstadoRequest;
import pe.edu.unmsm.servicios.solicitud.dto.SolicitudDtos.CambiarPrioridadRequest;
import pe.edu.unmsm.servicios.solicitud.dto.SolicitudDtos.CrearSolicitudRequest;
import pe.edu.unmsm.servicios.solicitud.dto.SolicitudDtos.EvidenciaResponse;
import pe.edu.unmsm.servicios.solicitud.dto.SolicitudDtos.HistorialResponse;
import pe.edu.unmsm.servicios.solicitud.dto.SolicitudDtos.SolicitudCreadaResponse;
import pe.edu.unmsm.servicios.solicitud.dto.SolicitudDtos.SolicitudDetalle;
import pe.edu.unmsm.servicios.solicitud.dto.SolicitudDtos.SolicitudResumen;
import pe.edu.unmsm.servicios.solicitud.dto.SolicitudDtos.TransicionResponse;
import pe.edu.unmsm.servicios.solicitud.service.SolicitudService;

@RestController
@RequestMapping("/api/solicitudes")
@Tag(name = "Solicitudes", description = "HU-04 a HU-09, HU-15, HU-16")
public class SolicitudController {

    private final SolicitudService service;

    public SolicitudController(SolicitudService service) {
        this.service = service;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Registra una solicitud (parte 'datos' en JSON y parte 'archivos', hasta 5)")
    public ResponseEntity<SolicitudCreadaResponse> registrar(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @Valid @RequestPart("datos") CrearSolicitudRequest datos,
            @RequestPart(value = "archivos", required = false) List<MultipartFile> archivos) {
        SolicitudCreadaResponse creada = service.registrar(usuario.id(), datos, archivos);
        return ResponseEntity.created(URI.create("/api/solicitudes/" + creada.id())).body(creada);
    }

    @GetMapping("/mis-solicitudes")
    @Operation(summary = "Solicitudes del usuario autenticado")
    public Page<SolicitudResumen> misSolicitudes(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @RequestParam(required = false) Integer estadoId,
            @RequestParam(required = false) Integer categoriaId,
            @PageableDefault(size = 10, sort = "fechaRegistro", direction = Sort.Direction.DESC) Pageable pageable) {
        return service.misSolicitudes(usuario.id(), estadoId, categoriaId, pageable);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPERVISOR', 'PERSONAL_AREA', 'ADMIN')")
    @Operation(summary = "Bandeja de solicitudes con filtros opcionales (HU-07)")
    @ApiResponse(responseCode = "200", description = "Listado paginado de solicitudes visibles para el rol")
    @ApiResponse(responseCode = "403", description = "Rol sin permiso para consultar la bandeja")
    public Page<SolicitudResumen> bandeja(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @RequestParam(required = false) Integer estadoId,
            @RequestParam(required = false) Integer categoriaId,
            @RequestParam(required = false) Integer prioridadId,
            @RequestParam(required = false) Boolean vencidas,
            @PageableDefault(size = 20, sort = "fechaRegistro", direction = Sort.Direction.DESC) Pageable pageable) {
        return service.bandeja(usuario.id(), estadoId, categoriaId, prioridadId, vencidas, pageable);
    }

    @GetMapping("/asignadas")
    @PreAuthorize("hasRole('TECNICO')")
    @Operation(summary = "Solicitudes asignadas al técnico autenticado (HU-09)")
    @ApiResponse(responseCode = "200", description = "Listado paginado de solicitudes asignadas")
    @ApiResponse(responseCode = "403", description = "Solo disponible para técnicos")
    public Page<SolicitudResumen> asignadas(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @RequestParam(required = false) Integer estadoId,
            @PageableDefault(size = 10, sort = "fechaRegistro", direction = Sort.Direction.DESC) Pageable pageable) {
        return service.asignadas(usuario.id(), estadoId, pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalle de una solicitud visible para el usuario")
    public SolicitudDetalle detalle(@AuthenticationPrincipal UsuarioAutenticado usuario, @PathVariable Long id) {
        return service.detalle(usuario.id(), id);
    }

    @GetMapping("/{id}/historial")
    @Operation(summary = "Historial de estados")
    public List<HistorialResponse> historial(@AuthenticationPrincipal UsuarioAutenticado usuario,
            @PathVariable Long id) {
        return service.historial(usuario.id(), id);
    }

    @GetMapping("/{id}/transiciones")
    @Operation(summary = "Estados destino permitidos para el usuario actual según RN-05 (HU-09)")
    @ApiResponse(responseCode = "200", description = "Lista de transiciones permitidas")
    @ApiResponse(responseCode = "404", description = "Solicitud no encontrada o no visible")
    public List<TransicionResponse> transiciones(@AuthenticationPrincipal UsuarioAutenticado usuario,
            @PathVariable Long id) {
        return service.transicionesDisponibles(usuario.id(), id);
    }

    @PutMapping("/{id}/asignar")
    @PreAuthorize("hasRole('SUPERVISOR')")
    @Operation(summary = "Asigna un técnico del área y pasa la solicitud a Asignada (HU-08, RN-07)")
    @ApiResponse(responseCode = "200", description = "Solicitud asignada")
    @ApiResponse(responseCode = "403", description = "No autorizado")
    @ApiResponse(responseCode = "422", description = "Transición o técnico inválido")
    public SolicitudDetalle asignar(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @PathVariable Long id,
            @Valid @RequestBody AsignarTecnicoRequest datos) {
        return service.asignar(usuario.id(), id, datos.responsableId());
    }

    @PutMapping("/{id}/estado")
    @PreAuthorize("hasAnyRole('TECNICO', 'PERSONAL_AREA', 'SUPERVISOR', 'SOLICITANTE')")
    @Operation(summary = "Cambia el estado de la solicitud validando estado_transicion y propiedad (HU-09, HU-16)")
    @ApiResponse(responseCode = "200", description = "Estado actualizado")
    @ApiResponse(responseCode = "403", description = "El usuario no puede actuar sobre esta solicitud (RN-06)")
    @ApiResponse(responseCode = "422", description = "Transición no permitida o motivo obligatorio faltante (RN-05, RN-13)")
    public SolicitudDetalle cambiarEstado(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @PathVariable Long id,
            @Valid @RequestBody CambiarEstadoRequest datos) {
        return service.cambiarEstado(usuario.id(), id, datos);
    }

    @PutMapping("/{id}/prioridad")
    @PreAuthorize("hasRole('SUPERVISOR')")
    @Operation(summary = "Actualiza la prioridad de una solicitud técnica (HU-07)")
    @ApiResponse(responseCode = "200", description = "Prioridad actualizada")
    @ApiResponse(responseCode = "403", description = "No autorizado")
    @ApiResponse(responseCode = "422", description = "Prioridad inválida")
    public SolicitudDetalle cambiarPrioridad(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @PathVariable Long id,
            @Valid @RequestBody CambiarPrioridadRequest datos) {
        return service.cambiarPrioridad(usuario.id(), id, datos.prioridadId());
    }

    @GetMapping("/{id}/evidencias")
    @Operation(summary = "Evidencias adjuntas")
    public List<EvidenciaResponse> evidencias(@AuthenticationPrincipal UsuarioAutenticado usuario,
            @PathVariable Long id) {
        return service.evidencias(usuario.id(), id);
    }

    @PostMapping(path = "/{id}/evidencias", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Agrega archivos de evidencia a una solicitud visible (HU-15, RN-12)")
    @ApiResponse(responseCode = "201", description = "Evidencias registradas")
    @ApiResponse(responseCode = "404", description = "Solicitud no encontrada o no visible")
    @ApiResponse(responseCode = "422", description = "Archivo inválido o exceso de archivos")
    public ResponseEntity<List<EvidenciaResponse>> agregarEvidencias(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @PathVariable Long id,
            @RequestPart(value = "archivos", required = false) List<MultipartFile> archivos) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.agregarEvidencias(usuario.id(), id, archivos));
    }
}
