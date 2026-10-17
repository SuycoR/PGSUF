package pe.edu.unmsm.servicios.catalogo.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import pe.edu.unmsm.servicios.catalogo.entity.Area;

public interface AreaRepository extends JpaRepository<Area, Integer> {
}
