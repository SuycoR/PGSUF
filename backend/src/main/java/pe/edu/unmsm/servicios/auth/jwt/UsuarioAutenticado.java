package pe.edu.unmsm.servicios.auth.jwt;

/** Principal stored in the security context for each request, built from the JWT claims. */
public record UsuarioAutenticado(Long id, String correo, String rol) {
}
