package pe.edu.unmsm.servicios.catalogo.repository;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import pe.edu.unmsm.servicios.catalogo.entity.TipoSolicitud;

public interface TipoSolicitudRepository extends JpaRepository<TipoSolicitud, Integer> {

    @EntityGraph(attributePaths = {"categoria", "area"})
    List<TipoSolicitud> findByCategoriaIdAndActivoTrueOrderByNombre(Integer categoriaId);

    @EntityGraph(attributePaths = {"categoria", "area"})
    List<TipoSolicitud> findAllByOrderByCategoriaNombreAscNombreAsc();

    boolean existsByCategoriaIdAndNombre(Integer categoriaId, String nombre);

    boolean existsByCategoriaIdAndNombreAndIdNot(Integer categoriaId, String nombre, Integer id);
}
