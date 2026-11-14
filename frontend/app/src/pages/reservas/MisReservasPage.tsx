import { useEffect, useState, type FormEvent } from 'react';
import { reservasApi, type FiltrosAmbientes } from '../../api/reservas';
import type {
  Ambiente,
  DisponibilidadResponse,
  Page,
  ReservaResumen,
} from '../../api/types';
import { EstadoBadge } from '../../components/EstadoBadge';
import { formatearFecha } from '../../utils/formato';
import './Reservas.css';

export function MisReservasPage() {
  const [tab, setTab] = useState<'lista' | 'nueva'>('lista');

  // Estado de lista
  const [pagina, setPagina] = useState<Page<ReservaResumen> | null>(null);
  const [paginaNum, setPaginaNum] = useState(0);
  const [cargandoLista, setCargandoLista] = useState(false);
  const [errorLista, setErrorLista] = useState<string | null>(null);

  // Estado de nueva reserva
  const [ambientes, setAmbientes] = useState<Ambiente[]>([]);
  const [filtrosAmb, setFiltrosAmb] = useState<FiltrosAmbientes>({});
  const [ambienteId, setAmbienteId] = useState<number | ''>('');
  const [fecha, setFecha] = useState(() => new Date().toISOString().slice(0, 10));
  const [horaInicio, setHoraInicio] = useState('08:00');
  const [horaFin, setHoraFin] = useState('10:00');
  const [motivo, setMotivo] = useState('');
  const [disponibilidad, setDisponibilidad] = useState<DisponibilidadResponse | null>(null);
  const [cargandoDisp, setCargandoDisp] = useState(false);
  const [guardando, setGuardando] = useState(false);
  const [errorForm, setErrorForm] = useState<string | null>(null);
  const [mensajeExito, setMensajeExito] = useState<string | null>(null);

  // Cargar mis reservas
  const cargarMisReservas = (p = 0) => {
    setCargandoLista(true);
    setErrorLista(null);
    reservasApi
      .mias(p)
      .then((data) => {
        setPagina(data);
        setPaginaNum(p);
      })
      .catch((err: Error) => setErrorLista(err.message))
      .finally(() => setCargandoLista(false));
  };

  useEffect(() => {
    if (tab === 'lista') {
      cargarMisReservas(paginaNum);
    }
  }, [tab, paginaNum]);

  // Cargar catálogo de ambientes
  useEffect(() => {
    if (tab === 'nueva') {
      reservasApi
        .listarAmbientes(filtrosAmb)
        .then((data) => {
          setAmbientes(data);
          if (data.length > 0 && !ambienteId) {
            setAmbienteId(data[0].id);
          }
        })
        .catch((err: Error) => setErrorForm(err.message));
    }
  }, [tab, filtrosAmb]);

  // Consultar disponibilidad al cambiar ambienteId o fecha
  useEffect(() => {
    if (tab === 'nueva' && ambienteId && fecha) {
      setCargandoDisp(true);
      reservasApi
        .consultarDisponibilidad(Number(ambienteId), fecha)
        .then((data) => setDisponibilidad(data))
        .catch(() => setDisponibilidad(null))
        .finally(() => setCargandoDisp(false));
    } else {
      setDisponibilidad(null);
    }
  }, [tab, ambienteId, fecha]);

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setErrorForm(null);
    setMensajeExito(null);

    if (!ambienteId) {
      setErrorForm('Debe seleccionar un ambiente.');
      return;
    }
    if (!fecha) {
      setErrorForm('Debe ingresar una fecha.');
      return;
    }
    if (!horaInicio || !horaFin) {
      setErrorForm('Debe ingresar el horario completo.');
      return;
    }
    if (horaInicio >= horaFin) {
      setErrorForm('La hora de fin debe ser posterior a la hora de inicio.');
      return;
    }
    if (!motivo.trim()) {
      setErrorForm('El motivo es obligatorio.');
      return;
    }

    setGuardando(true);
    try {
      const resp = await reservasApi.crear({
        ambienteId: Number(ambienteId),
        fecha,
        horaInicio: horaInicio.length === 5 ? `${horaInicio}:00` : horaInicio,
        horaFin: horaFin.length === 5 ? `${horaFin}:00` : horaFin,
        motivo: motivo.trim(),
      });
      setMensajeExito(`¡Reserva ${resp.codigo} registrada con éxito! Pasa a evaluación por un supervisor.`);
      setMotivo('');
      setTab('lista');
      cargarMisReservas(0);
    } catch (err: unknown) {
      setErrorForm(err instanceof Error ? err.message : 'Error al registrar la reserva.');
    } finally {
      setGuardando(false);
    }
  };

  return (
    <div className="reservas-seccion">
      <div className="encabezado-pagina">
        <h1>Módulo de Reservas de Ambientes</h1>
      </div>

      {mensajeExito && <div className="alerta alerta-exito">{mensajeExito}</div>}

      <div className="reservas-tabs" role="tablist">
        <button
          type="button"
          role="tab"
          aria-selected={tab === 'lista'}
          className={`reservas-tab-btn ${tab === 'lista' ? 'activo' : ''}`}
          onClick={() => {
            setTab('lista');
            setMensajeExito(null);
          }}
        >
          Mis Reservas
        </button>
        <button
          type="button"
          role="tab"
          aria-selected={tab === 'nueva'}
          className={`reservas-tab-btn ${tab === 'nueva' ? 'activo' : ''}`}
          onClick={() => {
            setTab('nueva');
            setMensajeExito(null);
          }}
        >
          Nueva Reserva
        </button>
      </div>

      {tab === 'lista' && (
        <section aria-label="Lista de mis reservas">
          {errorLista && <div className="alerta alerta-error">{errorLista}</div>}
          {cargandoLista ? (
            <p>Cargando reservas...</p>
          ) : !pagina || pagina.content.length === 0 ? (
            <div className="tarjeta">
              <p>No tienes reservas registradas.</p>
              <button type="button" className="btn btn-primario" onClick={() => setTab('nueva')}>
                Solicitar una reserva
              </button>
            </div>
          ) : (
            <div className="tarjeta">
              <table className="tabla">
                <thead>
                  <tr>
                    <th>Código</th>
                    <th>Ambiente</th>
                    <th>Fecha</th>
                    <th>Horario</th>
                    <th>Motivo</th>
                    <th>Estado</th>
                  </tr>
                </thead>
                <tbody>
                  {pagina.content.map((res) => (
                    <tr key={res.id}>
                      <td style={{ fontWeight: 600 }}>{res.codigo}</td>
                      <td>{res.ambienteNombre}</td>
                      <td>{formatearFecha(res.fecha)}</td>
                      <td>
                        {res.horaInicio.slice(0, 5)} - {res.horaFin.slice(0, 5)}
                      </td>
                      <td>
                        {res.motivo}
                        {res.motivoRechazo && (
                          <div style={{ color: 'var(--color-error)', fontSize: '0.85rem', marginTop: '0.2rem' }}>
                            Motivo rechazo: {res.motivoRechazo}
                          </div>
                        )}
                      </td>
                      <td>
                        <EstadoBadge estado={res.estadoNombre} />
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
        </section>
      )}

      {tab === 'nueva' && (
        <section aria-label="Formulario de reserva" className="tarjeta">
          <h2>Solicitar Reserva de Ambiente (HU-13)</h2>
          <p style={{ color: 'var(--color-texto-suave)', marginBottom: '1.25rem' }}>
            Selecciona el ambiente y fecha deseada. El sistema valida cruces de horario en tiempo real con bloqueo concurrente.
          </p>

          {errorForm && <div className="alerta alerta-error">{errorForm}</div>}

          {/* Filtros de ambiente */}
          <div className="filtros" style={{ marginBottom: '1rem' }}>
            <div className="campo">
              <label htmlFor="filtro-tipo">Tipo de Ambiente</label>
              <select
                id="filtro-tipo"
                value={filtrosAmb.tipo || ''}
                onChange={(e) => setFiltrosAmb((f) => ({ ...f, tipo: e.target.value || undefined }))}
              >
                <option value="">Todos los tipos</option>
                <option value="AUDITORIO">Auditorio</option>
                <option value="LABORATORIO">Laboratorio</option>
                <option value="SALON">Salón</option>
              </select>
            </div>

            <div className="campo">
              <label htmlFor="filtro-pabellon">Pabellón</label>
              <select
                id="filtro-pabellon"
                value={filtrosAmb.pabellon || ''}
                onChange={(e) => setFiltrosAmb((f) => ({ ...f, pabellon: e.target.value || undefined }))}
              >
                <option value="">Todos los pabellones</option>
                <option value="ANTIGUO">Antiguo</option>
                <option value="NUEVO">Nuevo</option>
                <option value="CENTRAL">Central</option>
              </select>
            </div>

            <div className="campo">
              <label htmlFor="filtro-piso">Piso</label>
              <select
                id="filtro-piso"
                value={filtrosAmb.piso !== undefined ? filtrosAmb.piso : ''}
                onChange={(e) =>
                  setFiltrosAmb((f) => ({
                    ...f,
                    piso: e.target.value !== '' ? Number(e.target.value) : undefined,
                  }))
                }
              >
                <option value="">Todos los pisos</option>
                <option value="1">Piso 1</option>
                <option value="2">Piso 2</option>
                <option value="3">Piso 3</option>
                <option value="4">Piso 4</option>
              </select>
            </div>
          </div>

          <form onSubmit={handleSubmit} className="formulario-solicitud">
            <div className="campo">
              <label htmlFor="ambiente-select">Ambiente a reservar *</label>
              <select
                id="ambiente-select"
                value={ambienteId}
                onChange={(e) => setAmbienteId(e.target.value ? Number(e.target.value) : '')}
                required
              >
                {ambientes.map((a) => (
                  <option key={a.id} value={a.id}>
                    {a.nombre} — {a.tipo} ({a.pabellon} {a.piso ? `Piso ${a.piso}` : ''}, Cap: {a.capacidad})
                  </option>
                ))}
              </select>
            </div>

            <div className="fila-dos">
              <div className="campo">
                <label htmlFor="fecha-reserva">Fecha *</label>
                <input
                  type="date"
                  id="fecha-reserva"
                  value={fecha}
                  min={new Date().toISOString().slice(0, 10)}
                  onChange={(e) => setFecha(e.target.value)}
                  required
                />
              </div>

              <div className="campo">
                <label>Horarios ocupados en esta fecha</label>
                <div className="disponibilidad-box">
                  {cargandoDisp ? (
                    <span style={{ fontSize: '0.85rem' }}>Consultando disponibilidad...</span>
                  ) : disponibilidad && disponibilidad.reservasAprobadas.length > 0 ? (
                    <div className="intervalos-ocupados">
                      {disponibilidad.reservasAprobadas.map((int, i) => (
                        <span key={i} className="badge-intervalo">
                          Ocupado: {int.horaInicio.slice(0, 5)} - {int.horaFin.slice(0, 5)} ({int.codigo})
                        </span>
                      ))}
                    </div>
                  ) : (
                    <span className="badge-disponible">Completamente disponible todo el día</span>
                  )}
                </div>
              </div>
            </div>

            <div className="fila-dos">
              <div className="campo">
                <label htmlFor="hora-inicio">Hora de inicio *</label>
                <input
                  type="time"
                  id="hora-inicio"
                  value={horaInicio}
                  onChange={(e) => setHoraInicio(e.target.value)}
                  required
                />
              </div>

              <div className="campo">
                <label htmlFor="hora-fin">Hora de fin *</label>
                <input
                  type="time"
                  id="hora-fin"
                  value={horaFin}
                  onChange={(e) => setHoraFin(e.target.value)}
                  required
                />
              </div>
            </div>

            <div className="campo">
              <label htmlFor="motivo-reserva">Motivo de la actividad académica *</label>
              <textarea
                id="motivo-reserva"
                rows={3}
                maxLength={255}
                value={motivo}
                onChange={(e) => setMotivo(e.target.value)}
                placeholder="Indica la actividad académica, curso, sustentación o evento a realizarse..."
                required
              />
              <span style={{ fontSize: '0.8rem', color: 'var(--color-texto-suave)' }}>
                {motivo.length}/255 caracteres
              </span>
            </div>

            <div className="acciones">
              <button
                type="button"
                className="btn btn-secundario"
                onClick={() => setTab('lista')}
                disabled={guardando}
              >
                Cancelar
              </button>
              <button type="submit" className="btn btn-primario" disabled={guardando}>
                {guardando ? 'Registrando...' : 'Confirmar Reserva'}
              </button>
            </div>
          </form>
        </section>
      )}
    </div>
  );
}
