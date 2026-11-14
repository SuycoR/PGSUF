package pe.edu.unmsm.servicios.ambiente.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "ambiente")
public class Ambiente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true, length = 80)
    private String nombre;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private TipoAmbiente tipo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Pabellon pabellon;

    @Column
    private Integer piso;

    @Column(nullable = false)
    private int capacidad;

    @Column(nullable = false)
    private boolean activo = true;

    protected Ambiente() {
    }

    public Ambiente(String nombre, TipoAmbiente tipo, Pabellon pabellon, Integer piso, int capacidad, boolean activo) {
        this.nombre = nombre;
        this.tipo = tipo;
        this.pabellon = pabellon;
        this.piso = piso;
        this.capacidad = capacidad;
        this.activo = activo;
    }

    public Integer getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public TipoAmbiente getTipo() {
        return tipo;
    }

    public Pabellon getPabellon() {
        return pabellon;
    }

    public Integer getPiso() {
        return piso;
    }

    public int getCapacidad() {
        return capacidad;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }
}
