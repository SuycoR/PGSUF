package pe.edu.unmsm.servicios.dashboard.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.edu.unmsm.servicios.catalogo.entity.Categoria;
import pe.edu.unmsm.servicios.catalogo.entity.Prioridad;
import pe.edu.unmsm.servicios.common.BusinessException;
import pe.edu.unmsm.servicios.dashboard.dto.DashboardDtos.DashboardResponse;
import pe.edu.unmsm.servicios.dashboard.dto.DashboardDtos.KpiItem;
import pe.edu.unmsm.servicios.solicitud.entity.Solicitud;
import pe.edu.unmsm.servicios.solicitud.repository.SolicitudRepository;
import pe.edu.unmsm.servicios.usuario.entity.Usuario;

@Service
public class DashboardService {

    private final SolicitudRepository solicitudes;
    private final Clock clock;

    public DashboardService(SolicitudRepository solicitudes, Clock clock) {
        this.solicitudes = solicitudes;
        this.clock = clock;
    }

    /**
     * HU-11: calculates aggregated indicators and metrics for a given period (RN-03).
     */
    @Transactional(readOnly = true)
    public DashboardResponse obtenerIndicadores(LocalDate desde, LocalDate hasta) {
        if (desde != null && hasta != null && desde.isAfter(hasta)) {
            throw BusinessException.unprocessable("RANGO_INVALIDO", "Rango de fechas inválido",
                    "La fecha 'desde' no puede ser posterior a la fecha 'hasta'.");
        }

        LocalDateTime inicio = desde != null ? desde.atStartOfDay() : null;
        LocalDateTime fin = hasta != null ? hasta.atTime(LocalTime.MAX).truncatedTo(ChronoUnit.SECONDS) : null;
        LocalDateTime ahora = LocalDateTime.now(clock).truncatedTo(ChronoUnit.SECONDS);

        List<Solicitud> lista = solicitudes.buscarParaDashboard(inicio, fin);

        long totalRegistradas = lista.size();
        long totalPendientes = lista.stream()
                .filter(s -> !s.getEstado().isEsFinal())
                .count();
        long totalAtendidas = lista.stream()
                .filter(s -> s.getEstado().isEsFinal())
                .count();

        // RN-03: expired if fechaLimite < ahora and state is not final
        long totalVencidas = lista.stream()
                .filter(s -> !s.getEstado().isEsFinal() && s.getFechaLimite().isBefore(ahora))
                .count();

        double porcentajeResueltas = totalRegistradas > 0
                ? Math.round(((double) totalAtendidas / totalRegistradas * 100.0) * 10.0) / 10.0
                : 0.0;

        double tiempoPromedioAtencionHoras = lista.stream()
                .filter(s -> s.getFechaCierre() != null)
                .mapToDouble(s -> ChronoUnit.MINUTES.between(s.getFechaRegistro(), s.getFechaCierre()) / 60.0)
                .average()
                .stream()
                .map(avg -> Math.round(avg * 10.0) / 10.0)
                .findFirst()
                .orElse(0.0);

        // Group by category
        Map<Categoria, Long> porCat = lista.stream()
                .collect(Collectors.groupingBy(s -> s.getTipoSolicitud().getCategoria(), Collectors.counting()));
        List<KpiItem> kpiPorCategoria = porCat.entrySet().stream()
                .map(e -> new KpiItem((long) e.getKey().getId(), e.getKey().getNombre(), e.getValue()))
                .sorted(Comparator.comparingLong(KpiItem::cantidad).reversed().thenComparing(KpiItem::nombre))
                .toList();

        // Group by priority
        Map<Prioridad, Long> porPrio = lista.stream()
                .collect(Collectors.groupingBy(Solicitud::getPrioridad, Collectors.counting()));
        List<KpiItem> kpiPorPrioridad = porPrio.entrySet().stream()
                .map(e -> new KpiItem((long) e.getKey().getId(), e.getKey().getNombre(), e.getValue()))
                .sorted(Comparator.comparing(KpiItem::id))
                .toList();

        // Group by technician/responsible
        Map<Usuario, Long> porResp = lista.stream()
                .collect(Collectors.groupingBy(
                        s -> s.getResponsable() != null ? s.getResponsable() : UsuarioAnonimo.INSTANCE,
                        Collectors.counting()));
        List<KpiItem> kpiPorResponsable = porResp.entrySet().stream()
                .map(e -> {
                    if (e.getKey() instanceof UsuarioAnonimo) {
                        return new KpiItem(null, "Sin asignar", e.getValue());
                    }
                    return new KpiItem(e.getKey().getId(), e.getKey().getNombre(), e.getValue());
                })
                .sorted(Comparator.comparingLong(KpiItem::cantidad).reversed().thenComparing(KpiItem::nombre))
                .toList();

        return new DashboardResponse(
                totalRegistradas,
                totalPendientes,
                totalAtendidas,
                totalVencidas,
                porcentajeResueltas,
                tiempoPromedioAtencionHoras,
                kpiPorCategoria,
                kpiPorPrioridad,
                kpiPorResponsable
        );
    }

    /**
     * Marker singleton for unassigned requests in grouping calculations.
     */
    private static final class UsuarioAnonimo extends Usuario {
        static final UsuarioAnonimo INSTANCE = new UsuarioAnonimo();

        @Override
        public Long getId() {
            return null;
        }

        @Override
        public String getNombre() {
            return "Sin asignar";
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof UsuarioAnonimo;
        }

        @Override
        public int hashCode() {
            return 42;
        }
    }
}
