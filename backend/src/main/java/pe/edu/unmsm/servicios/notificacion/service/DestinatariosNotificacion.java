package pe.edu.unmsm.servicios.notificacion.service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * RN-16: pure recipient selection rules for request notifications.
 */
public final class DestinatariosNotificacion {

    private DestinatariosNotificacion() {
    }

    /** Assignment notifies the assigned technician and the requester (deduplicated). */
    public static List<Long> paraAsignacion(Long solicitanteId, Long tecnicoId) {
        Set<Long> destinatarios = new LinkedHashSet<>();
        if (tecnicoId != null) {
            destinatarios.add(tecnicoId);
        }
        if (solicitanteId != null) {
            destinatarios.add(solicitanteId);
        }
        return List.copyOf(destinatarios);
    }

    /** State change notifies the requester and the assigned responsible user, except the actor. */
    public static List<Long> paraCambioEstado(Long solicitanteId, Long responsableId, Long actorId) {
        return involucradosExcluyendoActor(solicitanteId, responsableId, actorId);
    }

    /** Comment notifies the other involved parties (requester, responsible), never the author. */
    public static List<Long> paraComentario(Long solicitanteId, Long responsableId, Long autorId) {
        return involucradosExcluyendoActor(solicitanteId, responsableId, autorId);
    }

    private static List<Long> involucradosExcluyendoActor(Long solicitanteId, Long responsableId, Long actorId) {
        Set<Long> destinatarios = new LinkedHashSet<>();
        if (solicitanteId != null && !Objects.equals(solicitanteId, actorId)) {
            destinatarios.add(solicitanteId);
        }
        if (responsableId != null && !Objects.equals(responsableId, actorId)) {
            destinatarios.add(responsableId);
        }
        return List.copyOf(destinatarios);
    }
}
