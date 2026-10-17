package pe.edu.unmsm.servicios.solicitud.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import pe.edu.unmsm.servicios.catalogo.entity.Estado;
import pe.edu.unmsm.servicios.usuario.entity.Usuario;

/** RN-14: one row per state change, including the initial registration. */
@Entity
@Table(name = "historial_estado")
public class HistorialEstado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "solicitud_id", nullable = false)
    private Solicitud solicitud;

    /** Null in the initial registration. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "estado_anterior_id")
    private Estado estadoAnterior;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "estado_nuevo_id", nullable = false)
    private Estado estadoNuevo;

    /** Null when the system made the change (RN-09). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @Column(length = 500)
    private String observacion;

    @Column(nullable = false)
    private LocalDateTime fecha;

    protected HistorialEstado() {
    }

    public HistorialEstado(Solicitud solicitud, Estado estadoAnterior, Estado estadoNuevo, Usuario usuario,
            String observacion, LocalDateTime fecha) {
        this.solicitud = solicitud;
        this.estadoAnterior = estadoAnterior;
        this.estadoNuevo = estadoNuevo;
        this.usuario = usuario;
        this.observacion = observacion;
        this.fecha = fecha;
    }

    public Long getId() {
        return id;
    }

    public Solicitud getSolicitud() {
        return solicitud;
    }

    public Estado getEstadoAnterior() {
        return estadoAnterior;
    }

    public Estado getEstadoNuevo() {
        return estadoNuevo;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public String getObservacion() {
        return observacion;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }
}
