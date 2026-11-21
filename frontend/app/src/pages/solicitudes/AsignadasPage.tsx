import { useEffect, useState } from 'react';
import { Link } from 'react-router';

import { catalogos } from '../../api/catalogos';
import { ApiError } from '../../api/client';
import { solicitudes } from '../../api/solicitudes';
import type { Estado, Page, SolicitudResumen } from '../../api/types';
import { EstadoBadge } from '../../components/EstadoBadge';
import { formatearFechaHora } from '../../utils/formato';
import './Solicitudes.css';

/** HU-09: requests assigned to the authenticated technician. */
export function AsignadasPage() {
  const [estados, setEstados] = useState<Estado[]>([]);
  const [estadoId, setEstadoId] = useState('');
  const [pagina, setPagina] = useState(0);
  const [resultado, setResultado] = useState<Page<SolicitudResumen> | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    catalogos.estadosSolicitud()
      .then(setEstados)
      .catch(() => setError('No se pudieron cargar los filtros.'));
  }, []);

  useEffect(() => {
    let vigente = true;
    solicitudes
      .asignadas({ estadoId: Number(estadoId) || undefined, page: pagina })
      .then((r) => {
        if (vigente) {
          setResultado(r);
          setError(null);
        }
      })
      .catch((e) => vigente && setError(e instanceof ApiError ? e.message : 'No se pudieron cargar las solicitudes asignadas.'));
    return () => {
      vigente = false;
    };
  }, [estadoId, pagina]);

  const filas = resultado?.content ?? [];
  const totalPaginas = resultado?.page.totalPages ?? 0;

  return (
    <section>
      <div className="encabezado-pagina">
        <h1>Solicitudes asignadas</h1>
      </div>

      <div className="filtros">
        <div className="campo">
          <label htmlFor="filtro-estado">Estado</label>
          <select
            id="filtro-estado"
            value={estadoId}
            onChange={(e) => {
              setEstadoId(e.target.value);
              setPagina(0);
            }}
          >
            <option value="">Todos</option>
            {estados.map((e) => <option key={e.id} value={e.id}>{e.nombre}</option>)}
          </select>
        </div>
      </div>

      {error && <div className="alerta-error" role="alert">{error}</div>}

      {resultado && filas.length === 0 && (
        <p className="vacio">{estadoId ? 'No hay solicitudes asignadas en ese estado.' : 'No tienes solicitudes asignadas.'}</p>
      )}

      {filas.length > 0 && (
        <div className="tabla-contenedor">
          <table className="tabla">
            <thead>
              <tr>
                <th>Código</th>
                <th>Categoría</th>
                <th>Tipo</th>
                <th>Prioridad</th>
                <th>Fecha</th>
                <th>Límite</th>
                <th>Estado</th>
              </tr>
            </thead>
            <tbody>
              {filas.map((s) => (
                <tr key={s.id}>
                  <td><Link to={`/solicitudes/${s.id}`}>{s.codigo}</Link></td>
                  <td>{s.categoria}</td>
                  <td>{s.tipo}</td>
                  <td>{s.prioridad}</td>
                  <td>{formatearFechaHora(s.fechaRegistro)}</td>
                  <td>{formatearFechaHora(s.fechaLimite)}</td>
                  <td>
                    <EstadoBadge estado={s.estado} />
                    {s.vencida && <span className="etiqueta-vencida">Vencida</span>}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {totalPaginas > 1 && (
        <nav className="paginacion" aria-label="Paginación">
          <button type="button" className="btn btn-pequeno" disabled={pagina === 0} onClick={() => setPagina((p) => p - 1)}>
            Anterior
          </button>
          <span>Página {pagina + 1} de {totalPaginas}</span>
          <button type="button" className="btn btn-pequeno" disabled={pagina + 1 >= totalPaginas} onClick={() => setPagina((p) => p + 1)}>
            Siguiente
          </button>
        </nav>
      )}
    </section>
  );
}
