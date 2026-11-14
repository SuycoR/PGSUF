import { useCallback, useEffect, useRef, useState } from 'react';
import { Link, useLocation, useParams } from 'react-router';

import { catalogos } from '../../api/catalogos';
import { ApiError } from '../../api/client';
import { solicitudes } from '../../api/solicitudes';
import type {
  Evidencia,
  Historial,
  Prioridad,
  SolicitudDetalle,
  Tecnico,
  TransicionDisponible,
} from '../../api/types';
import { useAuth } from '../../auth/useAuth';
import { EstadoBadge } from '../../components/EstadoBadge';
import { formatearFechaHora, formatearTamano } from '../../utils/formato';
import { validarArchivos } from './validarSolicitud';
import './Solicitudes.css';

interface Datos {
  detalle: SolicitudDetalle;
  historial: Historial[];
  evidencias: Evidencia[];
  transiciones: TransicionDisponible[];
}

/** HU-06, HU-07, HU-08, HU-09, HU-15, HU-16. */
export function SolicitudDetallePage() {
  const { id } = useParams();
  const location = useLocation();
  const { usuario } = useAuth();
  const mensajeInicial = (location.state as { mensaje?: string } | null)?.mensaje;

  const [datos, setDatos] = useState<Datos | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [mensajeAccion, setMensajeAccion] = useState<string | null>(mensajeInicial ?? null);
  const [errorAccion, setErrorAccion] = useState<string | null>(null);
  const [errorDescarga, setErrorDescarga] = useState<string | null>(null);
  const [ocupado, setOcupado] = useState(false);

  const [prioridades, setPrioridades] = useState<Prioridad[]>([]);
  const [prioridadId, setPrioridadId] = useState('');
  const [tecnicos, setTecnicos] = useState<Tecnico[]>([]);
  const [tecnicoId, setTecnicoId] = useState('');
  const [comentario, setComentario] = useState('');
  const [archivosNuevos, setArchivosNuevos] = useState<File[]>([]);
  const inputArchivoRef = useRef<HTMLInputElement | null>(null);

  const esSupervisor = usuario?.rol === 'SUPERVISOR';

  const cargarSolicitud = useCallback(async (solicitudId: number) => {
    const [detalle, historial, evidencias, transiciones] = await Promise.all([
      solicitudes.detalle(solicitudId),
      solicitudes.historial(solicitudId),
      solicitudes.evidencias(solicitudId),
      solicitudes.transiciones(solicitudId).catch(() => [] as TransicionDisponible[]),
    ]);
    return { detalle, historial, evidencias, transiciones };
  }, []);

  useEffect(() => {
    const solicitudId = Number(id);
    let vigente = true;
    cargarSolicitud(solicitudId)
      .then((res) => {
        if (!vigente) return;
        setDatos(res);
        setError(null);
      })
      .catch((e) => {
        if (!vigente) return;
        setError(e instanceof ApiError && e.status === 404 ? 'Solicitud no encontrada.' : 'No se pudo cargar la solicitud.');
      });
    return () => {
      vigente = false;
    };
  }, [id, cargarSolicitud]);

  useEffect(() => {
    if (!esSupervisor || !datos || datos.detalle.estadoFinal) return;
    let vigente = true;
    const puedeAsignar = datos.transiciones.some((t) => t.nombre === 'Asignada');

    Promise.all([
      catalogos.prioridades().catch(() => [] as Prioridad[]),
      puedeAsignar ? solicitudes.tecnicos(datos.detalle.areaId).catch(() => [] as Tecnico[]) : Promise.resolve([] as Tecnico[]),
    ]).then(([prios, tecs]) => {
      if (!vigente) return;
      setPrioridades(prios);
      const actual = datos.detalle.prioridadId
        ? String(datos.detalle.prioridadId)
        : String(prios.find((p) => p.nombre === datos.detalle.prioridad)?.id ?? '');
      setPrioridadId(actual);
      setTecnicos(tecs);
    });

    return () => {
      vigente = false;
    };
  }, [esSupervisor, datos]);

  async function refrescar() {
    if (!datos) return;
    const actualizado = await cargarSolicitud(datos.detalle.id);
    setDatos(actualizado);
  }

  async function descargar(evidencia: Evidencia) {
    setErrorDescarga(null);
    try {
      const blob = await solicitudes.descargarEvidencia(evidencia.id);
      const url = URL.createObjectURL(blob);
      const enlace = document.createElement('a');
      enlace.href = url;
      enlace.download = evidencia.nombreArchivo;
      enlace.click();
      URL.revokeObjectURL(url);
    } catch {
      setErrorDescarga(`No se pudo descargar "${evidencia.nombreArchivo}".`);
    }
  }

  async function ejecutarAccion(accion: () => Promise<string>) {
    setErrorAccion(null);
    setMensajeAccion(null);
    setOcupado(true);
    try {
      const exito = await accion();
      await refrescar();
      setMensajeAccion(exito);
    } catch (e) {
      setErrorAccion(e instanceof ApiError ? e.message : 'No se pudo completar la acción.');
    } finally {
      setOcupado(false);
    }
  }

  function manejarAsignar(e: React.FormEvent) {
    e.preventDefault();
    if (!datos || !tecnicoId) {
      setErrorAccion('Seleccione un técnico.');
      return;
    }
    void ejecutarAccion(async () => {
      const res = await solicitudes.asignar(datos.detalle.id, Number(tecnicoId));
      setTecnicoId('');
      return `Solicitud asignada a ${res.responsable?.nombre ?? 'técnico'}.`;
    });
  }

  function manejarPrioridad(e: React.FormEvent) {
    e.preventDefault();
    if (!datos || !prioridadId) return;
    void ejecutarAccion(async () => {
      const res = await solicitudes.cambiarPrioridad(datos.detalle.id, Number(prioridadId));
      return `Prioridad actualizada a ${res.prioridad}.`;
    });
  }

  function manejarTransicion(transicion: TransicionDisponible) {
    if (!datos) return;
    if (transicion.requiereMotivo && !comentario.trim()) {
      setErrorAccion(`Debe indicar el motivo para pasar a ${transicion.nombre}.`);
      return;
    }
    void ejecutarAccion(async () => {
      const res = await solicitudes.cambiarEstado(datos.detalle.id, transicion.estadoDestinoId, comentario);
      setComentario('');
      return `Estado actualizado a ${res.estado}.`;
    });
  }

  function manejarSubidaEvidencias(e: React.FormEvent) {
    e.preventDefault();
    if (!datos) return;
    if (archivosNuevos.length === 0) {
      setErrorAccion('Seleccione al menos un archivo para subir.');
      return;
    }
    const err = validarArchivos(archivosNuevos, false);
    if (err) {
      setErrorAccion(err);
      return;
    }
    void ejecutarAccion(async () => {
      await solicitudes.agregarEvidencias(datos.detalle.id, archivosNuevos);
      setArchivosNuevos([]);
      if (inputArchivoRef.current) {
        inputArchivoRef.current.value = '';
      }
      return 'Evidencias adjuntadas correctamente.';
    });
  }

  if (error) {
    return (
      <section className="tarjeta">
        <h1>{error}</h1>
        <Link to="/solicitudes">Volver a mis solicitudes</Link>
      </section>
    );
  }
  if (!datos) {
    return <p aria-busy="true">Cargando…</p>;
  }

  const { detalle, historial, evidencias, transiciones } = datos;
  const mostrarAsignar = esSupervisor && transiciones.some((t) => t.nombre === 'Asignada');
  const accionesEstado = esSupervisor
    ? transiciones.filter((t) => t.nombre !== 'Asignada')
    : transiciones;
  const mostrarPrioridad = esSupervisor && !detalle.estadoFinal && prioridades.length > 0;

  const rutaVolver =
    usuario?.rol === 'TECNICO'
      ? '/asignadas'
      : usuario?.rol === 'SUPERVISOR' || usuario?.rol === 'PERSONAL_AREA'
        ? '/bandeja'
        : '/solicitudes';

  return (
    <section className="detalle">
      {mensajeAccion && <div className="alerta-exito" role="status">{mensajeAccion}</div>}
      {errorAccion && <div className="alerta-error" role="alert">{errorAccion}</div>}

      <div className="encabezado-pagina">
        <h1>{detalle.codigo}</h1>
        <div>
          <EstadoBadge estado={detalle.estado} />
          {detalle.vencida && <span className="etiqueta-vencida">Vencida</span>}
        </div>
      </div>

      <div className="tarjeta">
        <dl className="datos">
          <Dato termino="Categoría" valor={detalle.categoria} />
          <Dato termino="Tipo" valor={detalle.tipo} />
          <Dato termino="Área responsable" valor={detalle.area} />
          <Dato termino="Prioridad" valor={detalle.prioridad} />
          <Dato termino="Ubicación" valor={detalle.ubicacion} />
          <Dato termino="Solicitante" valor={detalle.solicitante.nombre} />
          <Dato termino="Responsable" valor={detalle.responsable?.nombre ?? 'Sin asignar'} />
          <Dato termino="Registrada" valor={formatearFechaHora(detalle.fechaRegistro)} />
          <Dato termino="Fecha límite" valor={formatearFechaHora(detalle.fechaLimite)} />
          {detalle.fechaCierre && <Dato termino="Cerrada" valor={formatearFechaHora(detalle.fechaCierre)} />}
        </dl>
        <h2>Descripción</h2>
        <p className="texto-largo">{detalle.descripcion}</p>
        {detalle.respuesta && (
          <>
            <h2>Respuesta</h2>
            <p className="texto-largo">{detalle.respuesta}</p>
          </>
        )}
      </div>

      {(mostrarPrioridad || mostrarAsignar || accionesEstado.length > 0) && (
        <div className="tarjeta">
          <h2>Gestión de la solicitud</h2>
          <div className="gestion-grid">
            {mostrarPrioridad && (
              <form onSubmit={manejarPrioridad} className="fila-inline">
                <div className="campo">
                  <label htmlFor="select-prioridad">Prioridad</label>
                  <select
                    id="select-prioridad"
                    value={prioridadId}
                    onChange={(e) => setPrioridadId(e.target.value)}
                    disabled={ocupado}
                  >
                    {prioridades.map((p) => (
                      <option key={p.id} value={p.id}>{p.nombre}</option>
                    ))}
                  </select>
                </div>
                <button type="submit" className="btn" disabled={ocupado || !prioridadId}>
                  Actualizar prioridad
                </button>
              </form>
            )}

            {mostrarAsignar && (
              <form onSubmit={manejarAsignar} className="fila-inline">
                <div className="campo">
                  <label htmlFor="select-tecnico">Técnico responsable</label>
                  <select
                    id="select-tecnico"
                    value={tecnicoId}
                    onChange={(e) => setTecnicoId(e.target.value)}
                    disabled={ocupado}
                  >
                    <option value="">Seleccione un técnico</option>
                    {tecnicos.map((t) => (
                      <option key={t.id} value={t.id}>{t.nombre}</option>
                    ))}
                  </select>
                </div>
                <button type="submit" className="btn btn-primario" disabled={ocupado}>
                  Asignar
                </button>
              </form>
            )}
          </div>

          {accionesEstado.length > 0 && (
            <div style={{ marginTop: mostrarPrioridad || mostrarAsignar ? '1.25rem' : 0 }}>
              <div className="campo">
                <label htmlFor="comentario-estado">Comentario u observación</label>
                <textarea
                  id="comentario-estado"
                  rows={2}
                  value={comentario}
                  onChange={(e) => setComentario(e.target.value)}
                  placeholder="Opcional, o motivo obligatorio si rechaza la solicitud"
                  disabled={ocupado}
                />
              </div>
              <div className="acciones-estado">
                {accionesEstado.map((t) => (
                  <button
                    key={t.estadoDestinoId}
                    type="button"
                    className={`btn ${t.nombre === 'Rechazada' ? 'btn-peligro' : 'btn-primario'}`}
                    disabled={ocupado}
                    onClick={() => manejarTransicion(t)}
                  >
                    {t.nombre}
                  </button>
                ))}
              </div>
            </div>
          )}
        </div>
      )}

      <div className="tarjeta">
        <h2>Evidencias</h2>
        {errorDescarga && <div className="alerta-error" role="alert">{errorDescarga}</div>}
        {evidencias.length === 0 ? (
          <p className="vacio">Sin archivos adjuntos.</p>
        ) : (
          <ul className="lista-archivos">
            {evidencias.map((e) => (
              <li key={e.id}>
                <span>{e.nombreArchivo} ({formatearTamano(e.tamanoBytes)})</span>
                <button type="button" className="btn btn-pequeno" onClick={() => descargar(e)}>Descargar</button>
              </li>
            ))}
          </ul>
        )}

        {!detalle.estadoFinal && (
          <form onSubmit={manejarSubidaEvidencias} className="subida-evidencias">
            <div className="fila-inline">
              <div className="campo">
                <label htmlFor="nuevas-evidencias">Agregar evidencias (PDF, JPG o PNG, máx. 5 MB)</label>
                <input
                  ref={inputArchivoRef}
                  id="nuevas-evidencias"
                  type="file"
                  multiple
                  accept=".pdf,.jpg,.jpeg,.png"
                  onChange={(e) => setArchivosNuevos(Array.from(e.target.files ?? []))}
                  disabled={ocupado}
                />
              </div>
              <button type="submit" className="btn" disabled={ocupado}>
                Subir evidencias
              </button>
            </div>
          </form>
        )}
      </div>

      <div className="tarjeta">
        <h2>Historial</h2>
        <ol className="historial">
          {historial.map((h) => (
            <li key={h.id}>
              <EstadoBadge estado={h.estadoNuevo} />
              <span className="historial-meta">
                {formatearFechaHora(h.fecha)} · {h.usuario}
              </span>
              {h.observacion && <p className="historial-observacion">{h.observacion}</p>}
            </li>
          ))}
        </ol>
      </div>

      <Link to={rutaVolver}>← Volver</Link>
    </section>
  );
}

function Dato({ termino, valor }: { termino: string; valor: string }) {
  return (
    <div>
      <dt>{termino}</dt>
      <dd>{valor}</dd>
    </div>
  );
}
