import { useEffect, useState, type FormEvent } from 'react';
import { dashboardApi } from '../../api/dashboard';
import type { DashboardResponse } from '../../api/types';
import './Dashboard.css';

export function DashboardPage() {
  const [desde, setDesde] = useState('');
  const [hasta, setHasta] = useState('');
  const [datos, setDatos] = useState<DashboardResponse | null>(null);
  const [cargando, setCargando] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const cargarDashboard = (fDesde?: string, fHasta?: string) => {
    setCargando(true);
    setError(null);
    dashboardApi
      .obtener(fDesde || undefined, fHasta || undefined)
      .then((res) => setDatos(res))
      .catch((err: Error) => setError(err.message))
      .finally(() => setCargando(false));
  };

  useEffect(() => {
    cargarDashboard();
  }, []);

  const handleFiltrar = (e: FormEvent) => {
    e.preventDefault();
    if (desde && hasta && desde > hasta) {
      setError("La fecha 'Desde' no puede ser posterior a 'Hasta'.");
      return;
    }
    cargarDashboard(desde, hasta);
  };

  const handleLimpiar = () => {
    setDesde('');
    setHasta('');
    cargarDashboard();
  };

  return (
    <div className="dashboard-seccion">
      <div className="encabezado-pagina">
        <h1>Dashboard</h1>
      </div>

      <p style={{ color: 'var(--color-texto-suave)' }}>
        Métricas clave de atención y calidad de servicio para toma de decisiones gerenciales.
      </p>

      {/* Filtro por rango de fechas */}
      <form onSubmit={handleFiltrar} className="tarjeta filtros" style={{ display: 'flex', gap: '1rem', flexWrap: 'wrap', alignItems: 'flex-end' }}>
        <div className="campo">
          <label htmlFor="fecha-desde">Desde</label>
          <input
            type="date"
            id="fecha-desde"
            value={desde}
            onChange={(e) => setDesde(e.target.value)}
          />
        </div>

        <div className="campo">
          <label htmlFor="fecha-hasta">Hasta</label>
          <input
            type="date"
            id="fecha-hasta"
            value={hasta}
            onChange={(e) => setHasta(e.target.value)}
          />
        </div>

        <button type="submit" className="btn btn-primario" disabled={cargando}>
          {cargando ? 'Consultando...' : 'Filtrar período'}
        </button>

        {(desde || hasta) && (
          <button type="button" className="btn btn-secundario" onClick={handleLimpiar} disabled={cargando}>
            Ver histórico completo
          </button>
        )}
      </form>

      {error && <div className="alerta alerta-error">{error}</div>}

      {cargando && !datos ? (
        <p>Calculando indicadores...</p>
      ) : !datos ? (
        <p>No hay información disponible.</p>
      ) : (
        <>
          {/* Tarjetas KPI principales */}
          <div className="kpi-grid">
            <div className="kpi-card" data-testid="kpi-registradas">
              <span className="kpi-titulo">Total Registradas</span>
              <span className="kpi-valor">{datos.totalRegistradas}</span>
            </div>

            <div className="kpi-card pendientes" data-testid="kpi-pendientes">
              <span className="kpi-titulo">Pendientes</span>
              <span className="kpi-valor">{datos.totalPendientes}</span>
            </div>

            <div className="kpi-card atendidas" data-testid="kpi-atendidas">
              <span className="kpi-titulo">Atendidas</span>
              <span className="kpi-valor">{datos.totalAtendidas}</span>
            </div>

            <div className={`kpi-card ${datos.totalVencidas > 0 ? 'vencidas' : ''}`} data-testid="kpi-vencidas">
              <span className="kpi-titulo">Vencidas (RN-03)</span>
              <span className="kpi-valor">{datos.totalVencidas}</span>
            </div>

            <div className="kpi-card" data-testid="kpi-porcentaje">
              <span className="kpi-titulo">% Resueltas</span>
              <span className="kpi-valor">{datos.porcentajeResueltas}%</span>
            </div>

            <div className="kpi-card" data-testid="kpi-tiempo-promedio">
              <span className="kpi-titulo">Tiempo Promedio</span>
              <span className="kpi-valor">{datos.tiempoPromedioAtencionHoras} h</span>
            </div>
          </div>

          {/* Gráficos y desglose */}
          <div className="desglose-grid">
            {/* Por Categoría */}
            <div className="desglose-card">
              <h3>Solicitudes por Categoría</h3>
              {datos.porCategoria.length === 0 ? (
                <p style={{ color: 'var(--color-texto-suave)', fontSize: '0.9rem' }}>Sin datos en este período.</p>
              ) : (
                datos.porCategoria.map((cat, i) => {
                  const pct = datos.totalRegistradas > 0 ? Math.round((cat.cantidad / datos.totalRegistradas) * 100) : 0;
                  return (
                    <div key={i} className="barra-item">
                      <div className="barra-info">
                        <span>{cat.nombre}</span>
                        <span style={{ fontWeight: 600 }}>{cat.cantidad} ({pct}%)</span>
                      </div>
                      <div className="barra-contenedor">
                        <div className="barra-progreso" style={{ width: `${pct}%` }} />
                      </div>
                    </div>
                  );
                })
              )}
            </div>

            {/* Por Prioridad */}
            <div className="desglose-card">
              <h3>Solicitudes por Prioridad</h3>
              {datos.porPrioridad.length === 0 ? (
                <p style={{ color: 'var(--color-texto-suave)', fontSize: '0.9rem' }}>Sin datos en este período.</p>
              ) : (
                datos.porPrioridad.map((prio, i) => {
                  const pct = datos.totalRegistradas > 0 ? Math.round((prio.cantidad / datos.totalRegistradas) * 100) : 0;
                  return (
                    <div key={i} className="barra-item">
                      <div className="barra-info">
                        <span>{prio.nombre}</span>
                        <span style={{ fontWeight: 600 }}>{prio.cantidad} ({pct}%)</span>
                      </div>
                      <div className="barra-contenedor">
                        <div className="barra-progreso acento" style={{ width: `${pct}%` }} />
                      </div>
                    </div>
                  );
                })
              )}
            </div>

            {/* Por Responsable / Técnico */}
            <div className="desglose-card">
              <h3>Atención por Responsable</h3>
              {datos.porResponsable.length === 0 ? (
                <p style={{ color: 'var(--color-texto-suave)', fontSize: '0.9rem' }}>Sin datos en este período.</p>
              ) : (
                datos.porResponsable.map((resp, i) => {
                  const pct = datos.totalRegistradas > 0 ? Math.round((resp.cantidad / datos.totalRegistradas) * 100) : 0;
                  return (
                    <div key={i} className="barra-item">
                      <div className="barra-info">
                        <span>{resp.nombre}</span>
                        <span style={{ fontWeight: 600 }}>{resp.cantidad} ({pct}%)</span>
                      </div>
                      <div className="barra-contenedor">
                        <div className="barra-progreso exito" style={{ width: `${pct}%` }} />
                      </div>
                    </div>
                  );
                })
              )}
            </div>
          </div>
        </>
      )}
    </div>
  );
}
