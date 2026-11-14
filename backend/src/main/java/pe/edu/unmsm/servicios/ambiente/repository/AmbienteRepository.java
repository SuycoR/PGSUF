package pe.edu.unmsm.servicios.ambiente.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import pe.edu.unmsm.servicios.ambiente.entity.Ambiente;
import pe.edu.unmsm.servicios.ambiente.entity.Pabellon;
import pe.edu.unmsm.servicios.ambiente.entity.TipoAmbiente;

public interface AmbienteRepository extends JpaRepository<Ambiente, Integer> {

    @Query("""
            SELECT a FROM Ambiente a
            WHERE a.activo = true
              AND (:tipo IS NULL OR a.tipo = :tipo)
              AND (:pabellon IS NULL OR a.pabellon = :pabellon)
              AND (:piso IS NULL OR a.piso = :piso)
            ORDER BY a.pabellon, a.piso, a.nombre
            """)
    List<Ambiente> buscarActivosConFiltros(
            @Param("tipo") TipoAmbiente tipo,
            @Param("pabellon") Pabellon pabellon,
            @Param("piso") Integer piso);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Ambiente a WHERE a.id = :id")
    Optional<Ambiente> findByIdForUpdate(@Param("id") Integer id);
}
