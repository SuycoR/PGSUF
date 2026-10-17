package pe.edu.unmsm.servicios.solicitud.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * RN-01: unique yearly codes such as SOL-2026-0001.
 *
 * A single "INSERT ... ON DUPLICATE KEY UPDATE" both creates the year's row and increments it while
 * holding an exclusive row lock, so concurrent registrations never get the same number. LAST_INSERT_ID(expr)
 * stores the new value for this connection only. Must run inside the caller's transaction: if it rolls back,
 * the number is released.
 */
@Service
public class SecuenciaCodigoService {

    private final JdbcTemplate jdbc;

    public SecuenciaCodigoService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public String siguiente(String prefijo, int anio) {
        jdbc.update("""
                INSERT INTO secuencia_codigo (prefijo, anio, ultimo_valor) VALUES (?, ?, LAST_INSERT_ID(1))
                ON DUPLICATE KEY UPDATE ultimo_valor = LAST_INSERT_ID(ultimo_valor + 1)
                """, prefijo, anio);
        Integer valor = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Integer.class);
        return formatear(prefijo, anio, valor);
    }

    static String formatear(String prefijo, int anio, int valor) {
        return "%s-%d-%04d".formatted(prefijo, anio, valor);
    }
}
