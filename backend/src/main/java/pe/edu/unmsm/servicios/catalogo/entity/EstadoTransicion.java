package pe.edu.unmsm.servicios.catalogo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** RN-05 & RN-11: allowed state transitions per role and scope (SOLICITUD or RESERVA). */
@Entity
@Table(name = "estado_transicion")
public class EstadoTransicion {

    public static final String AMBITO_SOLICITUD = "SOLICITUD";
    public static final String AMBITO_RESERVA = "RESERVA";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "estado_origen_id", nullable = false)
    private Estado estadoOrigen;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "estado_destino_id", nullable = false)
    private Estado estadoDestino;

    /** Null when executed automatically by the system (RN-09). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rol_id")
    private Rol rol;

    @Column(nullable = false, length = 10)
    private String ambito;

    @Column(name = "requiere_motivo", nullable = false)
    private boolean requiereMotivo;

    protected EstadoTransicion() {
    }

    public Integer getId() {
        return id;
    }

    public Estado getEstadoOrigen() {
        return estadoOrigen;
    }

    public Estado getEstadoDestino() {
        return estadoDestino;
    }

    public Rol getRol() {
        return rol;
    }

    public String getAmbito() {
        return ambito;
    }

    public boolean isRequiereMotivo() {
        return requiereMotivo;
    }
}
