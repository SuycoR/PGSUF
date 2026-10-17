package pe.edu.unmsm.servicios.solicitud;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import pe.edu.unmsm.servicios.solicitud.dto.SolicitudDtos.CrearSolicitudRequest;
import pe.edu.unmsm.servicios.solicitud.service.SolicitudService;
import pe.edu.unmsm.servicios.usuario.service.UsuarioService;

/**
 * RN-01 under concurrency. Not transactional on purpose: each registration must commit on its own
 * connection to compete for the sequence row, so this test cleans up its own data.
 */
@SpringBootTest
@ActiveProfiles("test")
class CodigoConcurrenciaTest {

    private static final int HILOS = 10;

    @Autowired
    private SolicitudService solicitudService;
    @Autowired
    private UsuarioService usuarioService;
    @Autowired
    private JdbcTemplate jdbc;

    private Long usuarioId;

    @BeforeEach
    void setUp() {
        usuarioId = usuarioService.crear("Concurrencia", "concurrencia@unmsm.edu.pe", "Clave12345", "SOLICITANTE", null)
                .getId();
    }

    @AfterEach
    void limpiar() {
        jdbc.update("DELETE FROM historial_estado WHERE solicitud_id IN (SELECT id FROM solicitud WHERE usuario_id = ?)", usuarioId);
        jdbc.update("DELETE FROM solicitud WHERE usuario_id = ?", usuarioId);
        jdbc.update("DELETE FROM usuario WHERE id = ?", usuarioId);
    }

    @Test
    void parallelRegistrationsGetDistinctCodes() throws Exception {
        int tipo = jdbc.queryForObject("SELECT id FROM tipo_solicitud WHERE nombre = 'Red o wifi'", Integer.class);
        int prioridad = jdbc.queryForObject("SELECT id FROM prioridad WHERE nombre = 'Alta'", Integer.class);
        CrearSolicitudRequest datos = new CrearSolicitudRequest(tipo, prioridad, "Sin red", "Salón N-101");

        CountDownLatch salida = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(HILOS);
        List<Future<String>> resultados = new ArrayList<>();
        for (int i = 0; i < HILOS; i++) {
            Callable<String> tarea = () -> {
                salida.await();
                return solicitudService.registrar(usuarioId, datos, null).codigo();
            };
            resultados.add(pool.submit(tarea));
        }
        salida.countDown();

        List<String> codigos = new ArrayList<>();
        for (Future<String> resultado : resultados) {
            codigos.add(resultado.get());
        }
        pool.shutdown();

        assertThat(codigos).hasSize(HILOS).doesNotHaveDuplicates();
    }
}
