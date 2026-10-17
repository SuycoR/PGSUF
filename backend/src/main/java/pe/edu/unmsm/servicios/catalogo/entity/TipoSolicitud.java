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

@Entity
@Table(name = "tipo_solicitud")
public class TipoSolicitud {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "categoria_id", nullable = false)
    private Categoria categoria;

    /** Area the request is routed to. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "area_id", nullable = false)
    private Area area;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(name = "requiere_adjunto", nullable = false)
    private boolean requiereAdjunto;

    @Column(name = "sla_horas", nullable = false)
    private int slaHoras;

    @Column(nullable = false)
    private boolean activo = true;

    protected TipoSolicitud() {
    }

    public Integer getId() {
        return id;
    }

    public Categoria getCategoria() {
        return categoria;
    }

    public Area getArea() {
        return area;
    }

    public String getNombre() {
        return nombre;
    }

    public boolean isRequiereAdjunto() {
        return requiereAdjunto;
    }

    public int getSlaHoras() {
        return slaHoras;
    }

    public boolean isActivo() {
        return activo;
    }

    /** A type can be used only if both the type and its category are active. */
    public boolean disponible() {
        return activo && categoria.isActivo();
    }
}
