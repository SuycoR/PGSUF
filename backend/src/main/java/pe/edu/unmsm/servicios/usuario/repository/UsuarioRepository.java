package pe.edu.unmsm.servicios.usuario.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import pe.edu.unmsm.servicios.usuario.entity.Usuario;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByCorreo(String correo);

    boolean existsByCorreo(String correo);

    boolean existsByCorreoAndIdNot(String correo, Long id);

    boolean existsByRolNombre(String rolNombre);

    @EntityGraph(attributePaths = {"rol", "area"})
    @Query("""
            SELECT u FROM Usuario u
            WHERE u.rol.nombre = :rol
              AND u.activo = true
              AND (:areaId IS NULL OR u.area.id = :areaId)
            ORDER BY u.nombre ASC
            """)
    List<Usuario> buscarActivosPorRolYArea(@Param("rol") String rol, @Param("areaId") Integer areaId);
}
