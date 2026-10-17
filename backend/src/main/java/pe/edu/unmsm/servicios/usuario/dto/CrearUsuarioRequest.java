package pe.edu.unmsm.servicios.usuario.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** HU-12 (brought forward to S1): the administrator creates users of any role. */
public record CrearUsuarioRequest(
        @NotBlank(message = "es obligatorio") @Size(max = 120, message = "admite hasta 120 caracteres") String nombre,
        @NotBlank(message = "es obligatorio") @Email(message = "no es un correo válido") @Size(max = 120, message = "admite hasta 120 caracteres") String correo,
        @NotBlank(message = "es obligatoria") @Size(min = 8, max = 72, message = "debe tener entre 8 y 72 caracteres") String password,
        @NotBlank(message = "es obligatorio") String rol,
        Integer areaId) {
}
