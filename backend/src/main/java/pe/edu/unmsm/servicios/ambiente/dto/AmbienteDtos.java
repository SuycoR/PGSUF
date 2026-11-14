package pe.edu.unmsm.servicios.ambiente.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import pe.edu.unmsm.servicios.ambiente.entity.Ambiente;
import pe.edu.unmsm.servicios.ambiente.entity.Pabellon;
import pe.edu.unmsm.servicios.ambiente.entity.TipoAmbiente;
import pe.edu.unmsm.servicios.reserva.entity.Reserva;

public final class AmbienteDtos {

    private AmbienteDtos() {
    }

    public record AmbienteResponse(
            Integer id,
            String nombre,
            TipoAmbiente tipo,
            Pabellon pabellon,
            Integer piso,
            int capacidad,
            boolean activo
    ) {
        public static AmbienteResponse de(Ambiente a) {
            return new AmbienteResponse(
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

    public record IntervaloHorario(
            LocalTime horaInicio,
            LocalTime horaFin,
            String codigo
    ) {
        public static IntervaloHorario de(Reserva r) {
            return new IntervaloHorario(r.getHoraInicio(), r.getHoraFin(), r.getCodigo());
        }
    }

    public record DisponibilidadResponse(
            Integer ambienteId,
            String ambienteNombre,
            LocalDate fecha,
            List<IntervaloHorario> reservasAprobadas
    ) {
    }
}
