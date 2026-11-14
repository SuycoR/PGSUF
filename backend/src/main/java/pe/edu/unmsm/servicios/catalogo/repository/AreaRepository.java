package pe.edu.unmsm.servicios.catalogo.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import pe.edu.unmsm.servicios.catalogo.entity.Area;

public interface AreaRepository extends JpaRepository<Area, Integer> {

    List<Area> findAllByOrderByNombreAsc();

    boolean existsByNombre(String nombre);

    boolean existsByNombreAndIdNot(String nombre, Integer id);
}
