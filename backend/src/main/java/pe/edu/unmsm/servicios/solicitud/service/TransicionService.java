package pe.edu.unmsm.servicios.solicitud.service;

import java.util.List;

import org.springframework.stereotype.Service;

import pe.edu.unmsm.servicios.catalogo.entity.Estado;
import pe.edu.unmsm.servicios.catalogo.entity.EstadoTransicion;
import pe.edu.unmsm.servicios.catalogo.repository.EstadoRepository;
import pe.edu.unmsm.servicios.catalogo.repository.EstadoTransicionRepository;
import pe.edu.unmsm.servicios.common.BusinessException;

/**
 * RN-05, RN-11, RN-13: validates state transitions against {@code estado_transicion}
 * instead of hardcoding rules in services.
 */
@Service
public class TransicionService {

    private final EstadoTransicionRepository transiciones;
    private final EstadoRepository estados;

    public TransicionService(EstadoTransicionRepository transiciones, EstadoRepository estados) {
        this.transiciones = transiciones;
        this.estados = estados;
    }

    public List<EstadoTransicion> disponibles(Estado origen, String rolNombre, String ambito) {
        return transiciones.findByEstadoOrigenIdAndRolNombreAndAmbitoOrderByEstadoDestinoOrden(
                origen.getId(), rolNombre, ambito);
    }

    public EstadoTransicion validar(Estado origen, Integer destinoId, String rolNombre, String ambito, String motivo) {
        Estado destino = estados.findById(destinoId)
                .orElseThrow(() -> BusinessException.unprocessable("ESTADO_INVALIDO", "Estado inválido",
                        "El estado indicado no existe."));

        EstadoTransicion transicion = transiciones
                .findByEstadoOrigenIdAndEstadoDestinoIdAndRolNombreAndAmbito(
                        origen.getId(), destino.getId(), rolNombre, ambito)
                .orElseThrow(() -> BusinessException.unprocessable("TRANSICION_NO_PERMITIDA",
                        "Transición de estado no permitida",
                        "No se puede pasar de '" + origen.getNombre() + "' a '" + destino.getNombre() + "'."));

        if (transicion.isRequiereMotivo() && (motivo == null || motivo.isBlank())) {
            throw BusinessException.unprocessable("MOTIVO_OBLIGATORIO", "Motivo obligatorio",
                    "Debe indicar el motivo para pasar al estado '" + destino.getNombre() + "'.");
        }
        return transicion;
    }
}
