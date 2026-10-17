import { useEffect, useState } from 'react';
import { Link, useLocation, useParams } from 'react-router';

import { ApiError } from '../../api/client';
import { solicitudes } from '../../api/solicitudes';
import type { Evidencia, Historial, SolicitudDetalle } from '../../api/types';
import { EstadoBadge } from '../../components/EstadoBadge';
import { formatearFechaHora, formatearTamano } from '../../utils/formato';
import './Solicitudes.css';

interface Datos {
  detalle: SolicitudDetalle;
  historial: Historial[];
  evidencias: Evidencia[];
}

/** HU-06. */
export function SolicitudDetallePage() {
  const { id } = useParams();
  const location = useLocation();
  const mensaje = (location.state as { mensaje?: string } | null)?.mensaje;
  const [datos, setDatos] = useState<Datos | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [errorDescarga, setErrorDescarga] = useState<string | null>(null);

  useEffect(() => {
    const solicitudId = Number(id);
    let vigente = true;
    Promise.all([solicitudes.detalle(solicitudId), solicitudes.historial(solicitudId), solicitudes.evidencias(solicitudId)])
      .then(([detalle, historial, evidencias]) => vigente && setDatos({ detalle, historial, evidencias }))
      .catch((e) => {
        if (!vigente) return;
        setError(e instanceof ApiError && e.status === 404 ? 'Solicitud no encontrada.' : 'No se pudo cargar la solicitud.');
      });
    return () => {
      vigente = false;
    };
  }, [id]);

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

  const { detalle, historial, evidencias } = datos;
  return (
    <section className="detalle">
      {mensaje && <div className="alerta-exito" role="status">{mensaje}</div>}

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

      <Link to="/solicitudes">← Volver a mis solicitudes</Link>
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
