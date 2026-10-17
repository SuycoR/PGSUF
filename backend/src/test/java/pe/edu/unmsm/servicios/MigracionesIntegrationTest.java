package pe.edu.unmsm.servicios;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

@IntegrationTest
class MigracionesIntegrationTest {

    @Autowired
    private JdbcTemplate jdbc;

    private int contar(String sql) {
        return jdbc.queryForObject(sql, Integer.class);
    }

    @Test
    void seedsTheCatalogs() {
        assertThat(contar("SELECT COUNT(*) FROM rol")).isEqualTo(5);
        assertThat(contar("SELECT COUNT(*) FROM area")).isEqualTo(3);
        assertThat(contar("SELECT COUNT(*) FROM tipo_solicitud")).isEqualTo(11);
        assertThat(contar("SELECT COUNT(*) FROM estado")).isEqualTo(8);
        assertThat(contar("SELECT COUNT(*) FROM estado_transicion")).isEqualTo(12);
    }

    @Test
    void theOnlySystemTransitionIsTheDerivationOfAcademicProcedures() {
        assertThat(jdbc.queryForList("""
                SELECT CONCAT(o.nombre, ' -> ', d.nombre) FROM estado_transicion t
                JOIN estado o ON o.id = t.estado_origen_id JOIN estado d ON d.id = t.estado_destino_id
                WHERE t.rol_id IS NULL""", String.class))
                .containsExactly("Registrada -> En evaluación");
    }

    @Test
    void seedsTheRoomsOfBothPavilionsAndTheAuditorium() {
        assertThat(contar("SELECT COUNT(*) FROM ambiente WHERE tipo = 'SALON'")).isEqualTo(60);
        assertThat(contar("SELECT COUNT(*) FROM ambiente WHERE tipo = 'LABORATORIO'")).isEqualTo(60);
        assertThat(contar("SELECT capacidad FROM ambiente WHERE tipo = 'AUDITORIO'")).isEqualTo(300);
        assertThat(contar("SELECT COUNT(*) FROM ambiente WHERE nombre IN ('Salón A-101', 'Laboratorio N-415')")).isEqualTo(2);
    }

    @Test
    void seedsNoUsers() {
        assertThat(contar("SELECT COUNT(*) FROM usuario")).isZero();
    }
}
