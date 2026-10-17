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
import pe.edu.unmsm.servicios.catalogo.entity.Prioridad;
import pe.edu.unmsm.servicios.catalogo.entity.TipoSolicitud;
import pe.edu.unmsm.servicios.usuario.entity.Usuario;

@Entity
@Table(name = "solicitud")
public class Solicitud {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String codigo;

    /** Requester. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tipo_solicitud_id", nullable = false)
    private TipoSolicitud tipoSolicitud;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "prioridad_id", nullable = false)
    private Prioridad prioridad;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "estado_id", nullable = false)
    private Estado estado;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "responsable_id")
    private Usuario responsable;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String descripcion;

    @Column(nullable = false, length = 150)
    private String ubicacion;

    @Column(columnDefinition = "TEXT")
    private String respuesta;

    @Column(name = "fecha_registro", nullable = false)
    private LocalDateTime fechaRegistro;

    @Column(name = "fecha_limite", nullable = false)
    private LocalDateTime fechaLimite;

    @Column(name = "fecha_cierre")
    private LocalDateTime fechaCierre;

    protected Solicitud() {
    }

    public Solicitud(String codigo, Usuario usuario, TipoSolicitud tipoSolicitud, Prioridad prioridad, Estado estado,
            String descripcion, String ubicacion, LocalDateTime fechaRegistro) {
        this.codigo = codigo;
        this.usuario = usuario;
        this.tipoSolicitud = tipoSolicitud;
        this.prioridad = prioridad;
        this.estado = estado;
        this.descripcion = descripcion;
        this.ubicacion = ubicacion;
        this.fechaRegistro = fechaRegistro;
        this.fechaLimite = calcularFechaLimite(fechaRegistro, tipoSolicitud.getSlaHoras());
    }

    /** RN-02: registration date plus the SLA of the type, in calendar hours. */
    public static LocalDateTime calcularFechaLimite(LocalDateTime fechaRegistro, int slaHoras) {
        return fechaRegistro.plusHours(slaHoras);
    }

    /** RN-03: overdue when the deadline has passed and the request is not in a final state. */
    public boolean estaVencida(LocalDateTime ahora) {
        return !estado.isEsFinal() && fechaLimite.isBefore(ahora);
    }

    public void cambiarEstado(Estado nuevo, LocalDateTime cuando) {
        this.estado = nuevo;
        if (nuevo.isEsFinal()) {
            this.fechaCierre = cuando; // RN-15
        }
    }

    public Long getId() {
        return id;
    }

    public String getCodigo() {
        return codigo;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public TipoSolicitud getTipoSolicitud() {
        return tipoSolicitud;
    }

    public Prioridad getPrioridad() {
        return prioridad;
    }

    public Estado getEstado() {
        return estado;
    }

    public Usuario getResponsable() {
        return responsable;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public String getUbicacion() {
        return ubicacion;
    }

    public String getRespuesta() {
        return respuesta;
    }

    public LocalDateTime getFechaRegistro() {
        return fechaRegistro;
    }

    public LocalDateTime getFechaLimite() {
        return fechaLimite;
    }

    public LocalDateTime getFechaCierre() {
        return fechaCierre;
    }
}
