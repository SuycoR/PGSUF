package pe.edu.unmsm.servicios.solicitud.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

import pe.edu.unmsm.servicios.catalogo.entity.Area;
import pe.edu.unmsm.servicios.catalogo.entity.Categoria;
import pe.edu.unmsm.servicios.catalogo.entity.Rol;
import pe.edu.unmsm.servicios.catalogo.entity.TipoSolicitud;
import pe.edu.unmsm.servicios.solicitud.entity.Solicitud;
import pe.edu.unmsm.servicios.usuario.entity.Usuario;

class VisibilidadSolicitudesTest {

    private final VisibilidadSolicitudes visibilidad = new VisibilidadSolicitudes();

    private static Area area(int id) {
        Area area = mock(Area.class);
        when(area.getId()).thenReturn(id);
        return area;
    }

    private static Usuario usuario(long id, String rol, Area area) {
        Rol r = mock(Rol.class);
        when(r.getNombre()).thenReturn(rol);
        Usuario u = mock(Usuario.class);
        when(u.getId()).thenReturn(id);
        when(u.getRol()).thenReturn(r);
        when(u.getArea()).thenReturn(area);
        return u;
    }

    private static Solicitud solicitud(Usuario duenio, boolean tramite, Area area, Usuario responsable) {
        Categoria categoria = mock(Categoria.class);
        when(categoria.esTramite()).thenReturn(tramite);
        TipoSolicitud tipo = mock(TipoSolicitud.class);
        when(tipo.getCategoria()).thenReturn(categoria);
        when(tipo.getArea()).thenReturn(area);
        Solicitud s = mock(Solicitud.class);
        when(s.getUsuario()).thenReturn(duenio);
        when(s.getTipoSolicitud()).thenReturn(tipo);
        when(s.getResponsable()).thenReturn(responsable);
        return s;
    }

    private final Usuario ana = usuario(1, Rol.SOLICITANTE, null);
    private final Usuario beto = usuario(2, Rol.SOLICITANTE, null);
    private final Usuario supervisor = usuario(3, Rol.SUPERVISOR, null);
    private final Usuario secretaria = usuario(4, Rol.PERSONAL_AREA, area(2));
    private final Usuario matricula = usuario(5, Rol.PERSONAL_AREA, area(3));
    private final Usuario tecnico = usuario(6, Rol.TECNICO, area(1));
    private final Usuario admin = usuario(7, Rol.ADMIN, null);

    @Test
    void theRequesterSeesOnlyTheirOwn() {
        Solicitud deAna = solicitud(ana, false, area(1), null);

        assertThat(visibilidad.puedeVer(ana, deAna)).isTrue();
        assertThat(visibilidad.puedeVer(beto, deAna)).isFalse();
    }

    @Test
    void theSupervisorSeesTechnicalRequestsButNotProcedures() {
        assertThat(visibilidad.puedeVer(supervisor, solicitud(ana, false, area(1), null))).isTrue();
        assertThat(visibilidad.puedeVer(supervisor, solicitud(ana, true, area(2), null))).isFalse();
    }

    @Test
    void areaStaffSeeOnlyTheirArea() {
        Solicitud rectificacion = solicitud(ana, true, area(2), null);

        assertThat(visibilidad.puedeVer(secretaria, rectificacion)).isTrue();
        assertThat(visibilidad.puedeVer(matricula, rectificacion)).isFalse();
    }

    @Test
    void theTechnicianSeesOnlyAssignedRequests() {
        assertThat(visibilidad.puedeVer(tecnico, solicitud(ana, false, area(1), tecnico))).isTrue();
        assertThat(visibilidad.puedeVer(tecnico, solicitud(ana, false, area(1), null))).isFalse();
    }

    @Test
    void theAdminSeesEverything() {
        assertThat(visibilidad.puedeVer(admin, solicitud(ana, true, area(3), null))).isTrue();
    }
}
