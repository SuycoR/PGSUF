package pe.edu.unmsm.servicios.catalogo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "rol")
public class Rol {

    public static final String SOLICITANTE = "SOLICITANTE";
    public static final String TECNICO = "TECNICO";
    public static final String PERSONAL_AREA = "PERSONAL_AREA";
    public static final String SUPERVISOR = "SUPERVISOR";
    public static final String ADMIN = "ADMIN";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true, length = 30)
    private String nombre;

    protected Rol() {
    }

    public Integer getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    /** RN-10: technicians and area staff belong to an area; everyone else does not. */
    public boolean requiereArea() {
        return TECNICO.equals(nombre) || PERSONAL_AREA.equals(nombre);
    }
}
