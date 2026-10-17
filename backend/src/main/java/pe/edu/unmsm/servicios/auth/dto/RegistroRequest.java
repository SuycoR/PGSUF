package pe.edu.unmsm.servicios.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Password limit of 72: BCrypt ignores anything beyond 72 bytes. */
public record RegistroRequest(
        @NotBlank(message = "es obligatorio") @Size(max = 120, message = "admite hasta 120 caracteres") String nombre,
        @NotBlank(message = "es obligatorio") @Email(message = "no es un correo válido") @Size(max = 120, message = "admite hasta 120 caracteres") String correo,
        @NotBlank(message = "es obligatoria") @Size(min = 8, max = 72, message = "debe tener entre 8 y 72 caracteres") String password) {
}
