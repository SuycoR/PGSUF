import { useEffect, useState, type ChangeEvent, type FormEvent, type ReactElement } from 'react';
import { useNavigate } from 'react-router';

import { catalogos } from '../../api/catalogos';
import { ApiError } from '../../api/client';
import { solicitudes } from '../../api/solicitudes';
import type { Categoria, Prioridad, TipoSolicitud } from '../../api/types';
import { formatearTamano } from '../../utils/formato';
import './Solicitudes.css';
import {
  MAXIMO_ARCHIVOS,
  validarSolicitud,
  type ErroresSolicitud,
  type FormularioSolicitud,
} from './validarSolicitud';

const VACIO: FormularioSolicitud = {
  categoriaId: '',
  tipoSolicitudId: '',
  prioridadId: '',
  ubicacion: '',
  descripcion: '',
  archivos: [],
};

const CAMPOS_BACKEND: Record<string, keyof FormularioSolicitud> = {
  tipoSolicitudId: 'tipoSolicitudId',
  prioridadId: 'prioridadId',
  descripcion: 'descripcion',
  ubicacion: 'ubicacion',
};

/** HU-04 and HU-15. */
export function NuevaSolicitudPage() {
  const navigate = useNavigate();
  const [categorias, setCategorias] = useState<Categoria[]>([]);
  const [prioridades, setPrioridades] = useState<Prioridad[]>([]);
  const [tipos, setTipos] = useState<TipoSolicitud[]>([]);
  const [form, setForm] = useState<FormularioSolicitud>(VACIO);
  const [errores, setErrores] = useState<ErroresSolicitud>({});
  const [errorServidor, setErrorServidor] = useState<string | null>(null);
  const [enviando, setEnviando] = useState(false);

  useEffect(() => {
    Promise.all([catalogos.categorias(), catalogos.prioridades()])
      .then(([cats, prios]) => {
        setCategorias(cats);
        setPrioridades(prios);
      })
      .catch(() => setErrorServidor('No se pudieron cargar los catálogos. Recargue la página.'));
  }, []);

  useEffect(() => {
    if (!form.categoriaId) {
      return;
    }
    let vigente = true;
    catalogos
      .tiposSolicitud(Number(form.categoriaId))
      .then((lista) => vigente && setTipos(lista))
      .catch(() => vigente && setErrorServidor('No se pudieron cargar los tipos de solicitud.'));
    return () => {
      vigente = false;
    };
  }, [form.categoriaId]);

  // Types of the selected category only (a stale list from a previous category is never shown).
  const tiposVisibles = tipos.filter((t) => String(t.categoriaId) === form.categoriaId);
  const tipoElegido = tiposVisibles.find((t) => String(t.id) === form.tipoSolicitudId);
  const requiereAdjunto = tipoElegido?.requiereAdjunto ?? false;

  function cambiar(campo: keyof FormularioSolicitud) {
    return (e: ChangeEvent<HTMLInputElement | HTMLSelectElement | HTMLTextAreaElement>) => {
      const valor = e.target.value;
      setForm((actual) =>
        campo === 'categoriaId'
          ? { ...actual, categoriaId: valor, tipoSolicitudId: '' } // the types depend on the category
          : { ...actual, [campo]: valor },
      );
    };
  }

  function elegirArchivos(e: ChangeEvent<HTMLInputElement>) {
    const nuevos = Array.from(e.target.files ?? []);
    setForm((actual) => ({ ...actual, archivos: [...actual.archivos, ...nuevos] }));
    e.target.value = ''; // allows choosing the same file again after removing it
  }

  function quitarArchivo(indice: number) {
    setForm((actual) => ({ ...actual, archivos: actual.archivos.filter((_, i) => i !== indice) }));
  }

  async function onSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setErrorServidor(null);
    const encontrados = validarSolicitud(form, requiereAdjunto);
    setErrores(encontrados);
    if (Object.keys(encontrados).length > 0) {
      return; // HU-04 scenario 2
    }

    setEnviando(true);
    try {
      const creada = await solicitudes.registrar(
        {
          tipoSolicitudId: Number(form.tipoSolicitudId),
          prioridadId: Number(form.prioridadId),
          descripcion: form.descripcion.trim(),
          ubicacion: form.ubicacion.trim(),
        },
        form.archivos,
      );
      navigate(`/solicitudes/${creada.id}`, {
        state: { mensaje: `Solicitud ${creada.codigo} registrada en estado "${creada.estado}".` },
      });
    } catch (error) {
      // HU-04 scenario 3: the entered data is kept so the user can retry.
      if (error instanceof ApiError) {
        const delServidor: ErroresSolicitud = {};
        for (const { campo, mensaje } of error.errores) {
          const clave = CAMPOS_BACKEND[campo];
          if (clave) delServidor[clave] = mensaje;
        }
        setErrores(delServidor);
        setErrorServidor(error.message);
      } else {
        setErrorServidor('Ocurrió un error inesperado.');
      }
    } finally {
      setEnviando(false);
    }
  }

  return (
    <section className="tarjeta">
      <h1>Nueva solicitud</h1>

      {errorServidor && (
        <div className="alerta-error" role="alert">
          {errorServidor}
        </div>
      )}

      <form onSubmit={onSubmit} noValidate className="formulario-solicitud">
        <div className="fila-dos">
          <Campo id="categoria" etiqueta="Categoría" error={errores.categoriaId}>
            <select id="categoria" value={form.categoriaId} onChange={cambiar('categoriaId')}>
              <option value="">Seleccione…</option>
              {categorias.map((c) => (
                <option key={c.id} value={c.id}>{c.nombre}</option>
              ))}
            </select>
          </Campo>

          <Campo id="tipo" etiqueta="Tipo de solicitud" error={errores.tipoSolicitudId}>
            <select id="tipo" value={form.tipoSolicitudId} onChange={cambiar('tipoSolicitudId')} disabled={!form.categoriaId}>
              <option value="">{form.categoriaId ? 'Seleccione…' : 'Elija primero la categoría'}</option>
              {tiposVisibles.map((t) => (
                <option key={t.id} value={t.id}>{t.nombre}</option>
              ))}
            </select>
          </Campo>
        </div>

        {tipoElegido && (
          <p className="nota-tipo">
            Se deriva a <strong>{tipoElegido.area}</strong>. Plazo de atención: {tipoElegido.slaHoras} horas.
          </p>
        )}

        <div className="fila-dos">
          <Campo id="ubicacion" etiqueta="Ubicación" error={errores.ubicacion}>
            <input id="ubicacion" placeholder="Ej. Pabellón Antiguo, Salón A-204" value={form.ubicacion} onChange={cambiar('ubicacion')} />
          </Campo>

          <Campo id="prioridad" etiqueta="Prioridad" error={errores.prioridadId}>
            <select id="prioridad" value={form.prioridadId} onChange={cambiar('prioridadId')}>
              <option value="">Seleccione…</option>
              {prioridades.map((p) => (
                <option key={p.id} value={p.id}>{p.nombre}</option>
              ))}
            </select>
          </Campo>
        </div>

        <Campo id="descripcion" etiqueta="Descripción" error={errores.descripcion}>
          <textarea id="descripcion" rows={5} maxLength={2000} value={form.descripcion} onChange={cambiar('descripcion')} />
        </Campo>

        <Campo
          id="archivos"
          etiqueta={requiereAdjunto ? 'Documento de sustento (obligatorio)' : 'Evidencias (opcional)'}
          error={errores.archivos}
          ayuda={`PDF, JPG o PNG. Hasta ${MAXIMO_ARCHIVOS} archivos de 5 MB.`}
        >
          <input id="archivos" type="file" multiple accept=".pdf,.jpg,.jpeg,.png" onChange={elegirArchivos} />
        </Campo>

        {form.archivos.length > 0 && (
          <ul className="lista-archivos">
            {form.archivos.map((archivo, i) => (
              <li key={`${archivo.name}-${i}`}>
                <span>{archivo.name} ({formatearTamano(archivo.size)})</span>
                <button type="button" className="btn btn-pequeno" onClick={() => quitarArchivo(i)}>
                  Quitar
                </button>
              </li>
            ))}
          </ul>
        )}

        <div className="acciones">
          <button type="button" className="btn" onClick={() => navigate('/solicitudes')}>Cancelar</button>
          <button type="submit" className="btn btn-primario" disabled={enviando}>
            {enviando ? 'Registrando…' : 'Registrar solicitud'}
          </button>
        </div>
      </form>
    </section>
  );
}

interface CampoProps {
  id: string;
  etiqueta: string;
  error?: string;
  ayuda?: string;
  children: ReactElement;
}

function Campo({ id, etiqueta, error, ayuda, children }: CampoProps) {
  return (
    <div className="campo">
      <label htmlFor={id}>{etiqueta}</label>
      {children}
      {error ? (
        <span className="campo-error" id={`${id}-error`}>{error}</span>
      ) : (
        ayuda && <span className="campo-ayuda">{ayuda}</span>
      )}
    </div>
  );
}
