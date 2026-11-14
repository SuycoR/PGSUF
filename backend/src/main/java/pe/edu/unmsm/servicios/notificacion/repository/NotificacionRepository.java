package pe.edu.unmsm.servicios.notificacion.repository;

import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import pe.edu.unmsm.servicios.notificacion.entity.Notificacion;

public interface NotificacionRepository extends JpaRepository<Notificacion, Long> {

    @Query("""
            SELECT n FROM Notificacion n
            WHERE n.usuario.id = :usuarioId
              AND (:leida IS NULL OR n.leida = :leida)
            """)
    Page<Notificacion> buscarDelUsuario(
            @Param("usuarioId") Long usuarioId,
            @Param("leida") Boolean leida,
            Pageable pageable);

    long countByUsuarioIdAndLeidaFalse(Long usuarioId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE Notificacion n
            SET n.leida = true, n.fechaLectura = :ahora
            WHERE n.usuario.id = :usuarioId AND n.leida = false
            """)
    int marcarTodasComoLeidas(@Param("usuarioId") Long usuarioId, @Param("ahora") LocalDateTime ahora);
}
