import { useEffect, useState } from 'react';
import { Link } from 'react-router';

import { catalogos } from '../../api/catalogos';
import { ApiError } from '../../api/client';
import { solicitudes } from '../../api/solicitudes';
import type { Categoria, Estado, Page, SolicitudResumen } from '../../api/types';
import { EstadoBadge } from '../../components/EstadoBadge';
import { formatearFechaHora } from '../../utils/formato';
import './Solicitudes.css';

/** HU-05. */
export function MisSolicitudesPage() {
  const [estados, setEstados] = useState<Estado[]>([]);
  const [categorias, setCategorias] = useState<Categoria[]>([]);
  const [estadoId, setEstadoId] = useState('');
  const [categoriaId, setCategoriaId] = useState('');
  const [pagina, setPagina] = useState(0);
  const [resultado, setResultado] = useState<Page<SolicitudResumen> | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    Promise.all([catalogos.estadosSolicitud(), catalogos.categorias()])
      .then(([est, cats]) => {
        setEstados(est);
        setCategorias(cats);
      })
      .catch(() => setError('No se pudieron cargar los filtros.'));
  }, []);

  useEffect(() => {
    let vigente = true;
    solicitudes
      .mias({ estadoId: Number(estadoId) || undefined, categoriaId: Number(categoriaId) || undefined, page: pagina })
      .then((r) => {
        if (vigente) {
          setResultado(r);
          setError(null);
        }
      })
      .catch((e) => vigente && setError(e instanceof ApiError ? e.message : 'No se pudieron cargar las solicitudes.'));
    return () => {
      vigente = false;
    };
  }, [estadoId, categoriaId, pagina]);

  function filtrar(setter: (valor: string) => void) {
    return (valor: string) => {
      setter(valor);
      setPagina(0);
    };
  }

  const filas = resultado?.content ?? [];
  const totalPaginas = resultado?.page.totalPages ?? 0;
  const hayFiltros = Boolean(estadoId || categoriaId);

  return (
    <section>
      <div className="encabezado-pagina">
        <h1>Mis solicitudes</h1>
        <Link to="/solicitudes/nueva" className="btn btn-primario">+ Nueva solicitud</Link>
      </div>

      <div className="filtros">
        <div className="campo">
          <label htmlFor="filtro-estado">Estado</label>
          <select id="filtro-estado" value={estadoId} onChange={(e) => filtrar(setEstadoId)(e.target.value)}>
            <option value="">Todos</option>
            {estados.map((e) => <option key={e.id} value={e.id}>{e.nombre}</option>)}
          </select>
        </div>
        <div className="campo">
          <label htmlFor="filtro-categoria">Categoría</label>
          <select id="filtro-categoria" value={categoriaId} onChange={(e) => filtrar(setCategoriaId)(e.target.value)}>
            <option value="">Todas</option>
            {categorias.map((c) => <option key={c.id} value={c.id}>{c.nombre}</option>)}
          </select>
        </div>
      </div>

      {error && <div className="alerta-error" role="alert">{error}</div>}

      {resultado && filas.length === 0 && (
        <p className="vacio">{hayFiltros ? 'No hay solicitudes con esos filtros.' : 'Aún no tienes solicitudes.'}</p>
      )}

      {filas.length > 0 && (
        <div className="tabla-contenedor">
          <table className="tabla">
            <thead>
              <tr>
                <th>Código</th>
                <th>Categoría</th>
                <th>Tipo</th>
                <th>Fecha</th>
                <th>Estado</th>
              </tr>
            </thead>
            <tbody>
              {filas.map((s) => (
                <tr key={s.id}>
                  <td><Link to={`/solicitudes/${s.id}`}>{s.codigo}</Link></td>
                  <td>{s.categoria}</td>
                  <td>{s.tipo}</td>
                  <td>{formatearFechaHora(s.fechaRegistro)}</td>
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
