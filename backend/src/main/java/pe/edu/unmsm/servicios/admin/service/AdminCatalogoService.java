package pe.edu.unmsm.servicios.admin.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.edu.unmsm.servicios.ambiente.entity.Ambiente;
import pe.edu.unmsm.servicios.ambiente.entity.Pabellon;
import pe.edu.unmsm.servicios.ambiente.entity.TipoAmbiente;
import pe.edu.unmsm.servicios.ambiente.repository.AmbienteRepository;
import pe.edu.unmsm.servicios.catalogo.entity.Area;
import pe.edu.unmsm.servicios.catalogo.entity.Categoria;
import pe.edu.unmsm.servicios.catalogo.entity.Estado;
import pe.edu.unmsm.servicios.catalogo.entity.EstadoTransicion;
import pe.edu.unmsm.servicios.catalogo.entity.Prioridad;
import pe.edu.unmsm.servicios.catalogo.entity.Rol;
import pe.edu.unmsm.servicios.catalogo.entity.TipoSolicitud;
import pe.edu.unmsm.servicios.catalogo.repository.AreaRepository;
import pe.edu.unmsm.servicios.catalogo.repository.CategoriaRepository;
import pe.edu.unmsm.servicios.catalogo.repository.EstadoRepository;
import pe.edu.unmsm.servicios.catalogo.repository.EstadoTransicionRepository;
import pe.edu.unmsm.servicios.catalogo.repository.PrioridadRepository;
import pe.edu.unmsm.servicios.catalogo.repository.RolRepository;
import pe.edu.unmsm.servicios.catalogo.repository.TipoSolicitudRepository;
import pe.edu.unmsm.servicios.common.BusinessException;

@Service
public class AdminCatalogoService {

    private final AreaRepository areaRepository;
    private final CategoriaRepository categoriaRepository;
    private final TipoSolicitudRepository tipoSolicitudRepository;
    private final PrioridadRepository prioridadRepository;
    private final EstadoRepository estadoRepository;
    private final EstadoTransicionRepository estadoTransicionRepository;
    private final AmbienteRepository ambienteRepository;
    private final RolRepository rolRepository;

    public AdminCatalogoService(
            AreaRepository areaRepository,
            CategoriaRepository categoriaRepository,
            TipoSolicitudRepository tipoSolicitudRepository,
            PrioridadRepository prioridadRepository,
            EstadoRepository estadoRepository,
            EstadoTransicionRepository estadoTransicionRepository,
            AmbienteRepository ambienteRepository,
            RolRepository rolRepository) {
        this.areaRepository = areaRepository;
        this.categoriaRepository = categoriaRepository;
        this.tipoSolicitudRepository = tipoSolicitudRepository;
        this.prioridadRepository = prioridadRepository;
        this.estadoRepository = estadoRepository;
        this.estadoTransicionRepository = estadoTransicionRepository;
        this.ambienteRepository = ambienteRepository;
        this.rolRepository = rolRepository;
    }

    // ==========================================
    // ÁREAS
    // ==========================================
    @Transactional(readOnly = true)
    public List<Area> listarAreas() {
        return areaRepository.findAllByOrderByNombreAsc();
    }

    @Transactional(readOnly = true)
    public Area obtenerArea(Integer id) {
        return areaRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("Área no encontrada",
                        "El área con ID " + id + " no existe."));
    }

    @Transactional
    public Area crearArea(String nombre) {
        String nombreTrim = nombre.trim();
        if (areaRepository.existsByNombre(nombreTrim)) {
            throw BusinessException.conflict("AREA_DUPLICADA", "Nombre de área duplicado",
                    "Ya existe un área con el nombre '" + nombreTrim + "'.");
        }
        return areaRepository.save(new Area(nombreTrim, true));
    }

    @Transactional
    public Area actualizarArea(Integer id, String nombre) {
        Area area = obtenerArea(id);
        String nombreTrim = nombre.trim();
        if (areaRepository.existsByNombreAndIdNot(nombreTrim, id)) {
            throw BusinessException.conflict("AREA_DUPLICADA", "Nombre de área duplicado",
                    "Ya existe otra área con el nombre '" + nombreTrim + "'.");
        }
        area.setNombre(nombreTrim);
        return areaRepository.save(area);
    }

    @Transactional
    public Area cambiarActivoArea(Integer id, boolean activo) {
        Area area = obtenerArea(id);
        area.setActivo(activo);
        return areaRepository.save(area);
    }

    // ==========================================
    // CATEGORÍAS
    // ==========================================
    @Transactional(readOnly = true)
    public List<Categoria> listarCategorias() {
        return categoriaRepository.findAllByOrderByNombreAsc();
    }

    @Transactional(readOnly = true)
    public Categoria obtenerCategoria(Integer id) {
        return categoriaRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("Categoría no encontrada",
                        "La categoría con ID " + id + " no existe."));
    }

    @Transactional
    public Categoria crearCategoria(String nombre) {
        String nombreTrim = nombre.trim();
        if (categoriaRepository.existsByNombre(nombreTrim)) {
            throw BusinessException.conflict("CATEGORIA_DUPLICADA", "Nombre de categoría duplicado",
                    "Ya existe una categoría con el nombre '" + nombreTrim + "'.");
        }
        return categoriaRepository.save(new Categoria(nombreTrim, true));
    }

    @Transactional
    public Categoria actualizarCategoria(Integer id, String nombre) {
        Categoria categoria = obtenerCategoria(id);
        String nombreTrim = nombre.trim();
        if (categoriaRepository.existsByNombreAndIdNot(nombreTrim, id)) {
            throw BusinessException.conflict("CATEGORIA_DUPLICADA", "Nombre de categoría duplicado",
                    "Ya existe otra categoría con el nombre '" + nombreTrim + "'.");
        }
        categoria.setNombre(nombreTrim);
        return categoriaRepository.save(categoria);
    }

    @Transactional
    public Categoria cambiarActivoCategoria(Integer id, boolean activo) {
        Categoria categoria = obtenerCategoria(id);
        categoria.setActivo(activo);
        return categoriaRepository.save(categoria);
    }

    // ==========================================
    // TIPOS DE SOLICITUD
    // ==========================================
    @Transactional(readOnly = true)
    public List<TipoSolicitud> listarTiposSolicitud() {
        return tipoSolicitudRepository.findAllByOrderByCategoriaNombreAscNombreAsc();
    }

    @Transactional(readOnly = true)
    public TipoSolicitud obtenerTipoSolicitud(Integer id) {
        return tipoSolicitudRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("Tipo de solicitud no encontrado",
                        "El tipo de solicitud con ID " + id + " no existe."));
    }

    @Transactional
    public TipoSolicitud crearTipoSolicitud(Integer categoriaId, Integer areaId, String nombre, Boolean requiereAdjunto, int slaHoras) {
        String nombreTrim = nombre.trim();
        if (tipoSolicitudRepository.existsByCategoriaIdAndNombre(categoriaId, nombreTrim)) {
            throw BusinessException.conflict("TIPO_SOLICITUD_DUPLICADO", "Tipo de solicitud duplicado",
                    "Ya existe un tipo con el nombre '" + nombreTrim + "' en esta categoría.");
        }
        Categoria categoria = obtenerCategoria(categoriaId);
        Area area = obtenerArea(areaId);
        TipoSolicitud tipo = new TipoSolicitud(categoria, area, nombreTrim, Boolean.TRUE.equals(requiereAdjunto), slaHoras, true);
        return tipoSolicitudRepository.save(tipo);
    }

    @Transactional
    public TipoSolicitud actualizarTipoSolicitud(Integer id, Integer categoriaId, Integer areaId, String nombre, Boolean requiereAdjunto, int slaHoras) {
        TipoSolicitud tipo = obtenerTipoSolicitud(id);
        String nombreTrim = nombre.trim();
        if (tipoSolicitudRepository.existsByCategoriaIdAndNombreAndIdNot(categoriaId, nombreTrim, id)) {
            throw BusinessException.conflict("TIPO_SOLICITUD_DUPLICADO", "Tipo de solicitud duplicado",
                    "Ya existe otro tipo con el nombre '" + nombreTrim + "' en esta categoría.");
        }
        Categoria categoria = obtenerCategoria(categoriaId);
        Area area = obtenerArea(areaId);
        tipo.actualizar(categoria, area, nombreTrim, Boolean.TRUE.equals(requiereAdjunto), slaHoras);
        return tipoSolicitudRepository.save(tipo);
    }

    @Transactional
    public TipoSolicitud cambiarActivoTipoSolicitud(Integer id, boolean activo) {
        TipoSolicitud tipo = obtenerTipoSolicitud(id);
        tipo.setActivo(activo);
        return tipoSolicitudRepository.save(tipo);
    }

    // ==========================================
    // PRIORIDADES
    // ==========================================
    @Transactional(readOnly = true)
    public List<Prioridad> listarPrioridades() {
        return prioridadRepository.findAllByOrderByOrden();
    }

    @Transactional(readOnly = true)
    public Prioridad obtenerPrioridad(Integer id) {
        return prioridadRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("Prioridad no encontrada",
                        "La prioridad con ID " + id + " no existe."));
    }

    @Transactional
    public Prioridad crearPrioridad(String nombre, int orden) {
        String nombreTrim = nombre.trim();
        if (prioridadRepository.existsByNombre(nombreTrim)) {
            throw BusinessException.conflict("PRIORIDAD_DUPLICADA", "Prioridad duplicada",
                    "Ya existe una prioridad con el nombre '" + nombreTrim + "'.");
        }
        return prioridadRepository.save(new Prioridad(nombreTrim, orden));
    }

    @Transactional
    public Prioridad actualizarPrioridad(Integer id, String nombre, int orden) {
        Prioridad prioridad = obtenerPrioridad(id);
        String nombreTrim = nombre.trim();
        if (prioridadRepository.existsByNombreAndIdNot(nombreTrim, id)) {
            throw BusinessException.conflict("PRIORIDAD_DUPLICADA", "Prioridad duplicada",
                    "Ya existe otra prioridad con el nombre '" + nombreTrim + "'.");
        }
        prioridad.setNombre(nombreTrim);
        prioridad.setOrden(orden);
        return prioridadRepository.save(prioridad);
    }

    // ==========================================
    // ESTADOS
    // ==========================================
    @Transactional(readOnly = true)
    public List<Estado> listarEstados() {
        return estadoRepository.findAllByOrderByOrdenAsc();
    }

    @Transactional(readOnly = true)
    public Estado obtenerEstado(Integer id) {
        return estadoRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("Estado no encontrado",
                        "El estado con ID " + id + " no existe."));
    }

    @Transactional
    public Estado crearEstado(String nombre, String ambito, int orden, Boolean esFinal) {
        String nombreTrim = nombre.trim();
        if (estadoRepository.existsByNombre(nombreTrim)) {
            throw BusinessException.conflict("ESTADO_DUPLICADO", "Estado duplicado",
                    "Ya existe un estado con el nombre '" + nombreTrim + "'.");
        }
        return estadoRepository.save(new Estado(nombreTrim, ambito, orden, Boolean.TRUE.equals(esFinal)));
    }

    @Transactional
    public Estado actualizarEstado(Integer id, String nombre, String ambito, int orden, Boolean esFinal) {
        Estado estado = obtenerEstado(id);
        String nombreTrim = nombre.trim();
        if (estadoRepository.existsByNombreAndIdNot(nombreTrim, id)) {
            throw BusinessException.conflict("ESTADO_DUPLICADO", "Estado duplicado",
                    "Ya existe otro estado con el nombre '" + nombreTrim + "'.");
        }
        estado.actualizar(nombreTrim, ambito, orden, Boolean.TRUE.equals(esFinal));
        return estadoRepository.save(estado);
    }

    // ==========================================
    // TRANSICIONES
    // ==========================================
    @Transactional(readOnly = true)
    public List<EstadoTransicion> listarTransiciones() {
        return estadoTransicionRepository.findAllByOrderByAmbitoAscEstadoOrigenOrdenAsc();
    }

    @Transactional(readOnly = true)
    public EstadoTransicion obtenerTransicion(Integer id) {
        return estadoTransicionRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("Transición no encontrada",
                        "La transición con ID " + id + " no existe."));
    }

    @Transactional
    public EstadoTransicion crearTransicion(Integer origenId, Integer destinoId, Integer rolId, String ambito, Boolean requiereMotivo) {
        boolean duplicada = rolId != null
                ? estadoTransicionRepository.existsByEstadoOrigenIdAndEstadoDestinoIdAndRolIdAndAmbito(origenId, destinoId, rolId, ambito)
                : estadoTransicionRepository.existsByEstadoOrigenIdAndEstadoDestinoIdAndRolIsNullAndAmbito(origenId, destinoId, ambito);

        if (duplicada) {
            throw BusinessException.conflict("TRANSICION_DUPLICADA", "Transición duplicada",
                    "Ya existe una regla de transición para estos estados, rol y ámbito.");
        }

        Estado origen = obtenerEstado(origenId);
        Estado destino = obtenerEstado(destinoId);
        Rol rol = rolId != null
                ? rolRepository.findById(rolId).orElseThrow(() -> BusinessException.unprocessable("ROL_INVALIDO", "Rol no válido", "El rol no existe."))
                : null;

        return estadoTransicionRepository.save(new EstadoTransicion(origen, destino, rol, ambito, Boolean.TRUE.equals(requiereMotivo)));
    }

    @Transactional
    public EstadoTransicion actualizarTransicion(Integer id, Integer origenId, Integer destinoId, Integer rolId, String ambito, Boolean requiereMotivo) {
        EstadoTransicion transicion = obtenerTransicion(id);
        Estado origen = obtenerEstado(origenId);
        Estado destino = obtenerEstado(destinoId);
        Rol rol = rolId != null
                ? rolRepository.findById(rolId).orElseThrow(() -> BusinessException.unprocessable("ROL_INVALIDO", "Rol no válido", "El rol no existe."))
                : null;

        transicion.actualizar(origen, destino, rol, ambito, Boolean.TRUE.equals(requiereMotivo));
        return estadoTransicionRepository.save(transicion);
    }

    @Transactional
    public void eliminarTransicion(Integer id) {
        EstadoTransicion transicion = obtenerTransicion(id);
        estadoTransicionRepository.delete(transicion);
    }

    // ==========================================
    // AMBIENTES
    // ==========================================
    @Transactional(readOnly = true)
    public List<Ambiente> listarAmbientes() {
        return ambienteRepository.findAllByOrderByPabellonAscPisoAscNombreAsc();
    }

    @Transactional(readOnly = true)
    public Ambiente obtenerAmbiente(Integer id) {
        return ambienteRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("Ambiente no encontrado",
                        "El ambiente con ID " + id + " no existe."));
    }

    @Transactional
    public Ambiente crearAmbiente(String nombre, TipoAmbiente tipo, Pabellon pabellon, Integer piso, int capacidad) {
        String nombreTrim = nombre.trim();
        if (ambienteRepository.existsByNombre(nombreTrim)) {
            throw BusinessException.conflict("AMBIENTE_DUPLICADO", "Ambiente duplicado",
                    "Ya existe un ambiente con el nombre '" + nombreTrim + "'.");
        }
        return ambienteRepository.save(new Ambiente(nombreTrim, tipo, pabellon, piso, capacidad, true));
    }

    @Transactional
    public Ambiente actualizarAmbiente(Integer id, String nombre, TipoAmbiente tipo, Pabellon pabellon, Integer piso, int capacidad) {
        Ambiente ambiente = obtenerAmbiente(id);
        String nombreTrim = nombre.trim();
        if (ambienteRepository.existsByNombreAndIdNot(nombreTrim, id)) {
            throw BusinessException.conflict("AMBIENTE_DUPLICADO", "Ambiente duplicado",
                    "Ya existe otro ambiente con el nombre '" + nombreTrim + "'.");
        }
        ambiente.actualizar(nombreTrim, tipo, pabellon, piso, capacidad);
        return ambienteRepository.save(ambiente);
    }

    @Transactional
    public Ambiente cambiarActivoAmbiente(Integer id, boolean activo) {
        Ambiente ambiente = obtenerAmbiente(id);
        ambiente.setActivo(activo);
        return ambienteRepository.save(ambiente);
    }
}
