package pe.edu.unmsm.servicios.comentario.repository;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import pe.edu.unmsm.servicios.comentario.entity.Comentario;

public interface ComentarioRepository extends JpaRepository<Comentario, Long> {

    @EntityGraph(attributePaths = {"usuario", "usuario.rol"})
    List<Comentario> findBySolicitudIdOrderByFechaAscIdAsc(Long solicitudId);
}
