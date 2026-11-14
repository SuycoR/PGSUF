package pe.edu.unmsm.servicios.solicitud.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import pe.edu.unmsm.servicios.catalogo.entity.Estado;
import pe.edu.unmsm.servicios.catalogo.entity.EstadoTransicion;
import pe.edu.unmsm.servicios.catalogo.entity.Rol;
import pe.edu.unmsm.servicios.catalogo.repository.EstadoRepository;
import pe.edu.unmsm.servicios.catalogo.repository.EstadoTransicionRepository;
import pe.edu.unmsm.servicios.common.BusinessException;

class TransicionServiceTest {

    private final EstadoTransicionRepository transiciones = mock(EstadoTransicionRepository.class);
    private final EstadoRepository estados = mock(EstadoRepository.class);
    private final TransicionService service = new TransicionService(transiciones, estados);

    private static Estado estado(int id, String nombre) {
        Estado e = mock(Estado.class);
        when(e.getId()).thenReturn(id);
        when(e.getNombre()).thenReturn(nombre);
        return e;
    }

    private static EstadoTransicion transicion(Estado origen, Estado destino, boolean requiereMotivo) {
        EstadoTransicion t = mock(EstadoTransicion.class);
        when(t.getEstadoOrigen()).thenReturn(origen);
        when(t.getEstadoDestino()).thenReturn(destino);
        when(t.isRequiereMotivo()).thenReturn(requiereMotivo);
        return t;
    }

    @Test
    void disponiblesReturnsTransitionsFromRepository() {
        Estado enEvaluacion = estado(2, Estado.EN_EVALUACION);
        Estado asignada = estado(3, Estado.ASIGNADA);
        EstadoTransicion esperada = transicion(enEvaluacion, asignada, false);
        when(transiciones.findByEstadoOrigenIdAndRolNombreAndAmbitoOrderByEstadoDestinoOrden(
                2, Rol.SUPERVISOR, EstadoTransicion.AMBITO_SOLICITUD))
                .thenReturn(List.of(esperada));

        List<EstadoTransicion> resultado = service.disponibles(
                enEvaluacion, Rol.SUPERVISOR, EstadoTransicion.AMBITO_SOLICITUD);

        assertThat(resultado).containsExactly(esperada);
    }

    @Test
    void validarAcceptsAllowedTransition() {
        Estado asignada = estado(3, Estado.ASIGNADA);
        Estado enAtencion = estado(4, Estado.EN_ATENCION);
        EstadoTransicion esperada = transicion(asignada, enAtencion, false);
        when(estados.findById(4)).thenReturn(Optional.of(enAtencion));
        when(transiciones.findByEstadoOrigenIdAndEstadoDestinoIdAndRolNombreAndAmbito(
                3, 4, Rol.TECNICO, EstadoTransicion.AMBITO_SOLICITUD))
                .thenReturn(Optional.of(esperada));

        EstadoTransicion resultado = service.validar(
                asignada, 4, Rol.TECNICO, EstadoTransicion.AMBITO_SOLICITUD, null);

        assertThat(resultado).isSameAs(esperada);
    }

    @Test
    void validarRejectsUnknownTargetStateWith422() {
        Estado registrada = estado(1, Estado.REGISTRADA);
        when(estados.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.validar(
                registrada, 99, Rol.SUPERVISOR, EstadoTransicion.AMBITO_SOLICITUD, null))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
                    assertThat(be.getCodigo()).isEqualTo("ESTADO_INVALIDO");
                });
    }

    @Test
    void validarRejectsDisallowedTransitionWith422() {
        Estado registrada = estado(1, Estado.REGISTRADA);
        Estado resuelta = estado(5, Estado.RESUELTA);
        when(estados.findById(5)).thenReturn(Optional.of(resuelta));
        when(transiciones.findByEstadoOrigenIdAndEstadoDestinoIdAndRolNombreAndAmbito(
                1, 5, Rol.TECNICO, EstadoTransicion.AMBITO_SOLICITUD))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.validar(
                registrada, 5, Rol.TECNICO, EstadoTransicion.AMBITO_SOLICITUD, null))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
                    assertThat(be.getCodigo()).isEqualTo("TRANSICION_NO_PERMITIDA");
                });
    }

    @Test
    void validarRequiresReasonWhenTransitionRequiresIt() {
        Estado enEvaluacion = estado(2, Estado.EN_EVALUACION);
        Estado rechazada = estado(8, Estado.RECHAZADA);
        EstadoTransicion t = transicion(enEvaluacion, rechazada, true);
        when(estados.findById(8)).thenReturn(Optional.of(rechazada));
        when(transiciones.findByEstadoOrigenIdAndEstadoDestinoIdAndRolNombreAndAmbito(
                2, 8, Rol.SUPERVISOR, EstadoTransicion.AMBITO_SOLICITUD))
                .thenReturn(Optional.of(t));

        assertThatThrownBy(() -> service.validar(
                enEvaluacion, 8, Rol.SUPERVISOR, EstadoTransicion.AMBITO_SOLICITUD, "   "))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
                    assertThat(be.getCodigo()).isEqualTo("MOTIVO_OBLIGATORIO");
                });

        assertThat(service.validar(
                enEvaluacion, 8, Rol.SUPERVISOR, EstadoTransicion.AMBITO_SOLICITUD, "No procede"))
                .isSameAs(t);
    }
}
