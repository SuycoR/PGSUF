package pe.edu.unmsm.servicios.catalogo.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import pe.edu.unmsm.servicios.catalogo.entity.Estado;

public interface EstadoRepository extends JpaRepository<Estado, Integer> {

    Optional<Estado> findByNombre(String nombre);

    List<Estado> findByAmbitoInOrderByOrden(Collection<String> ambitos);
}
