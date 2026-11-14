import { useEffect, useState } from 'react';
import { reservasApi, type FiltrosBandejaReservas } from '../../api/reservas';
import type { Page, ReservaResumen } from '../../api/types';
import { EstadoBadge } from '../../components/EstadoBadge';
import { formatearFecha } from '../../utils/formato';
import './Reservas.css';

export function AprobacionReservasPage() {
  const [pagina, setPagina] = useState<Page<ReservaResumen> | null>(null);
  const [paginaNum, setPaginaNum] = useState(0);
  const [cargando, setCargando] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [mensaje, setMensaje] = useState<string | null>(null);

  // Filtros
  const [filtroEstado, setFiltroEstado] = useState<number | ''>('');
  const [filtroFecha, setFiltroFecha] = useState('');

  // Modal de rechazo
  const [reservaARechazar, setReservaARechazar] = useState<ReservaResumen | null>(null);
  const [motivoRechazo, setMotivoRechazo] = useState('');
  const [errorRechazo, setErrorRechazo] = useState<string | null>(null);
  const [procesando, setProcesando] = useState(false);

  const cargarBandeja = (p = 0) => {
    setCargando(true);
    setError(null);
    const filtros: FiltrosBandejaReservas = {
      page: p,
      ...(filtroEstado ? { estadoId: Number(filtroEstado) } : {}),
      ...(filtroFecha ? { fecha: filtroFecha } : {}),
    };

    reservasApi
      .bandeja(filtros)
      .then((data) => {
        setPagina(data);
        setPaginaNum(p);
      })
      .catch((err: Error) => setError(err.message))
      .finally(() => setCargando(false));
  };

  useEffect(() => {
    cargarBandeja(paginaNum);
  }, [paginaNum, filtroEstado, filtroFecha]);

  const handleAprobar = async (r: ReservaResumen) => {
    if (!window.confirm(`¿Aprobar la reserva ${r.codigo} para el ambiente ${r.ambienteNombre}?`)) {
      return;
    }
    setError(null);
    setMensaje(null);
    setProcesando(true);
    try {
      await reservasApi.aprobar(r.id);
      setMensaje(`La reserva ${r.codigo} fue aprobada con éxito.`);
      cargarBandeja(paginaNum);
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : 'Error al aprobar la reserva.');
    } finally {
      setProcesando(false);
    }
  };

  const handleConfirmarRechazo = async () => {
    if (!reservaARechazar) return;
    if (!motivoRechazo.trim()) {
      setErrorRechazo('El motivo de rechazo es obligatorio (RN-13).');
      return;
    }
    setError(null);
    setErrorRechazo(null);
    setProcesando(true);
    try {
      await reservasApi.rechazar(reservaARechazar.id, motivoRechazo.trim());
      setMensaje(`La reserva ${reservaARechazar.codigo} fue rechazada.`);
      setReservaARechazar(null);
      setMotivoRechazo('');
      cargarBandeja(paginaNum);
    } catch (err: unknown) {
      setErrorRechazo(err instanceof Error ? err.message : 'Error al rechazar la reserva.');
    } finally {
      setProcesando(false);
    }
  };

  return (
    <div className="reservas-seccion">
      <div className="encabezado-pagina">
        <h1>Bandeja de Aprobación de Reservas (HU-14)</h1>
      </div>

      <p style={{ color: 'var(--color-texto-suave)' }}>
        Revisa, aprueba o rechaza solicitudes de reserva de ambientes con control pesimista de cruces.
      </p>

      {mensaje && <div className="alerta alerta-exito">{mensaje}</div>}
      {error && <div className="alerta alerta-error">{error}</div>}

      <div className="filtros tarjeta" style={{ display: 'flex', gap: '1rem', flexWrap: 'wrap' }}>
        <div className="campo">
          <label htmlFor="filtro-estado-res">Filtrar por Estado</label>
          <select
            id="filtro-estado-res"
            value={filtroEstado}
            onChange={(e) => setFiltroEstado(e.target.value ? Number(e.target.value) : '')}
          >
            <option value="">Todos los estados</option>
            <option value="1">Registrada</option>
            <option value="6">Aprobada</option>
            <option value="7">Rechazada</option>
          </select>
        </div>

        <div className="campo">
          <label htmlFor="filtro-fecha-res">Filtrar por Fecha</label>
          <input
            type="date"
            id="filtro-fecha-res"
            value={filtroFecha}
            onChange={(e) => setFiltroFecha(e.target.value)}
          />
        </div>

        {(filtroEstado !== '' || filtroFecha) && (
          <div style={{ alignSelf: 'flex-end', marginBottom: '0.25rem' }}>
            <button
              type="button"
              className="btn btn-secundario btn-pequeno"
              onClick={() => {
                setFiltroEstado('');
                setFiltroFecha('');
              }}
            >
              Limpiar filtros
            </button>
          </div>
        )}
      </div>

      {cargando ? (
        <p>Cargando bandeja de reservas...</p>
      ) : !pagina || pagina.content.length === 0 ? (
        <div className="tarjeta">
          <p>No se encontraron reservas con los filtros aplicados.</p>
        </div>
      ) : (
        <div className="tarjeta">
          <table className="tabla">
            <thead>
              <tr>
                <th>Código</th>
                <th>Solicitante</th>
                <th>Ambiente</th>
                <th>Fecha</th>
                <th>Horario</th>
                <th>Motivo</th>
                <th>Estado</th>
                <th style={{ textAlign: 'center' }}>Acciones</th>
              </tr>
            </thead>
            <tbody>
              {pagina.content.map((res) => (
                <tr key={res.id}>
                  <td style={{ fontWeight: 600 }}>{res.codigo}</td>
                  <td>{res.usuarioNombre}</td>
                  <td>{res.ambienteNombre}</td>
                  <td>{formatearFecha(res.fecha)}</td>
                  <td>
                    {res.horaInicio.slice(0, 5)} - {res.horaFin.slice(0, 5)}
                  </td>
                  <td>
                    {res.motivo}
                    {res.motivoRechazo && (
                      <div style={{ color: 'var(--color-error)', fontSize: '0.85rem', marginTop: '0.2rem' }}>
                        Motivo: {res.motivoRechazo}
                      </div>
                    )}
                  </td>
                  <td>
                    <EstadoBadge estado={res.estadoNombre} />
                  </td>
                  <td style={{ textAlign: 'center', whiteSpace: 'nowrap' }}>
                    {res.estadoNombre === 'Registrada' ? (
                      <div style={{ display: 'inline-flex', gap: '0.5rem' }}>
                        <button
                          type="button"
                          className="btn btn-primario btn-pequeno"
                          disabled={procesando}
                          onClick={() => handleAprobar(res)}
                        >
                          Aprobar
                        </button>
                        <button
                          type="button"
                          className="btn btn-secundario btn-pequeno"
                          style={{ borderColor: 'var(--color-error)', color: 'var(--color-error)' }}
                          disabled={procesando}
                          onClick={() => {
                            setReservaARechazar(res);
                            setMotivoRechazo('');
                            setErrorRechazo(null);
                          }}
                        >
                          Rechazar
                        </button>
                      </div>
                    ) : (
                      <span style={{ fontSize: '0.85rem', color: 'var(--color-texto-suave)' }}>Atendida</span>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>

          {pagina.page.totalPages > 1 && (
            <div className="paginacion" style={{ display: 'flex', gap: '0.5rem', marginTop: '1rem' }}>
              <button
                type="button"
                className="btn btn-secundario btn-pequeno"
                disabled={paginaNum === 0}
                onClick={() => setPaginaNum((p) => Math.max(0, p - 1))}
              >
                Anterior
              </button>
              <span style={{ alignSelf: 'center', fontSize: '0.9rem' }}>
                Página {paginaNum + 1} de {pagina.page.totalPages}
              </span>
              <button
                type="button"
                className="btn btn-secundario btn-pequeno"
                disabled={paginaNum + 1 >= pagina.page.totalPages}
                onClick={() => setPaginaNum((p) => p + 1)}
              >
                Siguiente
              </button>
            </div>
          )}
        </div>
      )}

      {/* Modal de Rechazo Obligatorio (RN-13) */}
      {reservaARechazar && (
        <div className="modal-backdrop" role="dialog" aria-modal="true" aria-labelledby="modal-titulo">
          <div className="modal-content">
            <h3 id="modal-titulo">Rechazar Reserva {reservaARechazar.codigo}</h3>
            <p style={{ fontSize: '0.9rem', color: 'var(--color-texto-suave)' }}>
              Indica el motivo obligatorio de rechazo para el solicitante ({reservaARechazar.usuarioNombre}).
            </p>

            {errorRechazo && <div className="alerta alerta-error">{errorRechazo}</div>}

            <div className="campo">
              <label htmlFor="motivo-rechazo-input">Motivo de rechazo *</label>
              <textarea
                id="motivo-rechazo-input"
                rows={3}
                maxLength={255}
                value={motivoRechazo}
                onChange={(e) => setMotivoRechazo(e.target.value)}
                placeholder="Ejemplo: Conflicto de uso institucional preferente..."
                autoFocus
                required
              />
            </div>

            <div className="acciones" style={{ marginTop: '1.25rem' }}>
              <button
                type="button"
                className="btn btn-secundario"
                disabled={procesando}
                onClick={() => setReservaARechazar(null)}
              >
                Cancelar
              </button>
              <button
                type="button"
                className="btn btn-primario"
                style={{ background: 'var(--color-error)', borderColor: 'var(--color-error)' }}
                disabled={procesando}
                onClick={handleConfirmarRechazo}
              >
                {procesando ? 'Rechazando...' : 'Confirmar Rechazo'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
