package pe.edu.unmsm.servicios.solicitud.controller;

import java.nio.charset.StandardCharsets;

import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import pe.edu.unmsm.servicios.auth.jwt.UsuarioAutenticado;
import pe.edu.unmsm.servicios.solicitud.service.SolicitudService;
import pe.edu.unmsm.servicios.solicitud.service.SolicitudService.Descarga;

@RestController
@RequestMapping("/api/evidencias")
@Tag(name = "Solicitudes")
public class EvidenciaController {

    private final SolicitudService service;

    public EvidenciaController(SolicitudService service) {
        this.service = service;
    }

    /** Always served as an attachment; Spring Security adds "X-Content-Type-Options: nosniff". */
    @GetMapping("/{id}/descarga")
    @Operation(summary = "Descarga un archivo de evidencia")
    public ResponseEntity<Resource> descargar(@AuthenticationPrincipal UsuarioAutenticado usuario,
            @PathVariable Long id) {
        Descarga descarga = service.descargar(usuario.id(), id);
        ContentDisposition disposicion = ContentDisposition.attachment()
                .filename(descarga.nombre(), StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposicion.toString())
                .contentType(MediaType.parseMediaType(descarga.tipoMime()))
                .body(descarga.archivo());
    }
}
