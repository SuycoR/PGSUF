package pe.edu.unmsm.servicios.solicitud.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import pe.edu.unmsm.servicios.solicitud.entity.Solicitud;

public interface SolicitudRepository extends JpaRepository<Solicitud, Long> {

    /** HU-05: the requester's own requests, with optional filters. */
    @EntityGraph(attributePaths = {"tipoSolicitud", "tipoSolicitud.categoria", "estado", "prioridad"})
    @Query("""
            SELECT s FROM Solicitud s
            WHERE s.usuario.id = :usuarioId
              AND (:estadoId IS NULL OR s.estado.id = :estadoId)
              AND (:categoriaId IS NULL OR s.tipoSolicitud.categoria.id = :categoriaId)
            """)
    Page<Solicitud> buscarDelUsuario(@Param("usuarioId") Long usuarioId, @Param("estadoId") Integer estadoId,
            @Param("categoriaId") Integer categoriaId, Pageable pageable);

    @EntityGraph(attributePaths = {"tipoSolicitud", "tipoSolicitud.categoria", "tipoSolicitud.area", "estado",
            "prioridad", "usuario", "responsable"})
    @Query("SELECT s FROM Solicitud s WHERE s.id = :id")
    Optional<Solicitud> buscarDetalle(@Param("id") Long id);
}
