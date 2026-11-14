package pe.edu.unmsm.servicios.catalogo.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import pe.edu.unmsm.servicios.catalogo.entity.EstadoTransicion;

public interface EstadoTransicionRepository extends JpaRepository<EstadoTransicion, Integer> {

    @EntityGraph(attributePaths = {"estadoDestino"})
    List<EstadoTransicion> findByEstadoOrigenIdAndRolNombreAndAmbitoOrderByEstadoDestinoOrden(
            Integer estadoOrigenId, String rolNombre, String ambito);

    @EntityGraph(attributePaths = {"estadoDestino"})
    Optional<EstadoTransicion> findByEstadoOrigenIdAndEstadoDestinoIdAndRolNombreAndAmbito(
            Integer estadoOrigenId, Integer estadoDestinoId, String rolNombre, String ambito);
}
