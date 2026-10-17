package pe.edu.unmsm.servicios.auth.dto;

import pe.edu.unmsm.servicios.usuario.dto.UsuarioResponse;

public record LoginResponse(String token, UsuarioResponse usuario) {
}
