package pe.edu.unmsm.servicios.solicitud.dto;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import pe.edu.unmsm.servicios.solicitud.entity.Evidencia;
import pe.edu.unmsm.servicios.solicitud.entity.HistorialEstado;
import pe.edu.unmsm.servicios.solicitud.entity.Solicitud;
import pe.edu.unmsm.servicios.usuario.entity.Usuario;

/** Request and response contracts of the requests API (docs/07-api.md). */
public final class SolicitudDtos {

    private SolicitudDtos() {
    }

    /** JSON part "datos" of POST /api/solicitudes. */
    public record CrearSolicitudRequest(
            @NotNull(message = "es obligatorio") Integer tipoSolicitudId,
            @NotNull(message = "es obligatoria") Integer prioridadId,
            @NotBlank(message = "es obligatoria") @Size(max = 2000, message = "admite hasta 2000 caracteres") String descripcion,
            @NotBlank(message = "es obligatoria") @Size(max = 150, message = "admite hasta 150 caracteres") String ubicacion) {
    }

    public record SolicitudCreadaResponse(Long id, String codigo, String estado, LocalDateTime fechaRegistro,
            LocalDateTime fechaLimite) {

        public static SolicitudCreadaResponse de(Solicitud s) {
            return new SolicitudCreadaResponse(s.getId(), s.getCodigo(), s.getEstado().getNombre(),
                    s.getFechaRegistro(), s.getFechaLimite());
        }
    }

    public record SolicitudResumen(Long id, String codigo, String categoria, String tipo, String prioridad,
            String estado, boolean estadoFinal, LocalDateTime fechaRegistro, LocalDateTime fechaLimite,
            boolean vencida) {

        public static SolicitudResumen de(Solicitud s, LocalDateTime ahora) {
            return new SolicitudResumen(s.getId(), s.getCodigo(), s.getTipoSolicitud().getCategoria().getNombre(),
                    s.getTipoSolicitud().getNombre(), s.getPrioridad().getNombre(), s.getEstado().getNombre(),
                    s.getEstado().isEsFinal(), s.getFechaRegistro(), s.getFechaLimite(), s.estaVencida(ahora));
        }
    }

    public record PersonaResumen(Long id, String nombre) {

        static PersonaResumen de(Usuario u) {
            return u == null ? null : new PersonaResumen(u.getId(), u.getNombre());
        }
    }

    public record SolicitudDetalle(Long id, String codigo, String categoria, String tipo, String area,
            boolean requiereAdjunto, String prioridad, String estado, boolean estadoFinal, String descripcion,
            String ubicacion, String respuesta, PersonaResumen solicitante, PersonaResumen responsable,
            LocalDateTime fechaRegistro, LocalDateTime fechaLimite, LocalDateTime fechaCierre, boolean vencida) {

        public static SolicitudDetalle de(Solicitud s, LocalDateTime ahora) {
            var tipo = s.getTipoSolicitud();
            return new SolicitudDetalle(s.getId(), s.getCodigo(), tipo.getCategoria().getNombre(), tipo.getNombre(),
                    tipo.getArea().getNombre(), tipo.isRequiereAdjunto(), s.getPrioridad().getNombre(),
                    s.getEstado().getNombre(), s.getEstado().isEsFinal(), s.getDescripcion(), s.getUbicacion(),
                    s.getRespuesta(), PersonaResumen.de(s.getUsuario()), PersonaResumen.de(s.getResponsable()),
                    s.getFechaRegistro(), s.getFechaLimite(), s.getFechaCierre(), s.estaVencida(ahora));
        }
    }

    /** {@code usuario} is "Sistema" when the change was automatic. */
    public record HistorialResponse(Long id, String estadoAnterior, String estadoNuevo, String usuario,
            String observacion, LocalDateTime fecha) {

        public static HistorialResponse de(HistorialEstado h) {
            return new HistorialResponse(h.getId(),
                    h.getEstadoAnterior() == null ? null : h.getEstadoAnterior().getNombre(),
                    h.getEstadoNuevo().getNombre(),
                    h.getUsuario() == null ? "Sistema" : h.getUsuario().getNombre(),
                    h.getObservacion(), h.getFecha());
        }
    }

    public record EvidenciaResponse(Long id, String nombreArchivo, String tipoMime, int tamanoBytes,
            LocalDateTime fechaSubida, String subidoPor) {

        public static EvidenciaResponse de(Evidencia e) {
            return new EvidenciaResponse(e.getId(), e.getNombreArchivo(), e.getTipoMime(), e.getTamanoBytes(),
                    e.getFechaSubida(), e.getUsuario().getNombre());
        }
    }
}
