package pe.edu.unmsm.servicios.catalogo.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import pe.edu.unmsm.servicios.catalogo.entity.Prioridad;

public interface PrioridadRepository extends JpaRepository<Prioridad, Integer> {

    List<Prioridad> findAllByOrderByOrden();
}
