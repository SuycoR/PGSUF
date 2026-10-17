package pe.edu.unmsm.servicios.usuario.dto;

import pe.edu.unmsm.servicios.usuario.entity.Usuario;

/** Public view of a user: never exposes the password hash. */
public record UsuarioResponse(Long id, String nombre, String correo, String rol, Integer areaId, boolean activo) {

    public static UsuarioResponse de(Usuario usuario) {
        return new UsuarioResponse(
                usuario.getId(),
                usuario.getNombre(),
                usuario.getCorreo(),
                usuario.getRol().getNombre(),
                usuario.getArea() == null ? null : usuario.getArea().getId(),
                usuario.isActivo());
    }
}
