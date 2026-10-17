package pe.edu.unmsm.servicios.solicitud.service;

import java.util.Objects;

import org.springframework.stereotype.Component;

import pe.edu.unmsm.servicios.catalogo.entity.Rol;
import pe.edu.unmsm.servicios.solicitud.entity.Solicitud;
import pe.edu.unmsm.servicios.usuario.entity.Usuario;

/**
 * RN-20: who can see a request. Anyone who cannot see it gets a 404, so its existence is not revealed.
 */
@Component
public class VisibilidadSolicitudes {

    public boolean puedeVer(Usuario usuario, Solicitud solicitud) {
        if (Objects.equals(solicitud.getUsuario().getId(), usuario.getId())) {
            return true; // the requester always sees their own requests
        }
        return switch (usuario.getRol().getNombre()) {
            case Rol.ADMIN -> true;
            case Rol.SUPERVISOR -> !solicitud.getTipoSolicitud().getCategoria().esTramite();
            case Rol.PERSONAL_AREA -> usuario.getArea() != null
                    && Objects.equals(usuario.getArea().getId(), solicitud.getTipoSolicitud().getArea().getId());
            case Rol.TECNICO -> solicitud.getResponsable() != null
                    && Objects.equals(solicitud.getResponsable().getId(), usuario.getId());
            default -> false;
        };
    }
}
