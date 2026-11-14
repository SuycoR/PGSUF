package pe.edu.unmsm.servicios.reserva.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import pe.edu.unmsm.servicios.ambiente.entity.Pabellon;
import pe.edu.unmsm.servicios.ambiente.entity.TipoAmbiente;
import pe.edu.unmsm.servicios.reserva.entity.Reserva;

public final class ReservaDtos {

    private ReservaDtos() {
    }

    public record CrearReservaRequest(
            @NotNull(message = "El ambiente es obligatorio")
            Integer ambienteId,

            @NotNull(message = "La fecha es obligatoria")
            LocalDate fecha,

            @NotNull(message = "La hora de inicio es obligatoria")
            LocalTime horaInicio,

            @NotNull(message = "La hora de fin es obligatoria")
            LocalTime horaFin,

            @NotBlank(message = "El motivo es obligatorio")
            @Size(max = 255, message = "El motivo no puede superar 255 caracteres")
            String motivo
    ) {
    }

    public record RechazarReservaRequest(
            @NotBlank(message = "El motivo de rechazo es obligatorio")
            @Size(max = 255, message = "El motivo de rechazo no puede superar 255 caracteres")
            String motivo
    ) {
    }

    public record ReservaAmbienteResumen(
            Integer id,
            String nombre,
            TipoAmbiente tipo,
            Pabellon pabellon,
            Integer piso
    ) {
    }

    public record ReservaUsuarioResumen(
            Long id,
            String nombre,
            String correo
    ) {
    }

    public record ReservaEstadoResumen(
            Integer id,
            String nombre
    ) {
    }

    public record ReservaResponse(
            Long id,
            String codigo,
            ReservaAmbienteResumen ambiente,
            ReservaUsuarioResumen usuario,
            ReservaUsuarioResumen aprobador,
            ReservaEstadoResumen estado,
            LocalDate fecha,
            LocalTime horaInicio,
            LocalTime horaFin,
            String motivo,
            String motivoRechazo,
            LocalDateTime fechaRegistro,
            LocalDateTime fechaResolucion
    ) {
        public static ReservaResponse de(Reserva r) {
            return new ReservaResponse(
                    r.getId(),
                    r.getCodigo(),
                    new ReservaAmbienteResumen(
                            r.getAmbiente().getId(),
                            r.getAmbiente().getNombre(),
                            r.getAmbiente().getTipo(),
                            r.getAmbiente().getPabellon(),
                            r.getAmbiente().getPiso()
                    ),
                    new ReservaUsuarioResumen(
                            r.getUsuario().getId(),
                            r.getUsuario().getNombre(),
                            r.getUsuario().getCorreo()
                    ),
                    r.getAprobador() != null
                            ? new ReservaUsuarioResumen(
                            r.getAprobador().getId(),
                            r.getAprobador().getNombre(),
                            r.getAprobador().getCorreo())
                            : null,
                    new ReservaEstadoResumen(r.getEstado().getId(), r.getEstado().getNombre()),
                    r.getFecha(),
                    r.getHoraInicio(),
                    r.getHoraFin(),
                    r.getMotivo(),
                    r.getMotivoRechazo(),
                    r.getFechaRegistro(),
                    r.getFechaResolucion()
            );
        }
    }

    public record ReservaResumen(
            Long id,
            String codigo,
            Integer ambienteId,
            String ambienteNombre,
            Long usuarioId,
            String usuarioNombre,
            Integer estadoId,
            String estadoNombre,
            LocalDate fecha,
            LocalTime horaInicio,
            LocalTime horaFin,
            String motivo,
            String motivoRechazo,
            LocalDateTime fechaRegistro,
            LocalDateTime fechaResolucion
    ) {
        public static ReservaResumen de(Reserva r) {
            return new ReservaResumen(
                    r.getId(),
                    r.getCodigo(),
                    r.getAmbiente().getId(),
                    r.getAmbiente().getNombre(),
                    r.getUsuario().getId(),
                    r.getUsuario().getNombre(),
                    r.getEstado().getId(),
                    r.getEstado().getNombre(),
                    r.getFecha(),
                    r.getHoraInicio(),
                    r.getHoraFin(),
                    r.getMotivo(),
                    r.getMotivoRechazo(),
                    r.getFechaRegistro(),
                    r.getFechaResolucion()
            );
        }
    }
}
