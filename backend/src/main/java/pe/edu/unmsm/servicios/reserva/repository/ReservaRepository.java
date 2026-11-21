package pe.edu.unmsm.servicios.reserva.repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import pe.edu.unmsm.servicios.reserva.entity.Reserva;

public interface ReservaRepository extends JpaRepository<Reserva, Long> {

    @Query("""
            SELECT COUNT(r) > 0 FROM Reserva r
            WHERE r.ambiente.id = :ambienteId
              AND r.fecha = :fecha
              AND r.estado.nombre = 'Aprobada'
              AND r.horaInicio < :horaFin
              AND r.horaFin > :horaInicio
              AND (:excluirId IS NULL OR r.id != :excluirId)
            """)
    boolean existeCruce(
            @Param("ambienteId") Integer ambienteId,
            @Param("fecha") LocalDate fecha,
            @Param("horaInicio") LocalTime horaInicio,
            @Param("horaFin") LocalTime horaFin,
            @Param("excluirId") Long excluirId);

    @Query("""
            SELECT r FROM Reserva r
            WHERE r.ambiente.id = :ambienteId
              AND r.fecha = :fecha
              AND r.estado.nombre = 'Aprobada'
            ORDER BY r.horaInicio
            """)
    List<Reserva> findAprobadasPorAmbienteYFecha(
            @Param("ambienteId") Integer ambienteId,
            @Param("fecha") LocalDate fecha);

    @Query(value = """
            SELECT r FROM Reserva r
            JOIN FETCH r.ambiente a
            JOIN FETCH r.estado e
            WHERE r.usuario.id = :usuarioId
            ORDER BY r.fecha DESC, r.horaInicio DESC
            """,
            countQuery = """
            SELECT COUNT(r) FROM Reserva r
            WHERE r.usuario.id = :usuarioId
            """)
    Page<Reserva> buscarMisReservas(
            @Param("usuarioId") Long usuarioId,
            Pageable pageable);

    @Query(value = """
            SELECT r FROM Reserva r
            JOIN FETCH r.ambiente a
            JOIN FETCH r.usuario u
            JOIN FETCH r.estado e
            LEFT JOIN FETCH r.aprobador ap
            WHERE (:estadoId IS NULL OR e.id = :estadoId)
              AND (:ambienteId IS NULL OR a.id = :ambienteId)
              AND (:fecha IS NULL OR r.fecha = :fecha)
            ORDER BY r.fecha DESC, r.horaInicio DESC
            """,
            countQuery = """
            SELECT COUNT(r) FROM Reserva r
            WHERE (:estadoId IS NULL OR r.estado.id = :estadoId)
              AND (:ambienteId IS NULL OR r.ambiente.id = :ambienteId)
              AND (:fecha IS NULL OR r.fecha = :fecha)
            """)
    Page<Reserva> buscarBandeja(
            @Param("estadoId") Integer estadoId,
            @Param("ambienteId") Integer ambienteId,
            @Param("fecha") LocalDate fecha,
            Pageable pageable);

    @Query("""
            SELECT r FROM Reserva r
            JOIN FETCH r.ambiente a
            JOIN FETCH r.usuario u
            JOIN FETCH r.estado e
            LEFT JOIN FETCH r.aprobador ap
            WHERE r.id = :id
            """)
    Optional<Reserva> buscarDetalle(@Param("id") Long id);
}
