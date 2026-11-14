package pe.edu.unmsm.servicios.dashboard.dto;

import java.util.List;

public final class DashboardDtos {

    private DashboardDtos() {
    }

    public record KpiItem(
            Long id,
            String nombre,
            long cantidad
    ) {
    }

    public record DashboardResponse(
            long totalRegistradas,
            long totalPendientes,
            long totalAtendidas,
            long totalVencidas,
            double porcentajeResueltas,
            double tiempoPromedioAtencionHoras,
            List<KpiItem> porCategoria,
            List<KpiItem> porPrioridad,
            List<KpiItem> porResponsable
    ) {
    }
}
