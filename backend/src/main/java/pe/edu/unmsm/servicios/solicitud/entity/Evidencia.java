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

import pe.edu.unmsm.servicios.usuario.entity.Usuario;

@Entity
@Table(name = "evidencia")
public class Evidencia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "solicitud_id", nullable = false)
    private Solicitud solicitud;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    /** Original file name, for display only. */
    @Column(name = "nombre_archivo", nullable = false, length = 255)
    private String nombreArchivo;

    /** Generated path relative to the upload directory; never derived from user input. */
    @Column(nullable = false, length = 255)
    private String ruta;

    @Column(name = "tipo_mime", nullable = false, length = 50)
    private String tipoMime;

    @Column(name = "tamano_bytes", nullable = false)
    private int tamanoBytes;

    @Column(name = "fecha_subida", nullable = false)
    private LocalDateTime fechaSubida;

    protected Evidencia() {
    }

    public Evidencia(Solicitud solicitud, Usuario usuario, String nombreArchivo, String ruta, String tipoMime,
            int tamanoBytes, LocalDateTime fechaSubida) {
        this.solicitud = solicitud;
        this.usuario = usuario;
        this.nombreArchivo = nombreArchivo;
        this.ruta = ruta;
        this.tipoMime = tipoMime;
        this.tamanoBytes = tamanoBytes;
        this.fechaSubida = fechaSubida;
    }

    public Long getId() {
        return id;
    }

    public Solicitud getSolicitud() {
        return solicitud;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public String getNombreArchivo() {
        return nombreArchivo;
    }

    public String getRuta() {
        return ruta;
    }

    public String getTipoMime() {
        return tipoMime;
    }

    public int getTamanoBytes() {
        return tamanoBytes;
    }

    public LocalDateTime getFechaSubida() {
        return fechaSubida;
    }
}
