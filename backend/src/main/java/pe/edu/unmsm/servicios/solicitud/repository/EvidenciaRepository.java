package pe.edu.unmsm.servicios.solicitud.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import pe.edu.unmsm.servicios.solicitud.entity.Evidencia;

public interface EvidenciaRepository extends JpaRepository<Evidencia, Long> {

    @EntityGraph(attributePaths = {"usuario"})
    List<Evidencia> findBySolicitudIdOrderByFechaSubida(Long solicitudId);

    @EntityGraph(attributePaths = {"solicitud"})
    Optional<Evidencia> findWithSolicitudById(Long id);
}
