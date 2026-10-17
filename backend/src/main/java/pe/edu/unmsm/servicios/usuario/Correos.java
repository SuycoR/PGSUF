package pe.edu.unmsm.servicios.usuario;

import java.util.Locale;

/** Emails are stored and compared in lowercase so "Ana@" and "ana@" are the same account. */
public final class Correos {

    private Correos() {
    }

    public static String normalizar(String correo) {
        return correo == null ? null : correo.trim().toLowerCase(Locale.ROOT);
    }
}
