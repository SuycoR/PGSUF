package pe.edu.unmsm.servicios.comentario.dto;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import pe.edu.unmsm.servicios.comentario.entity.Comentario;

/** Request and response contracts for request comments (HU-10, docs/07-api.md). */
public final class ComentarioDtos {

    private ComentarioDtos() {
    }

    public record CrearComentarioRequest(
            @NotBlank(message = "es obligatorio")
            @Size(max = 2000, message = "admite hasta 2000 caracteres")
            String texto) {
    }

    public record ComentarioResponse(
            Long id,
            Long autorId,
            String autor,
            String autorRol,
            String texto,
            LocalDateTime fecha) {

        public static ComentarioResponse de(Comentario c) {
            return new ComentarioResponse(
                    c.getId(),
                    c.getUsuario().getId(),
                    c.getUsuario().getNombre(),
                    c.getUsuario().getRol().getNombre(),
                    c.getTexto(),
                    c.getFecha());
        }
    }
}
