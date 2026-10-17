package pe.edu.unmsm.servicios.solicitud.repository;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import pe.edu.unmsm.servicios.solicitud.entity.HistorialEstado;

public interface HistorialEstadoRepository extends JpaRepository<HistorialEstado, Long> {

    @EntityGraph(attributePaths = {"estadoAnterior", "estadoNuevo", "usuario"})
    List<HistorialEstado> findBySolicitudIdOrderByFechaAscIdAsc(Long solicitudId);
}
