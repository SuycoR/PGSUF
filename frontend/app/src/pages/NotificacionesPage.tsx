import { useCallback, useEffect, useState } from 'react';
import { useNavigate } from 'react-router';

import { ApiError } from '../api/client';
import { notificaciones } from '../api/notificaciones';
import type { Notificacion, Page } from '../api/types';
import { formatearFechaHora } from '../utils/formato';
import './Notificaciones.css';

/** HU-17: user notifications list with read/unread filter, mark one/all, and navigation. */
export function NotificacionesPage() {
  const navigate = useNavigate();
  const [filtroLeida, setFiltroLeida] = useState<string>('');
  const [pagina, setPagina] = useState(0);
  const [resultado, setResultado] = useState<Page<Notificacion> | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [ocupado, setOcupado] = useState(false);

  const leidaParam = filtroLeida === '' ? undefined : filtroLeida === 'true';

  const cargar = useCallback(async () => {
    const res = await notificaciones.listar({ leida: leidaParam, page: pagina });
    setResultado(res);
    setError(null);
  }, [leidaParam, pagina]);

  useEffect(() => {
    let vigente = true;
    notificaciones
      .listar({ leida: leidaParam, page: pagina })
      .then((res) => {
        if (vigente) {
          setResultado(res);
          setError(null);
        }
      })
      .catch((e) => {
        if (vigente) {
          setError(e instanceof ApiError ? e.message : 'No se pudieron cargar las notificaciones.');
        }
      });
    return () => {
      vigente = false;
    };
  }, [leidaParam, pagina]);

  async function manejarMarcarLeida(n: Notificacion) {
    setError(null);
    setOcupado(true);
    try {
      await notificaciones.marcarLeida(n.id);
      await cargar();
    } catch (e) {
      setError(e instanceof ApiError ? e.message : 'No se pudo marcar la notificación como leída.');
    } finally {
      setOcupado(false);
    }
  }

  async function manejarAbrirSolicitud(n: Notificacion) {
    if (!n.solicitudId) return;
    if (!n.leida) {
      try {
        await notificaciones.marcarLeida(n.id);
      } catch {
        // Navigate even if marking read fails transiently.
      }
    }
    navigate(`/solicitudes/${n.solicitudId}`);
  }

  async function manejarMarcarTodas() {
    setError(null);
    setOcupado(true);
    try {
      await notificaciones.marcarTodasLeidas();
      await cargar();
    } catch (e) {
      setError(e instanceof ApiError ? e.message : 'No se pudieron marcar todas como leídas.');
    } finally {
      setOcupado(false);
    }
  }

  const filas = resultado?.content ?? [];
  const totalPaginas = resultado?.page.totalPages ?? 0;
  const hayNoLeidas = filas.some((n) => !n.leida);

  return (
    <section>
      <div className="notificaciones-encabezado">
        <h1>Notificaciones</h1>
        <button
          type="button"
          className="btn"
          disabled={ocupado || !hayNoLeidas}
          onClick={manejarMarcarTodas}
        >
          Marcar todas como leídas
        </button>
      </div>

      <div className="notificaciones-filtros">
        <div className="campo">
          <label htmlFor="filtro-leida">Estado</label>
          <select
            id="filtro-leida"
            value={filtroLeida}
            onChange={(e) => {
              setFiltroLeida(e.target.value);
              setPagina(0);
            }}
          >
            <option value="">Todas</option>
            <option value="false">No leídas</option>
            <option value="true">Leídas</option>
          </select>
        </div>
      </div>

      {error && <div className="alerta-error" role="alert">{error}</div>}

      {!resultado && !error && <p aria-busy="true">Cargando…</p>}

      {resultado && filas.length === 0 && (
        <p className="vacio">
          {filtroLeida ? 'No hay notificaciones con ese filtro.' : 'No tienes notificaciones.'}
        </p>
      )}

      {filas.length > 0 && (
        <ul className="lista-notificaciones">
          {filas.map((n) => (
            <li
              key={n.id}
              className={`tarjeta notificacion-item ${n.leida ? '' : 'notificacion-no-leida'}`}
            >
              <div className="notificacion-cuerpo">
                <div className="notificacion-titulo-fila">
                  <strong>{n.titulo}</strong>
                  {!n.leida && <span className="notificacion-etiqueta-nueva">Nueva</span>}
                </div>
                <p className="notificacion-mensaje">{n.mensaje}</p>
                <span className="notificacion-fecha">{formatearFechaHora(n.fechaCreacion)}</span>
              </div>
              <div className="notificacion-acciones">
                {n.solicitudId !== null && (
                  <button
                    type="button"
                    className="btn btn-primario btn-pequeno"
                    disabled={ocupado}
                    onClick={() => manejarAbrirSolicitud(n)}
                  >
                    Ver solicitud
                  </button>
                )}
                {!n.leida && (
                  <button
                    type="button"
                    className="btn btn-pequeno"
                    disabled={ocupado}
                    onClick={() => manejarMarcarLeida(n)}
                  >
                    Marcar como leída
                  </button>
                )}
              </div>
            </li>
          ))}
        </ul>
      )}

      {totalPaginas > 1 && (
        <nav className="paginacion" aria-label="Paginación">
          <button
            type="button"
            className="btn btn-pequeno"
            disabled={pagina === 0}
            onClick={() => setPagina((p) => p - 1)}
          >
            Anterior
          </button>
          <span>Página {pagina + 1} de {totalPaginas}</span>
          <button
            type="button"
            className="btn btn-pequeno"
            disabled={pagina + 1 >= totalPaginas}
            onClick={() => setPagina((p) => p + 1)}
          >
            Siguiente
          </button>
        </nav>
      )}
    </section>
  );
}
