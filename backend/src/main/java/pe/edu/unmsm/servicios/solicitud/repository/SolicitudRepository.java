package pe.edu.unmsm.servicios.solicitud.repository;

import java.time.LocalDateTime;
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

    /** HU-07: inbox for supervisor (technical only), area staff (own area), and admin (all). */
    @EntityGraph(attributePaths = {"tipoSolicitud", "tipoSolicitud.categoria", "tipoSolicitud.area", "estado", "prioridad"})
    @Query("""
            SELECT s FROM Solicitud s
            WHERE (:soloTecnicas = false OR s.tipoSolicitud.categoria.nombre = :categoriaTecnica)
              AND (:areaId IS NULL OR s.tipoSolicitud.area.id = :areaId)
              AND (:estadoId IS NULL OR s.estado.id = :estadoId)
              AND (:categoriaId IS NULL OR s.tipoSolicitud.categoria.id = :categoriaId)
              AND (:prioridadId IS NULL OR s.prioridad.id = :prioridadId)
              AND (:vencidas IS NULL
                   OR (:vencidas = true AND s.estado.esFinal = false AND s.fechaLimite < :ahora)
                   OR (:vencidas = false AND (s.estado.esFinal = true OR s.fechaLimite >= :ahora)))
            """)
    Page<Solicitud> buscarBandeja(
            @Param("soloTecnicas") boolean soloTecnicas,
            @Param("categoriaTecnica") String categoriaTecnica,
            @Param("areaId") Integer areaId,
            @Param("estadoId") Integer estadoId,
            @Param("categoriaId") Integer categoriaId,
            @Param("prioridadId") Integer prioridadId,
            @Param("vencidas") Boolean vencidas,
            @Param("ahora") LocalDateTime ahora,
            Pageable pageable);

    /** HU-09: requests where the technician is the assigned responsible user. */
    @EntityGraph(attributePaths = {"tipoSolicitud", "tipoSolicitud.categoria", "estado", "prioridad"})
    @Query("""
            SELECT s FROM Solicitud s
            WHERE s.responsable.id = :responsableId
              AND (:estadoId IS NULL OR s.estado.id = :estadoId)
            """)
    Page<Solicitud> buscarAsignadas(
            @Param("responsableId") Long responsableId,
            @Param("estadoId") Integer estadoId,
            Pageable pageable);

    @EntityGraph(attributePaths = {"tipoSolicitud", "tipoSolicitud.categoria", "tipoSolicitud.area", "estado",
            "prioridad", "usuario", "responsable"})
    @Query("SELECT s FROM Solicitud s WHERE s.id = :id")
    Optional<Solicitud> buscarDetalle(@Param("id") Long id);

    /** HU-11: loads requests within a date range for metrics and dashboard calculations. */
    @EntityGraph(attributePaths = {"tipoSolicitud", "tipoSolicitud.categoria", "estado", "prioridad", "responsable"})
    @Query("""
            SELECT s FROM Solicitud s
            WHERE (:desde IS NULL OR s.fechaRegistro >= :desde)
              AND (:hasta IS NULL OR s.fechaRegistro <= :hasta)
            """)
    java.util.List<Solicitud> buscarParaDashboard(
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta);
}
