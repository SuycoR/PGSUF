package pe.edu.unmsm.servicios.reserva.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import pe.edu.unmsm.servicios.ambiente.entity.Ambiente;
import pe.edu.unmsm.servicios.catalogo.entity.Estado;
import pe.edu.unmsm.servicios.usuario.entity.Usuario;

@Entity
@Table(name = "reserva")
public class Reserva {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String codigo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ambiente_id", nullable = false)
    private Ambiente ambiente;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "aprobador_id")
    private Usuario aprobador;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "estado_id", nullable = false)
    private Estado estado;

    @Column(nullable = false)
    private LocalDate fecha;

    @Column(name = "hora_inicio", nullable = false)
    private LocalTime horaInicio;

    @Column(name = "hora_fin", nullable = false)
    private LocalTime horaFin;

    @Column(nullable = false, length = 255)
    private String motivo;

    @Column(name = "motivo_rechazo", length = 255)
    private String motivoRechazo;

    @Column(name = "fecha_registro", nullable = false)
    private LocalDateTime fechaRegistro;

    @Column(name = "fecha_resolucion")
    private LocalDateTime fechaResolucion;

    protected Reserva() {
    }

    public Reserva(String codigo, Ambiente ambiente, Usuario usuario, Estado estado,
                   LocalDate fecha, LocalTime horaInicio, LocalTime horaFin, String motivo,
                   LocalDateTime fechaRegistro) {
        this.codigo = codigo;
        this.ambiente = ambiente;
        this.usuario = usuario;
        this.estado = estado;
        this.fecha = fecha;
        this.horaInicio = horaInicio;
        this.horaFin = horaFin;
        this.motivo = motivo;
        this.fechaRegistro = fechaRegistro;
    }

    public void aprobar(Usuario aprobador, Estado estadoAprobada, LocalDateTime fechaResolucion) {
        this.aprobador = aprobador;
        this.estado = estadoAprobada;
        this.fechaResolucion = fechaResolucion;
    }

    public void rechazar(Usuario aprobador, Estado estadoRechazada, String motivoRechazo, LocalDateTime fechaResolucion) {
        this.aprobador = aprobador;
        this.estado = estadoRechazada;
        this.motivoRechazo = motivoRechazo;
        this.fechaResolucion = fechaResolucion;
    }

    public Long getId() {
        return id;
    }

    public String getCodigo() {
        return codigo;
    }

    public Ambiente getAmbiente() {
        return ambiente;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public Usuario getAprobador() {
        return aprobador;
    }

    public Estado getEstado() {
        return estado;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public LocalTime getHoraInicio() {
        return horaInicio;
    }

    public LocalTime getHoraFin() {
        return horaFin;
    }

    public String getMotivo() {
        return motivo;
    }

    public String getMotivoRechazo() {
        return motivoRechazo;
    }

    public LocalDateTime getFechaRegistro() {
        return fechaRegistro;
    }

    public LocalDateTime getFechaResolucion() {
        return fechaResolucion;
    }
}
