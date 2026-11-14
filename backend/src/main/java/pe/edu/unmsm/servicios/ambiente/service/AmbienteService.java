package pe.edu.unmsm.servicios.ambiente.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.edu.unmsm.servicios.ambiente.dto.AmbienteDtos.AmbienteResponse;
import pe.edu.unmsm.servicios.ambiente.dto.AmbienteDtos.DisponibilidadResponse;
import pe.edu.unmsm.servicios.ambiente.dto.AmbienteDtos.IntervaloHorario;
import pe.edu.unmsm.servicios.ambiente.entity.Ambiente;
import pe.edu.unmsm.servicios.ambiente.entity.Pabellon;
import pe.edu.unmsm.servicios.ambiente.entity.TipoAmbiente;
import pe.edu.unmsm.servicios.ambiente.repository.AmbienteRepository;
import pe.edu.unmsm.servicios.common.BusinessException;
import pe.edu.unmsm.servicios.reserva.entity.Reserva;
import pe.edu.unmsm.servicios.reserva.repository.ReservaRepository;

@Service
public class AmbienteService {

    private final AmbienteRepository ambientes;
    private final ReservaRepository reservas;

    public AmbienteService(AmbienteRepository ambientes, ReservaRepository reservas) {
        this.ambientes = ambientes;
        this.reservas = reservas;
    }

    @Transactional(readOnly = true)
    public List<AmbienteResponse> listar(TipoAmbiente tipo, Pabellon pabellon, Integer piso) {
        return ambientes.buscarActivosConFiltros(tipo, pabellon, piso).stream()
                .map(AmbienteResponse::de)
                .toList();
    }

    @Transactional(readOnly = true)
    public DisponibilidadResponse obtenerDisponibilidad(Integer ambienteId, LocalDate fecha) {
        Ambiente ambiente = ambientes.findById(ambienteId)
                .orElseThrow(() -> BusinessException.notFound("Ambiente no encontrado",
                        "El ambiente con id " + ambienteId + " no existe."));

        List<Reserva> aprobadas = reservas.findAprobadasPorAmbienteYFecha(ambienteId, fecha);
        List<IntervaloHorario> intervalos = aprobadas.stream()
                .map(IntervaloHorario::de)
                .toList();

        return new DisponibilidadResponse(ambiente.getId(), ambiente.getNombre(), fecha, intervalos);
    }
}
