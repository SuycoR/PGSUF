package pe.edu.unmsm.servicios.solicitud.controller;

import java.net.URI;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import pe.edu.unmsm.servicios.auth.jwt.UsuarioAutenticado;
import pe.edu.unmsm.servicios.solicitud.dto.SolicitudDtos.CrearSolicitudRequest;
import pe.edu.unmsm.servicios.solicitud.dto.SolicitudDtos.EvidenciaResponse;
import pe.edu.unmsm.servicios.solicitud.dto.SolicitudDtos.HistorialResponse;
import pe.edu.unmsm.servicios.solicitud.dto.SolicitudDtos.SolicitudCreadaResponse;
import pe.edu.unmsm.servicios.solicitud.dto.SolicitudDtos.SolicitudDetalle;
import pe.edu.unmsm.servicios.solicitud.dto.SolicitudDtos.SolicitudResumen;
import pe.edu.unmsm.servicios.solicitud.service.SolicitudService;

@RestController
@RequestMapping("/api/solicitudes")
@Tag(name = "Solicitudes", description = "HU-04, HU-05, HU-06, HU-15")
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

    @GetMapping("/{id}/evidencias")
    @Operation(summary = "Evidencias adjuntas")
    public List<EvidenciaResponse> evidencias(@AuthenticationPrincipal UsuarioAutenticado usuario,
            @PathVariable Long id) {
        return service.evidencias(usuario.id(), id);
    }
}
