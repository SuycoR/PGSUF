package pe.edu.unmsm.servicios.notificacion.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import pe.edu.unmsm.servicios.usuario.entity.Usuario;

/** HU-17 / RN-16: in-system notification for a single user. */
@Entity
@Table(name = "notificacion")
public class Notificacion {

    private static final int MAX_TITULO = 120;
    private static final int MAX_MENSAJE = 500;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TipoNotificacion tipo;

    @Column(nullable = false, length = MAX_TITULO)
    private String titulo;

    @Column(nullable = false, length = MAX_MENSAJE)
    private String mensaje;

    @Column(name = "solicitud_id")
    private Long solicitudId;

    @Column(name = "reserva_id")
    private Long reservaId;

    @Column(nullable = false)
    private boolean leida = false;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_lectura")
    private LocalDateTime fechaLectura;

    protected Notificacion() {
    }

    private Notificacion(Usuario usuario, TipoNotificacion tipo, String titulo, String mensaje,
            Long solicitudId, Long reservaId, LocalDateTime fechaCreacion) {
        this.usuario = usuario;
        this.tipo = tipo;
        this.titulo = truncar(titulo, MAX_TITULO);
        this.mensaje = truncar(mensaje, MAX_MENSAJE);
        this.solicitudId = solicitudId;
        this.reservaId = reservaId;
        this.fechaCreacion = fechaCreacion;
    }

    public static Notificacion paraSolicitud(Usuario usuario, TipoNotificacion tipo, String titulo, String mensaje,
            Long solicitudId, LocalDateTime fechaCreacion) {
        return new Notificacion(usuario, tipo, titulo, mensaje, solicitudId, null, fechaCreacion);
    }

    public void marcarLeida(LocalDateTime cuando) {
        if (!this.leida) {
            this.leida = true;
            this.fechaLectura = cuando;
        }
    }

    private static String truncar(String valor, int max) {
        if (valor == null) {
            return "";
        }
        return valor.length() <= max ? valor : valor.substring(0, max - 3) + "...";
    }

    public Long getId() {
        return id;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public TipoNotificacion getTipo() {
        return tipo;
    }

    public String getTitulo() {
        return titulo;
    }

    public String getMensaje() {
        return mensaje;
    }

    public Long getSolicitudId() {
        return solicitudId;
    }

    public Long getReservaId() {
        return reservaId;
    }

    public boolean isLeida() {
        return leida;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public LocalDateTime getFechaLectura() {
        return fechaLectura;
    }
}
