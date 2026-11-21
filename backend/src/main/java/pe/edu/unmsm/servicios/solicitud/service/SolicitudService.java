package pe.edu.unmsm.servicios.solicitud.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import pe.edu.unmsm.servicios.catalogo.entity.Categoria;
import pe.edu.unmsm.servicios.catalogo.entity.Estado;
import pe.edu.unmsm.servicios.catalogo.entity.EstadoTransicion;
import pe.edu.unmsm.servicios.catalogo.entity.Prioridad;
import pe.edu.unmsm.servicios.catalogo.entity.Rol;
import pe.edu.unmsm.servicios.catalogo.entity.TipoSolicitud;
import pe.edu.unmsm.servicios.catalogo.repository.EstadoRepository;
import pe.edu.unmsm.servicios.catalogo.repository.PrioridadRepository;
import pe.edu.unmsm.servicios.catalogo.repository.TipoSolicitudRepository;
import pe.edu.unmsm.servicios.common.BusinessException;
import pe.edu.unmsm.servicios.solicitud.dto.SolicitudDtos.CambiarEstadoRequest;
import pe.edu.unmsm.servicios.solicitud.dto.SolicitudDtos.CrearSolicitudRequest;
import pe.edu.unmsm.servicios.solicitud.dto.SolicitudDtos.EvidenciaResponse;
import pe.edu.unmsm.servicios.solicitud.dto.SolicitudDtos.HistorialResponse;
import pe.edu.unmsm.servicios.solicitud.dto.SolicitudDtos.SolicitudCreadaResponse;
import pe.edu.unmsm.servicios.solicitud.dto.SolicitudDtos.SolicitudDetalle;
import pe.edu.unmsm.servicios.solicitud.dto.SolicitudDtos.SolicitudResumen;
import pe.edu.unmsm.servicios.solicitud.dto.SolicitudDtos.TransicionResponse;
import pe.edu.unmsm.servicios.solicitud.entity.Evidencia;
import pe.edu.unmsm.servicios.solicitud.entity.HistorialEstado;
import pe.edu.unmsm.servicios.solicitud.entity.Solicitud;
import pe.edu.unmsm.servicios.solicitud.event.EstadoCambiadoEvent;
import pe.edu.unmsm.servicios.solicitud.event.SolicitudAsignadaEvent;
import pe.edu.unmsm.servicios.solicitud.repository.EvidenciaRepository;
import pe.edu.unmsm.servicios.solicitud.repository.HistorialEstadoRepository;
import pe.edu.unmsm.servicios.solicitud.repository.SolicitudRepository;
import pe.edu.unmsm.servicios.solicitud.service.AlmacenamientoArchivos.ArchivoGuardado;
import pe.edu.unmsm.servicios.solicitud.service.AlmacenamientoArchivos.ArchivoValidado;
import pe.edu.unmsm.servicios.usuario.entity.Usuario;
import pe.edu.unmsm.servicios.usuario.repository.UsuarioRepository;

@Service
public class SolicitudService {

    public static final int MAXIMO_ARCHIVOS = 5;
    private static final String PREFIJO_CODIGO = "SOL";

    private final SolicitudRepository solicitudes;
    private final HistorialEstadoRepository historial;
    private final EvidenciaRepository evidencias;
    private final TipoSolicitudRepository tipos;
    private final PrioridadRepository prioridades;
    private final EstadoRepository estados;
    private final UsuarioRepository usuarios;
    private final SecuenciaCodigoService secuencia;
    private final AlmacenamientoArchivos almacenamiento;
    private final VisibilidadSolicitudes visibilidad;
    private final TransicionService transiciones;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    public SolicitudService(SolicitudRepository solicitudes, HistorialEstadoRepository historial,
            EvidenciaRepository evidencias, TipoSolicitudRepository tipos, PrioridadRepository prioridades,
            EstadoRepository estados, UsuarioRepository usuarios, SecuenciaCodigoService secuencia,
            AlmacenamientoArchivos almacenamiento, VisibilidadSolicitudes visibilidad,
            TransicionService transiciones, ApplicationEventPublisher events, Clock clock) {
        this.solicitudes = solicitudes;
        this.historial = historial;
        this.evidencias = evidencias;
        this.tipos = tipos;
        this.prioridades = prioridades;
        this.estados = estados;
        this.usuarios = usuarios;
        this.secuencia = secuencia;
        this.almacenamiento = almacenamiento;
        this.visibilidad = visibilidad;
        this.transiciones = transiciones;
        this.events = events;
        this.clock = clock;
    }

    /** HU-04 and HU-15: registers a request with its evidence files in a single transaction. */
    @Transactional
    public SolicitudCreadaResponse registrar(Long usuarioId, CrearSolicitudRequest datos, List<MultipartFile> archivos) {
        TipoSolicitud tipo = tipos.findById(datos.tipoSolicitudId())
                .filter(TipoSolicitud::disponible)
                .orElseThrow(() -> BusinessException.unprocessable("TIPO_INVALIDO", "Tipo de solicitud inválido",
                        "El tipo de solicitud no existe o no está disponible."));
        Prioridad prioridad = prioridades.findById(datos.prioridadId())
                .orElseThrow(() -> BusinessException.unprocessable("PRIORIDAD_INVALIDA", "Prioridad inválida",
                        "La prioridad indicada no existe."));

        // Validate every file before touching the database or the disk.
        List<ArchivoValidado> validados = validarArchivos(archivos, tipo);

        Usuario solicitante = usuarios.getReferenceById(usuarioId);
        // DATETIME columns keep whole seconds: truncating avoids returning a time the database will not store.
        LocalDateTime ahora = ahora();
        Estado registrada = estado(Estado.REGISTRADA);

        Solicitud solicitud = solicitudes.save(new Solicitud(
                secuencia.siguiente(PREFIJO_CODIGO, ahora.getYear()), solicitante, tipo, prioridad, registrada,
                datos.descripcion().trim(), datos.ubicacion().trim(), ahora));
        historial.save(new HistorialEstado(solicitud, null, registrada, solicitante, null, ahora)); // RN-14

        if (tipo.getCategoria().esTramite()) {
            derivarAlArea(solicitud, registrada, ahora);
        }

        guardarEvidencias(solicitud, solicitante, validados, ahora);
        return SolicitudCreadaResponse.de(solicitud);
    }

    /** HU-05. */
    @Transactional(readOnly = true)
    public Page<SolicitudResumen> misSolicitudes(Long usuarioId, Integer estadoId, Integer categoriaId,
            Pageable pageable) {
        LocalDateTime ahora = LocalDateTime.now(clock);
        return solicitudes.buscarDelUsuario(usuarioId, estadoId, categoriaId, pageable)
                .map(s -> SolicitudResumen.de(s, ahora));
    }

    /** HU-07: inbox filtered by role visibility (SUP technical, PA own area, ADM all). */
    @Transactional(readOnly = true)
    public Page<SolicitudResumen> bandeja(Long usuarioId, Integer estadoId, Integer categoriaId, Integer prioridadId,
            Boolean vencidas, Pageable pageable) {
        Usuario usuario = buscarUsuario(usuarioId);
        String rol = usuario.getRol().getNombre();
        boolean soloTecnicas = Rol.SUPERVISOR.equals(rol);
        Integer areaId = null;
        if (Rol.PERSONAL_AREA.equals(rol)) {
            if (usuario.getArea() == null) {
                throw BusinessException.forbidden("El usuario de área no tiene un área asignada.");
            }
            areaId = usuario.getArea().getId();
        } else if (!soloTecnicas && !Rol.ADMIN.equals(rol)) {
            throw BusinessException.forbidden("No tiene permiso para consultar la bandeja.");
        }

        LocalDateTime ahora = ahora();
        return solicitudes.buscarBandeja(soloTecnicas, Categoria.SOPORTE_TECNICO, areaId, estadoId, categoriaId,
                        prioridadId, vencidas, ahora, pageable)
                .map(s -> SolicitudResumen.de(s, ahora));
    }

    /** HU-09: requests assigned to the authenticated technician. */
    @Transactional(readOnly = true)
    public Page<SolicitudResumen> asignadas(Long usuarioId, Integer estadoId, Pageable pageable) {
        LocalDateTime ahora = ahora();
        return solicitudes.buscarAsignadas(usuarioId, estadoId, pageable)
                .map(s -> SolicitudResumen.de(s, ahora));
    }

    /** HU-06. */
    @Transactional(readOnly = true)
    public SolicitudDetalle detalle(Long usuarioId, Long solicitudId) {
        return SolicitudDetalle.de(visible(usuarioId, solicitudId), LocalDateTime.now(clock));
    }

    /** HU-09: target states allowed from the current state for the authenticated user (RN-05, RN-06). */
    @Transactional(readOnly = true)
    public List<TransicionResponse> transicionesDisponibles(Long usuarioId, Long solicitudId) {
        Solicitud solicitud = visible(usuarioId, solicitudId);
        Usuario usuario = buscarUsuario(usuarioId);
        if (!puedeActuar(usuario, solicitud)) {
            return List.of();
        }
        return transiciones.disponibles(solicitud.getEstado(), usuario.getRol().getNombre(),
                        EstadoTransicion.AMBITO_SOLICITUD).stream()
                .map(TransicionResponse::de)
                .toList();
    }

    /** HU-08 (RN-07): assigns a technician of the request's area and transitions to "Asignada". */
    @Transactional
    public SolicitudDetalle asignar(Long supervisorId, Long solicitudId, Long responsableId) {
        Usuario supervisor = buscarUsuario(supervisorId);
        Solicitud solicitud = buscarSolicitud(solicitudId);
        validarAutorizacionAccion(supervisor, solicitud);

        Estado origen = solicitud.getEstado();
        Estado asignada = estado(Estado.ASIGNADA);
        transiciones.validar(origen, asignada.getId(), supervisor.getRol().getNombre(),
                EstadoTransicion.AMBITO_SOLICITUD, null);

        Usuario tecnico = usuarios.findById(responsableId)
                .orElseThrow(() -> BusinessException.unprocessable("TECNICO_INVALIDO", "Técnico inválido",
                        "El técnico seleccionado no existe."));
        if (!tecnico.isActivo() || !Rol.TECNICO.equals(tecnico.getRol().getNombre())) {
            throw BusinessException.unprocessable("TECNICO_INVALIDO", "Técnico inválido",
                    "El usuario seleccionado no es un técnico activo.");
        }
        if (tecnico.getArea() == null
                || !Objects.equals(tecnico.getArea().getId(), solicitud.getTipoSolicitud().getArea().getId())) {
            throw BusinessException.unprocessable("TECNICO_AREA_INVALIDA", "Técnico de otra área",
                    "El técnico seleccionado no pertenece al área de la solicitud.");
        }

        LocalDateTime ahora = ahora();
        solicitud.asignarResponsable(tecnico);
        solicitud.cambiarEstado(asignada, ahora);
        historial.save(new HistorialEstado(solicitud, origen, asignada, supervisor,
                "Asignada a " + tecnico.getNombre(), ahora));
        events.publishEvent(new SolicitudAsignadaEvent(solicitud.getId(), tecnico.getId(), supervisor.getId()));
        return SolicitudDetalle.de(solicitud, ahora);
    }

    /** HU-09 & HU-16 (RN-05, RN-06, RN-08, RN-13, RN-14, RN-15): transitions the request state. */
    @Transactional
    public SolicitudDetalle cambiarEstado(Long usuarioId, Long solicitudId, CambiarEstadoRequest datos) {
        Usuario usuario = buscarUsuario(usuarioId);
        Solicitud solicitud = buscarSolicitud(solicitudId);
        validarAutorizacionAccion(usuario, solicitud);

        String observacion = normalizarTexto(datos.comentario());
        Estado origen = solicitud.getEstado();
        EstadoTransicion transicion = transiciones.validar(origen, datos.estadoId(), usuario.getRol().getNombre(),
                EstadoTransicion.AMBITO_SOLICITUD, observacion);
        Estado destino = transicion.getEstadoDestino();

        if (Estado.ASIGNADA.equals(destino.getNombre()) && solicitud.getResponsable() == null) {
            throw BusinessException.unprocessable("RESPONSABLE_REQUERIDO", "Asignación requerida",
                    "Debe asignar un técnico responsable mediante la acción de asignar.");
        }

        LocalDateTime ahora = ahora();
        if (Rol.PERSONAL_AREA.equals(usuario.getRol().getNombre())
                && Estado.EN_EVALUACION.equals(origen.getNombre())
                && Estado.EN_ATENCION.equals(destino.getNombre())) {
            solicitud.asignarResponsable(usuario); // RN-08
        }
        if (transicion.isRequiereMotivo() || (Estado.RESUELTA.equals(destino.getNombre()) && observacion != null)) {
            solicitud.setRespuesta(observacion); // RN-13 & HU-16
        }

        solicitud.cambiarEstado(destino, ahora);
        historial.save(new HistorialEstado(solicitud, origen, destino, usuario, observacion, ahora));
        events.publishEvent(new EstadoCambiadoEvent(solicitud.getId(), origen.getNombre(), destino.getNombre(),
                usuario.getId()));
        return SolicitudDetalle.de(solicitud, ahora);
    }

    /** HU-07: updates the request priority (supervisor only). */
    @Transactional
    public SolicitudDetalle cambiarPrioridad(Long supervisorId, Long solicitudId, Integer prioridadId) {
        Usuario supervisor = buscarUsuario(supervisorId);
        Solicitud solicitud = buscarSolicitud(solicitudId);
        validarAutorizacionAccion(supervisor, solicitud);

        Prioridad prioridad = prioridades.findById(prioridadId)
                .orElseThrow(() -> BusinessException.unprocessable("PRIORIDAD_INVALIDA", "Prioridad inválida",
                        "La prioridad indicada no existe."));
        solicitud.cambiarPrioridad(prioridad);
        return SolicitudDetalle.de(solicitud, ahora());
    }

    /** HU-15 (RN-12): attaches additional evidence files for any involved user. */
    @Transactional
    public List<EvidenciaResponse> agregarEvidencias(Long usuarioId, Long solicitudId, List<MultipartFile> archivos) {
        Solicitud solicitud = visible(usuarioId, solicitudId);
        Usuario usuario = buscarUsuario(usuarioId);
        List<MultipartFile> recibidos = archivos == null ? List.of() : archivos;
        if (recibidos.isEmpty()) {
            throw BusinessException.unprocessable("ARCHIVO_INVALIDO", "Archivo requerido",
                    "Debe adjuntar al menos un archivo.");
        }
        if (recibidos.size() > MAXIMO_ARCHIVOS) {
            throw BusinessException.unprocessable("DEMASIADOS_ARCHIVOS", "Demasiados archivos",
                    "Puede adjuntar hasta " + MAXIMO_ARCHIVOS + " archivos por envío.");
        }
        List<ArchivoValidado> validados = recibidos.stream().map(almacenamiento::validar).toList();
        return guardarEvidencias(solicitud, usuario, validados, ahora());
    }

    @Transactional(readOnly = true)
    public List<HistorialResponse> historial(Long usuarioId, Long solicitudId) {
        visible(usuarioId, solicitudId);
        return historial.findBySolicitudIdOrderByFechaAscIdAsc(solicitudId).stream()
                .map(HistorialResponse::de)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<EvidenciaResponse> evidencias(Long usuarioId, Long solicitudId) {
        visible(usuarioId, solicitudId);
        return evidencias.findBySolicitudIdOrderByFechaSubida(solicitudId).stream()
                .map(EvidenciaResponse::de)
                .toList();
    }

    public record Descarga(Resource archivo, String nombre, String tipoMime) {
    }

    @Transactional(readOnly = true)
    public Descarga descargar(Long usuarioId, Long evidenciaId) {
        Evidencia evidencia = evidencias.findWithSolicitudById(evidenciaId)
                .orElseThrow(() -> BusinessException.notFound("Archivo no encontrado", "El archivo no existe."));
        visible(usuarioId, evidencia.getSolicitud().getId());
        return new Descarga(almacenamiento.leer(evidencia.getRuta()), evidencia.getNombreArchivo(),
                evidencia.getTipoMime());
    }

    /** RN-20: a request the user cannot see answers 404, exactly like one that does not exist. */
    private Solicitud visible(Long usuarioId, Long solicitudId) {
        Usuario usuario = buscarUsuario(usuarioId);
        return solicitudes.buscarDetalle(solicitudId)
                .filter(s -> visibilidad.puedeVer(usuario, s))
                .orElseThrow(() -> BusinessException.notFound("Solicitud no encontrada",
                        "La solicitud no existe."));
    }

    private Solicitud buscarSolicitud(Long solicitudId) {
        return solicitudes.buscarDetalle(solicitudId)
                .orElseThrow(() -> BusinessException.notFound("Solicitud no encontrada",
                        "La solicitud no existe."));
    }

    private Usuario buscarUsuario(Long usuarioId) {
        return usuarios.findById(usuarioId)
                .orElseThrow(() -> BusinessException.notFound("Usuario no encontrado", "El usuario ya no existe."));
    }

    /** RN-06: role + relationship (ownership, assignment, or area) required to act on a request. */
    private void validarAutorizacionAccion(Usuario usuario, Solicitud solicitud) {
        if (!puedeActuar(usuario, solicitud)) {
            throw BusinessException.forbidden("No tiene permiso para actuar sobre esta solicitud.");
        }
    }

    private boolean puedeActuar(Usuario usuario, Solicitud solicitud) {
        return switch (usuario.getRol().getNombre()) {
            case Rol.SOLICITANTE -> Objects.equals(solicitud.getUsuario().getId(), usuario.getId());
            case Rol.TECNICO -> solicitud.getResponsable() != null
                    && Objects.equals(solicitud.getResponsable().getId(), usuario.getId());
            case Rol.PERSONAL_AREA -> usuario.getArea() != null
                    && Objects.equals(usuario.getArea().getId(), solicitud.getTipoSolicitud().getArea().getId())
                    && (solicitud.getResponsable() == null
                            || Objects.equals(solicitud.getResponsable().getId(), usuario.getId()));
            case Rol.SUPERVISOR -> !solicitud.getTipoSolicitud().getCategoria().esTramite();
            default -> false;
        };
    }

    private LocalDateTime ahora() {
        return LocalDateTime.now(clock).truncatedTo(ChronoUnit.SECONDS);
    }

    private static String normalizarTexto(String texto) {
        if (texto == null) {
            return null;
        }
        String limpio = texto.trim();
        return limpio.isEmpty() ? null : limpio;
    }

    /** RN-09: academic procedures go straight to their area, as a system change in the history. */
    private void derivarAlArea(Solicitud solicitud, Estado registrada, LocalDateTime ahora) {
        Estado enEvaluacion = estado(Estado.EN_EVALUACION);
        solicitud.cambiarEstado(enEvaluacion, ahora);
        historial.save(new HistorialEstado(solicitud, registrada, enEvaluacion, null,
                "Derivada automáticamente a " + solicitud.getTipoSolicitud().getArea().getNombre(), ahora));
    }

    /** RN-12: count, content type, size, and mandatory attachment for types that require it. */
    private List<ArchivoValidado> validarArchivos(List<MultipartFile> archivos, TipoSolicitud tipo) {
        List<MultipartFile> recibidos = archivos == null ? List.of() : archivos;
        if (recibidos.size() > MAXIMO_ARCHIVOS) {
            throw BusinessException.unprocessable("DEMASIADOS_ARCHIVOS", "Demasiados archivos",
                    "Puede adjuntar hasta " + MAXIMO_ARCHIVOS + " archivos por envío.");
        }
        if (tipo.isRequiereAdjunto() && recibidos.isEmpty()) {
            throw BusinessException.unprocessable("ADJUNTO_OBLIGATORIO", "Falta el documento de sustento",
                    "Debe adjuntar el documento de sustento.");
        }
        return recibidos.stream().map(almacenamiento::validar).toList();
    }

    /** Files written to disk are removed if the transaction rolls back, so no orphan files remain. */
    private List<EvidenciaResponse> guardarEvidencias(Solicitud solicitud, Usuario usuario,
            List<ArchivoValidado> validados, LocalDateTime ahora) {
        List<String> escritos = new ArrayList<>();
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCompletion(int status) {
                    if (status != STATUS_COMMITTED) {
                        escritos.forEach(almacenamiento::borrar);
                    }
                }
            });
        }
        List<EvidenciaResponse> guardadas = new ArrayList<>();
        for (ArchivoValidado validado : validados) {
            ArchivoGuardado guardado = almacenamiento.guardar(solicitud.getId(), validado);
            escritos.add(guardado.ruta());
            Evidencia entidad = evidencias.save(new Evidencia(solicitud, usuario, guardado.nombreOriginal(),
                    guardado.ruta(), guardado.tipoMime(), guardado.tamanoBytes(), ahora));
            guardadas.add(EvidenciaResponse.de(entidad));
        }
        return guardadas;
    }

    private Estado estado(String nombre) {
        return estados.findByNombre(nombre)
                .orElseThrow(() -> new IllegalStateException("Missing seeded state: " + nombre));
    }
}
