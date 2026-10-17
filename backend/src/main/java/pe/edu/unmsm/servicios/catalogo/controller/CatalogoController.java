package pe.edu.unmsm.servicios.catalogo.controller;

import java.util.List;
import java.util.Set;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import pe.edu.unmsm.servicios.catalogo.repository.CategoriaRepository;
import pe.edu.unmsm.servicios.catalogo.repository.EstadoRepository;
import pe.edu.unmsm.servicios.catalogo.repository.PrioridadRepository;
import pe.edu.unmsm.servicios.catalogo.repository.TipoSolicitudRepository;

/** Read-only catalogs used by the forms and filters. */
@RestController
@RequestMapping("/api/catalogos")
@Tag(name = "Catálogos")
public class CatalogoController {

    public record CategoriaResponse(Integer id, String nombre, boolean esTramite) {
    }

    public record TipoSolicitudResponse(Integer id, String nombre, Integer categoriaId, String area,
            boolean requiereAdjunto, int slaHoras) {
    }

    public record PrioridadResponse(Integer id, String nombre, int orden) {
    }

    public record EstadoResponse(Integer id, String nombre, String ambito, int orden, boolean esFinal) {
    }

    private final CategoriaRepository categorias;
    private final TipoSolicitudRepository tipos;
    private final PrioridadRepository prioridades;
    private final EstadoRepository estados;

    public CatalogoController(CategoriaRepository categorias, TipoSolicitudRepository tipos,
            PrioridadRepository prioridades, EstadoRepository estados) {
        this.categorias = categorias;
        this.tipos = tipos;
        this.prioridades = prioridades;
        this.estados = estados;
    }

    @GetMapping("/categorias")
    @Operation(summary = "Categorías activas")
    public List<CategoriaResponse> categorias() {
        return categorias.findByActivoTrueOrderByNombre().stream()
                .map(c -> new CategoriaResponse(c.getId(), c.getNombre(), c.esTramite()))
                .toList();
    }

    @GetMapping("/tipos-solicitud")
    @Transactional(readOnly = true)
    @Operation(summary = "Tipos activos de una categoría")
    public List<TipoSolicitudResponse> tipos(@RequestParam Integer categoriaId) {
        return tipos.findByCategoriaIdAndActivoTrueOrderByNombre(categoriaId).stream()
                .map(t -> new TipoSolicitudResponse(t.getId(), t.getNombre(), t.getCategoria().getId(),
                        t.getArea().getNombre(), t.isRequiereAdjunto(), t.getSlaHoras()))
                .toList();
    }

    @GetMapping("/prioridades")
    @Operation(summary = "Prioridades")
    public List<PrioridadResponse> prioridades() {
        return prioridades.findAllByOrderByOrden().stream()
                .map(p -> new PrioridadResponse(p.getId(), p.getNombre(), p.getOrden()))
                .toList();
    }

    @GetMapping("/estados")
    @Operation(summary = "Estados; ámbito SOLICITUD incluye también los de ámbito AMBOS (RN-11)")
    public List<EstadoResponse> estados(@RequestParam(defaultValue = "SOLICITUD") String ambito) {
        return estados.findByAmbitoInOrderByOrden(Set.of(ambito, "AMBOS")).stream()
                .map(e -> new EstadoResponse(e.getId(), e.getNombre(), e.getAmbito(), e.getOrden(), e.isEsFinal()))
                .toList();
    }
}
