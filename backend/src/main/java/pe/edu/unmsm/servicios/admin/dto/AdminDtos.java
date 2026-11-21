package pe.edu.unmsm.servicios.admin.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import pe.edu.unmsm.servicios.ambiente.entity.Ambiente;
import pe.edu.unmsm.servicios.ambiente.entity.Pabellon;
import pe.edu.unmsm.servicios.ambiente.entity.TipoAmbiente;
import pe.edu.unmsm.servicios.catalogo.entity.Area;
import pe.edu.unmsm.servicios.catalogo.entity.Categoria;
import pe.edu.unmsm.servicios.catalogo.entity.Estado;
import pe.edu.unmsm.servicios.catalogo.entity.EstadoTransicion;
import pe.edu.unmsm.servicios.catalogo.entity.Prioridad;
import pe.edu.unmsm.servicios.catalogo.entity.TipoSolicitud;

public final class AdminDtos {

    private AdminDtos() {
    }

    public record CambiarActivoRequest(
            @NotNull(message = "el estado activo es obligatorio") Boolean activo
    ) {}

    public record ActualizarUsuarioRequest(
            @NotBlank(message = "es obligatorio") @Size(max = 120, message = "admite hasta 120 caracteres") String nombre,
            @NotBlank(message = "es obligatorio") @Email(message = "no es un correo válido") @Size(max = 120, message = "admite hasta 120 caracteres") String correo,
            @Size(min = 8, max = 72, message = "debe tener entre 8 y 72 caracteres") String password,
            @NotBlank(message = "es obligatorio") String rol,
            Integer areaId
    ) {}

    // ---------------- Area
    public record GuardarAreaRequest(
            @NotBlank(message = "el nombre es obligatorio") @Size(max = 80, message = "admite hasta 80 caracteres") String nombre
    ) {}

    public record AreaAdminResponse(Integer id, String nombre, boolean activo) {
        public static AreaAdminResponse de(Area a) {
            return new AreaAdminResponse(a.getId(), a.getNombre(), a.isActivo());
        }
    }

    // ---------------- Categoria
    public record GuardarCategoriaRequest(
            @NotBlank(message = "el nombre es obligatorio") @Size(max = 80, message = "admite hasta 80 caracteres") String nombre
    ) {}

    public record CategoriaAdminResponse(Integer id, String nombre, boolean activo, boolean esTramite) {
        public static CategoriaAdminResponse de(Categoria c) {
            return new CategoriaAdminResponse(c.getId(), c.getNombre(), c.isActivo(), c.esTramite());
        }
    }

    // ---------------- Tipo Solicitud
    public record GuardarTipoSolicitudRequest(
            @NotNull(message = "la categoría es obligatoria") Integer categoriaId,
            @NotNull(message = "el área es obligatoria") Integer areaId,
            @NotBlank(message = "el nombre es obligatorio") @Size(max = 100, message = "admite hasta 100 caracteres") String nombre,
            Boolean requiereAdjunto,
            @NotNull(message = "el SLA es obligatorio") @Min(value = 1, message = "el SLA debe ser al menos 1 hora") Integer slaHoras
    ) {}

    public record TipoSolicitudAdminResponse(
            Integer id,
            Integer categoriaId,
            String categoriaNombre,
            Integer areaId,
            String areaNombre,
            String nombre,
            boolean requiereAdjunto,
            int slaHoras,
            boolean activo
    ) {
        public static TipoSolicitudAdminResponse de(TipoSolicitud t) {
            return new TipoSolicitudAdminResponse(
                    t.getId(),
                    t.getCategoria().getId(),
                    t.getCategoria().getNombre(),
                    t.getArea().getId(),
                    t.getArea().getNombre(),
                    t.getNombre(),
                    t.isRequiereAdjunto(),
                    t.getSlaHoras(),
                    t.isActivo()
            );
        }
    }

    // ---------------- Prioridad
    public record GuardarPrioridadRequest(
            @NotBlank(message = "el nombre es obligatorio") @Size(max = 20, message = "admite hasta 20 caracteres") String nombre,
            @NotNull(message = "el orden es obligatorio") @Min(value = 1, message = "el orden debe ser positivo") Integer orden
    ) {}

    public record PrioridadAdminResponse(Integer id, String nombre, int orden) {
        public static PrioridadAdminResponse de(Prioridad p) {
            return new PrioridadAdminResponse(p.getId(), p.getNombre(), p.getOrden());
        }
    }

    // ---------------- Estado
    public record GuardarEstadoRequest(
            @NotBlank(message = "el nombre es obligatorio") @Size(max = 30, message = "admite hasta 30 caracteres") String nombre,
            @NotBlank(message = "el ámbito es obligatorio") @Pattern(regexp = "SOLICITUD|RESERVA|AMBOS", message = "ámbito debe ser SOLICITUD, RESERVA o AMBOS") String ambito,
            @NotNull(message = "el orden es obligatorio") @Min(value = 1, message = "el orden debe ser positivo") Integer orden,
            Boolean esFinal
    ) {}

    public record EstadoAdminResponse(Integer id, String nombre, String ambito, int orden, boolean esFinal) {
        public static EstadoAdminResponse de(Estado e) {
            return new EstadoAdminResponse(e.getId(), e.getNombre(), e.getAmbito(), e.getOrden(), e.isEsFinal());
        }
    }

    // ---------------- Transicion
    public record GuardarTransicionRequest(
            @NotNull(message = "el estado origen es obligatorio") Integer estadoOrigenId,
            @NotNull(message = "el estado destino es obligatorio") Integer estadoDestinoId,
            Integer rolId,
            @NotBlank(message = "el ámbito es obligatorio") @Pattern(regexp = "SOLICITUD|RESERVA", message = "ámbito debe ser SOLICITUD o RESERVA") String ambito,
            Boolean requiereMotivo
    ) {}

    public record TransicionAdminResponse(
            Integer id,
            Integer estadoOrigenId,
            String estadoOrigenNombre,
            Integer estadoDestinoId,
            String estadoDestinoNombre,
            Integer rolId,
            String rolNombre,
            String ambito,
            boolean requiereMotivo
    ) {
        public static TransicionAdminResponse de(EstadoTransicion t) {
            return new TransicionAdminResponse(
                    t.getId(),
                    t.getEstadoOrigen().getId(),
                    t.getEstadoOrigen().getNombre(),
                    t.getEstadoDestino().getId(),
                    t.getEstadoDestino().getNombre(),
                    t.getRol() != null ? t.getRol().getId() : null,
                    t.getRol() != null ? t.getRol().getNombre() : null,
                    t.getAmbito(),
                    t.isRequiereMotivo()
            );
        }
    }

    // ---------------- Ambiente
    public record GuardarAmbienteRequest(
            @NotBlank(message = "el nombre es obligatorio") @Size(max = 80, message = "admite hasta 80 caracteres") String nombre,
            @NotNull(message = "el tipo de ambiente es obligatorio") TipoAmbiente tipo,
            @NotNull(message = "el pabellón es obligatorio") Pabellon pabellon,
            @Min(value = 1, message = "el piso mínimo es 1") @Max(value = 4, message = "el piso máximo es 4") Integer piso,
            @NotNull(message = "la capacidad es obligatoria") @Min(value = 1, message = "la capacidad debe ser al menos 1") Integer capacidad
    ) {}

    public record AmbienteAdminResponse(
            Integer id,
            String nombre,
            TipoAmbiente tipo,
            Pabellon pabellon,
            Integer piso,
            int capacidad,
            boolean activo
    ) {
        public static AmbienteAdminResponse de(Ambiente a) {
            return new AmbienteAdminResponse(
                    a.getId(),
                    a.getNombre(),
                    a.getTipo(),
                    a.getPabellon(),
                    a.getPiso(),
                    a.getCapacidad(),
                    a.isActivo()
            );
        }
    }
}
