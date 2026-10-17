package pe.edu.unmsm.servicios.solicitud.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import pe.edu.unmsm.servicios.catalogo.entity.Estado;
import pe.edu.unmsm.servicios.catalogo.entity.Prioridad;
import pe.edu.unmsm.servicios.catalogo.entity.TipoSolicitud;
import pe.edu.unmsm.servicios.catalogo.repository.EstadoRepository;
import pe.edu.unmsm.servicios.catalogo.repository.PrioridadRepository;
import pe.edu.unmsm.servicios.catalogo.repository.TipoSolicitudRepository;
import pe.edu.unmsm.servicios.common.BusinessException;
import pe.edu.unmsm.servicios.solicitud.dto.SolicitudDtos.CrearSolicitudRequest;
import pe.edu.unmsm.servicios.solicitud.dto.SolicitudDtos.EvidenciaResponse;
import pe.edu.unmsm.servicios.solicitud.dto.SolicitudDtos.HistorialResponse;
import pe.edu.unmsm.servicios.solicitud.dto.SolicitudDtos.SolicitudCreadaResponse;
import pe.edu.unmsm.servicios.solicitud.dto.SolicitudDtos.SolicitudDetalle;
import pe.edu.unmsm.servicios.solicitud.dto.SolicitudDtos.SolicitudResumen;
import pe.edu.unmsm.servicios.solicitud.entity.Evidencia;
import pe.edu.unmsm.servicios.solicitud.entity.HistorialEstado;
import pe.edu.unmsm.servicios.solicitud.entity.Solicitud;
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
    private final Clock clock;

    public SolicitudService(SolicitudRepository solicitudes, HistorialEstadoRepository historial,
            EvidenciaRepository evidencias, TipoSolicitudRepository tipos, PrioridadRepository prioridades,
            EstadoRepository estados, UsuarioRepository usuarios, SecuenciaCodigoService secuencia,
            AlmacenamientoArchivos almacenamiento, VisibilidadSolicitudes visibilidad, Clock clock) {
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
        LocalDateTime ahora = LocalDateTime.now(clock).truncatedTo(ChronoUnit.SECONDS);
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

    /** HU-06. */
    @Transactional(readOnly = true)
    public SolicitudDetalle detalle(Long usuarioId, Long solicitudId) {
        return SolicitudDetalle.de(visible(usuarioId, solicitudId), LocalDateTime.now(clock));
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
        Usuario usuario = usuarios.findById(usuarioId)
                .orElseThrow(() -> BusinessException.notFound("Usuario no encontrado", "El usuario ya no existe."));
        return solicitudes.buscarDetalle(solicitudId)
                .filter(s -> visibilidad.puedeVer(usuario, s))
                .orElseThrow(() -> BusinessException.notFound("Solicitud no encontrada",
                        "La solicitud no existe."));
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
    private void guardarEvidencias(Solicitud solicitud, Usuario usuario, List<ArchivoValidado> validados,
            LocalDateTime ahora) {
        List<String> escritos = new ArrayList<>();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status != STATUS_COMMITTED) {
                    escritos.forEach(almacenamiento::borrar);
                }
            }
        });
        for (ArchivoValidado validado : validados) {
            ArchivoGuardado guardado = almacenamiento.guardar(solicitud.getId(), validado);
            escritos.add(guardado.ruta());
            evidencias.save(new Evidencia(solicitud, usuario, guardado.nombreOriginal(), guardado.ruta(),
                    guardado.tipoMime(), guardado.tamanoBytes(), ahora));
        }
    }

    private Estado estado(String nombre) {
        return estados.findByNombre(nombre)
                .orElseThrow(() -> new IllegalStateException("Missing seeded state: " + nombre));
    }
}
