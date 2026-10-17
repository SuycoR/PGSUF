package pe.edu.unmsm.servicios.catalogo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "estado")
public class Estado {

    public static final String REGISTRADA = "Registrada";
    public static final String EN_EVALUACION = "En evaluación";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true, length = 30)
    private String nombre;

    /** SOLICITUD, RESERVA or AMBOS (RN-11). */
    @Column(nullable = false, length = 10)
    private String ambito;

    @Column(nullable = false)
    private int orden;

    @Column(name = "es_final", nullable = false)
    private boolean esFinal;

    protected Estado() {
    }

    public Integer getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public String getAmbito() {
        return ambito;
    }

    public int getOrden() {
        return orden;
    }

    public boolean isEsFinal() {
        return esFinal;
    }
}
