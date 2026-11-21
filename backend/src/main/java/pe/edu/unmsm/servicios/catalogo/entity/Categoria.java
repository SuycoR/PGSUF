package pe.edu.unmsm.servicios.catalogo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "categoria")
public class Categoria {

    /** Technical requests go through supervisor evaluation; every other category is an academic procedure. */
    public static final String SOPORTE_TECNICO = "Soporte técnico e infraestructura";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true, length = 80)
    private String nombre;

    @Column(nullable = false)
    private boolean activo = true;

    protected Categoria() {
    }

    public Categoria(String nombre, boolean activo) {
        this.nombre = nombre;
        this.activo = activo;
    }

    public Integer getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }

    /** RN-09: academic procedures (rectifications, enrollment) are routed to their area automatically. */
    public boolean esTramite() {
        return !SOPORTE_TECNICO.equals(nombre);
    }
}
