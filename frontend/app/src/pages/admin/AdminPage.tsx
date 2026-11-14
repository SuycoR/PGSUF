import { useEffect, useState, type FormEvent } from 'react';
import { adminApi } from '../../api/admin';
import type {
  AmbienteAdmin,
  AreaAdmin,
  CategoriaAdmin,
  EstadoAdmin,
  PrioridadAdmin,
  TipoSolicitudAdmin,
  TransicionAdmin,
  UsuarioAdmin,
} from '../../api/types';
import './Admin.css';

type TabTipo =
  | 'usuarios'
  | 'areas'
  | 'categorias'
  | 'tipos'
  | 'prioridades'
  | 'estados'
  | 'transiciones'
  | 'ambientes';

const ROLES_DISPONIBLES = ['SOLICITANTE', 'TECNICO', 'PERSONAL_AREA', 'SUPERVISOR', 'ADMIN'];

export function AdminPage() {
  const [tabActiva, setTabActiva] = useState<TabTipo>('usuarios');
  const [cargando, setCargando] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [mensajeExito, setMensajeExito] = useState<string | null>(null);

  // Datos
  const [usuarios, setUsuarios] = useState<UsuarioAdmin[]>([]);
  const [areas, setAreas] = useState<AreaAdmin[]>([]);
  const [categorias, setCategorias] = useState<CategoriaAdmin[]>([]);
  const [tipos, setTipos] = useState<TipoSolicitudAdmin[]>([]);
  const [prioridades, setPrioridades] = useState<PrioridadAdmin[]>([]);
  const [estados, setEstados] = useState<EstadoAdmin[]>([]);
  const [transiciones, setTransiciones] = useState<TransicionAdmin[]>([]);
  const [ambientes, setAmbientes] = useState<AmbienteAdmin[]>([]);

  // Búsqueda
  const [busqueda, setBusqueda] = useState('');

  // Modal genérico
  const [modalAbierto, setModalAbierto] = useState(false);
  const [modoEdicion, setModoEdicion] = useState(false);
  const [itemEditandoId, setItemEditandoId] = useState<number | null>(null);

  // Estados de formularios individuales
  const [formUsuario, setFormUsuario] = useState({
    nombre: '',
    correo: '',
    password: '',
    rol: 'SOLICITANTE',
    areaId: '' as string | number,
  });

  const [formArea, setFormArea] = useState({ nombre: '' });
  const [formCategoria, setFormCategoria] = useState({ nombre: '' });
  const [formTipo, setFormTipo] = useState({
    categoriaId: '' as string | number,
    areaId: '' as string | number,
    nombre: '',
    requiereAdjunto: false,
    slaHoras: 24,
  });
  const [formPrioridad, setFormPrioridad] = useState({ nombre: '', orden: 1 });
  const [formEstado, setFormEstado] = useState({
    nombre: '',
    ambito: 'SOLICITUD',
    orden: 1,
    esFinal: false,
  });
  const [formTransicion, setFormTransicion] = useState({
    estadoOrigenId: '' as string | number,
    estadoDestinoId: '' as string | number,
    rolId: '' as string | number,
    ambito: 'SOLICITUD',
    requiereMotivo: false,
  });
  const [formAmbiente, setFormAmbiente] = useState({
    nombre: '',
    tipo: 'SALON',
    pabellon: 'ANTIGUO',
    piso: 1 as string | number,
    capacidad: 30,
  });

  const limpiarMensajes = () => {
    setError(null);
    setMensajeExito(null);
  };

  // Carga de catálogos auxiliares necesarios para selects
  const cargarAuxiliares = () => {
    adminApi.listarAreas().then(setAreas).catch(() => {});
    adminApi.listarCategorias().then(setCategorias).catch(() => {});
    adminApi.listarEstados().then(setEstados).catch(() => {});
  };

  const cargarDatos = (tab = tabActiva) => {
    setCargando(true);

    switch (tab) {
      case 'usuarios':
        adminApi
          .listarUsuarios(0, 100)
          .then((res) => setUsuarios(res.content))
          .catch((e: Error) => setError(e.message))
          .finally(() => setCargando(false));
        break;
      case 'areas':
        adminApi
          .listarAreas()
          .then(setAreas)
          .catch((e: Error) => setError(e.message))
          .finally(() => setCargando(false));
        break;
      case 'categorias':
        adminApi
          .listarCategorias()
          .then(setCategorias)
          .catch((e: Error) => setError(e.message))
          .finally(() => setCargando(false));
        break;
      case 'tipos':
        adminApi
          .listarTiposSolicitud()
          .then(setTipos)
          .catch((e: Error) => setError(e.message))
          .finally(() => setCargando(false));
        break;
      case 'prioridades':
        adminApi
          .listarPrioridades()
          .then(setPrioridades)
          .catch((e: Error) => setError(e.message))
          .finally(() => setCargando(false));
        break;
      case 'estados':
        adminApi
          .listarEstados()
          .then(setEstados)
          .catch((e: Error) => setError(e.message))
          .finally(() => setCargando(false));
        break;
      case 'transiciones':
        adminApi
          .listarTransiciones()
          .then(setTransiciones)
          .catch((e: Error) => setError(e.message))
          .finally(() => setCargando(false));
        break;
      case 'ambientes':
        adminApi
          .listarAmbientes()
          .then(setAmbientes)
          .catch((e: Error) => setError(e.message))
          .finally(() => setCargando(false));
        break;
    }
  };

  useEffect(() => {
    cargarAuxiliares();
    cargarDatos(tabActiva);
    setBusqueda('');
  }, [tabActiva]);

  // Manejo de abrir modal en modo creación
  const abrirModalCrear = () => {
    limpiarMensajes();
    setModoEdicion(false);
    setItemEditandoId(null);

    switch (tabActiva) {
      case 'usuarios':
        setFormUsuario({ nombre: '', correo: '', password: '', rol: 'SOLICITANTE', areaId: '' });
        break;
      case 'areas':
        setFormArea({ nombre: '' });
        break;
      case 'categorias':
        setFormCategoria({ nombre: '' });
        break;
      case 'tipos':
        setFormTipo({
          categoriaId: categorias[0]?.id || '',
          areaId: areas[0]?.id || '',
          nombre: '',
          requiereAdjunto: false,
          slaHoras: 24,
        });
        break;
      case 'prioridades':
        setFormPrioridad({ nombre: '', orden: prioridades.length + 1 });
        break;
      case 'estados':
        setFormEstado({ nombre: '', ambito: 'SOLICITUD', orden: estados.length + 1, esFinal: false });
        break;
      case 'transiciones':
        setFormTransicion({
          estadoOrigenId: estados[0]?.id || '',
          estadoDestinoId: estados[1]?.id || '',
          rolId: '',
          ambito: 'SOLICITUD',
          requiereMotivo: false,
        });
        break;
      case 'ambientes':
        setFormAmbiente({ nombre: '', tipo: 'SALON', pabellon: 'ANTIGUO', piso: 1, capacidad: 30 });
        break;
    }

    setModalAbierto(true);
  };

  // Manejo de guardar
  const guardar = async (e: FormEvent) => {
    e.preventDefault();
    limpiarMensajes();

    try {
      if (tabActiva === 'usuarios') {
        const areaNum = formUsuario.areaId ? Number(formUsuario.areaId) : null;
        if (modoEdicion && itemEditandoId) {
          await adminApi.actualizarUsuario(itemEditandoId, {
            nombre: formUsuario.nombre,
            correo: formUsuario.correo,
            password: formUsuario.password || undefined,
            rol: formUsuario.rol,
            areaId: areaNum,
          });
          setMensajeExito('Usuario actualizado exitosamente');
        } else {
          await adminApi.crearUsuario({
            nombre: formUsuario.nombre,
            correo: formUsuario.correo,
            password: formUsuario.password,
            rol: formUsuario.rol,
            areaId: areaNum,
          });
          setMensajeExito('Usuario creado exitosamente');
        }
      } else if (tabActiva === 'areas') {
        if (modoEdicion && itemEditandoId) {
          await adminApi.actualizarArea(itemEditandoId, formArea.nombre);
          setMensajeExito('Área actualizada exitosamente');
        } else {
          await adminApi.crearArea(formArea.nombre);
          setMensajeExito('Área creada exitosamente');
        }
      } else if (tabActiva === 'categorias') {
        if (modoEdicion && itemEditandoId) {
          await adminApi.actualizarCategoria(itemEditandoId, formCategoria.nombre);
          setMensajeExito('Categoría actualizada exitosamente');
        } else {
          await adminApi.crearCategoria(formCategoria.nombre);
          setMensajeExito('Categoría creada exitosamente');
        }
      } else if (tabActiva === 'tipos') {
        const payload = {
          categoriaId: Number(formTipo.categoriaId),
          areaId: Number(formTipo.areaId),
          nombre: formTipo.nombre,
          requiereAdjunto: formTipo.requiereAdjunto,
          slaHoras: Number(formTipo.slaHoras),
        };
        if (modoEdicion && itemEditandoId) {
          await adminApi.actualizarTipoSolicitud(itemEditandoId, payload);
          setMensajeExito('Tipo de solicitud actualizado');
        } else {
          await adminApi.crearTipoSolicitud(payload);
          setMensajeExito('Tipo de solicitud creado');
        }
      } else if (tabActiva === 'prioridades') {
        const payload = { nombre: formPrioridad.nombre, orden: Number(formPrioridad.orden) };
        if (modoEdicion && itemEditandoId) {
          await adminApi.actualizarPrioridad(itemEditandoId, payload);
          setMensajeExito('Prioridad actualizada');
        } else {
          await adminApi.crearPrioridad(payload);
          setMensajeExito('Prioridad creada');
        }
      } else if (tabActiva === 'estados') {
        const payload = {
          nombre: formEstado.nombre,
          ambito: formEstado.ambito,
          orden: Number(formEstado.orden),
          esFinal: formEstado.esFinal,
        };
        if (modoEdicion && itemEditandoId) {
          await adminApi.actualizarEstado(itemEditandoId, payload);
          setMensajeExito('Estado actualizado');
        } else {
          await adminApi.crearEstado(payload);
          setMensajeExito('Estado creado');
        }
      } else if (tabActiva === 'transiciones') {
        const payload = {
          estadoOrigenId: Number(formTransicion.estadoOrigenId),
          estadoDestinoId: Number(formTransicion.estadoDestinoId),
          rolId: formTransicion.rolId ? Number(formTransicion.rolId) : null,
          ambito: formTransicion.ambito,
          requiereMotivo: formTransicion.requiereMotivo,
        };
        if (modoEdicion && itemEditandoId) {
          await adminApi.actualizarTransicion(itemEditandoId, payload);
          setMensajeExito('Transición actualizada');
        } else {
          await adminApi.crearTransicion(payload);
          setMensajeExito('Transición creada');
        }
      } else if (tabActiva === 'ambientes') {
        const payload = {
          nombre: formAmbiente.nombre,
          tipo: formAmbiente.tipo,
          pabellon: formAmbiente.pabellon,
          piso: formAmbiente.piso ? Number(formAmbiente.piso) : null,
          capacidad: Number(formAmbiente.capacidad),
        };
        if (modoEdicion && itemEditandoId) {
          await adminApi.actualizarAmbiente(itemEditandoId, payload);
          setMensajeExito('Ambiente actualizado');
        } else {
          await adminApi.crearAmbiente(payload);
          setMensajeExito('Ambiente creado');
        }
      }

      setModalAbierto(false);
      cargarDatos(tabActiva);
    } catch (err: unknown) {
      if (err instanceof Error) {
        setError(err.message);
      } else {
        setError('Ocurrió un error inesperado al guardar.');
      }
    }
  };

  // Toggle Activo
  const toggleActivo = async (id: number, nuevoEstado: boolean) => {
    limpiarMensajes();
    try {
      if (tabActiva === 'usuarios') {
        await adminApi.cambiarActivoUsuario(id, nuevoEstado);
      } else if (tabActiva === 'areas') {
        await adminApi.cambiarActivoArea(id, nuevoEstado);
      } else if (tabActiva === 'categorias') {
        await adminApi.cambiarActivoCategoria(id, nuevoEstado);
      } else if (tabActiva === 'tipos') {
        await adminApi.cambiarActivoTipoSolicitud(id, nuevoEstado);
      } else if (tabActiva === 'ambientes') {
        await adminApi.cambiarActivoAmbiente(id, nuevoEstado);
      }
      setMensajeExito(`Registro ${nuevoEstado ? 'activado' : 'desactivado'} con éxito`);
      cargarDatos(tabActiva);
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : 'Error al cambiar estado activo.');
    }
  };

  // Eliminar transición
  const eliminarTransicion = async (id: number) => {
    if (!window.confirm('¿Está seguro de eliminar esta regla de transición?')) return;
    limpiarMensajes();
    try {
      await adminApi.eliminarTransicion(id);
      setMensajeExito('Transición eliminada exitosamente');
      cargarDatos(tabActiva);
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : 'Error al eliminar transición.');
    }
  };

  return (
    <div className="admin-seccion">
      <div className="admin-header">
        <div>
          <h2>Administración del Sistema</h2>
          <p>Módulo de configuración de usuarios, áreas y catálogos de servicio (HU-12)</p>
        </div>
      </div>

      {mensajeExito && <div className="alerta-exito">{mensajeExito}</div>}
      {error && <div className="alerta-error">{error}</div>}

      <div className="admin-tabs" role="tablist">
        {(
          [
            ['usuarios', '👥 Usuarios'],
            ['areas', '🏢 Áreas'],
            ['categorias', '📁 Categorías'],
            ['tipos', '📑 Tipos de Solicitud'],
            ['prioridades', '⚡ Prioridades'],
            ['estados', '🔄 Estados'],
            ['transiciones', '🔀 Transiciones'],
            ['ambientes', '🏛️ Ambientes'],
          ] as [TabTipo, string][]
        ).map(([key, label]) => (
          <button
            key={key}
            type="button"
            className={`admin-tab-btn ${tabActiva === key ? 'activo' : ''}`}
            onClick={() => setTabActiva(key)}
          >
            {label}
          </button>
        ))}
      </div>

      <div className="tarjeta">
        <div className="admin-toolbar">
          <input
            type="search"
            className="campo admin-search"
            placeholder={`Buscar en ${tabActiva}...`}
            value={busqueda}
            onChange={(e) => setBusqueda(e.target.value)}
          />
          <button type="button" className="btn btn-primario" onClick={abrirModalCrear}>
            + Nuevo registro
          </button>
        </div>

        {cargando ? (
          <p>Cargando información...</p>
        ) : (
          <div className="admin-tabla-wrapper">
            {/* Tabla Usuarios */}
            {tabActiva === 'usuarios' && (
              <table className="admin-tabla">
                <thead>
                  <tr>
                    <th>ID</th>
                    <th>Nombre</th>
                    <th>Correo</th>
                    <th>Rol</th>
                    <th>Área</th>
                    <th>Estado</th>
                    <th>Acciones</th>
                  </tr>
                </thead>
                <tbody>
                  {usuarios
                    .filter(
                      (u) =>
                        u.nombre.toLowerCase().includes(busqueda.toLowerCase()) ||
                        u.correo.toLowerCase().includes(busqueda.toLowerCase()) ||
                        u.rol.toLowerCase().includes(busqueda.toLowerCase())
                    )
                    .map((u) => {
                      const areaNombre = areas.find((a) => a.id === u.areaId)?.nombre || '—';
                      return (
                        <tr key={u.id}>
                          <td>{u.id}</td>
                          <td><strong>{u.nombre}</strong></td>
                          <td>{u.correo}</td>
                          <td>{u.rol}</td>
                          <td>{areaNombre}</td>
                          <td>
                            <span className={u.activo ? 'badge-activo' : 'badge-inactivo'}>
                              {u.activo ? 'Activo' : 'Inactivo'}
                            </span>
                          </td>
                          <td>
                            <div className="admin-acciones">
                              <button
                                type="button"
                                className="btn btn-sm"
                                onClick={() => {
                                  setModoEdicion(true);
                                  setItemEditandoId(u.id);
                                  setFormUsuario({
                                    nombre: u.nombre,
                                    correo: u.correo,
                                    password: '',
                                    rol: u.rol,
                                    areaId: u.areaId || '',
                                  });
                                  setModalAbierto(true);
                                }}
                              >
                                Editar
                              </button>
                              <button
                                type="button"
                                className={`btn btn-sm ${u.activo ? 'btn-peligro' : ''}`}
                                onClick={() => toggleActivo(u.id, !u.activo)}
                              >
                                {u.activo ? 'Desactivar' : 'Activar'}
                              </button>
                            </div>
                          </td>
                        </tr>
                      );
                    })}
                </tbody>
              </table>
            )}

            {/* Tabla Áreas */}
            {tabActiva === 'areas' && (
              <table className="admin-tabla">
                <thead>
                  <tr>
                    <th>ID</th>
                    <th>Nombre</th>
                    <th>Estado</th>
                    <th>Acciones</th>
                  </tr>
                </thead>
                <tbody>
                  {areas
                    .filter((a) => a.nombre.toLowerCase().includes(busqueda.toLowerCase()))
                    .map((a) => (
                      <tr key={a.id}>
                        <td>{a.id}</td>
                        <td><strong>{a.nombre}</strong></td>
                        <td>
                          <span className={a.activo ? 'badge-activo' : 'badge-inactivo'}>
                            {a.activo ? 'Activo' : 'Inactivo'}
                          </span>
                        </td>
                        <td>
                          <div className="admin-acciones">
                            <button
                              type="button"
                              className="btn btn-sm"
                              onClick={() => {
                                setModoEdicion(true);
                                setItemEditandoId(a.id);
                                setFormArea({ nombre: a.nombre });
                                setModalAbierto(true);
                              }}
                            >
                              Editar
                            </button>
                            <button
                              type="button"
                              className={`btn btn-sm ${a.activo ? 'btn-peligro' : ''}`}
                              onClick={() => toggleActivo(a.id, !a.activo)}
                            >
                              {a.activo ? 'Desactivar' : 'Activar'}
                            </button>
                          </div>
                        </td>
                      </tr>
                    ))}
                </tbody>
              </table>
            )}

            {/* Tabla Categorías */}
            {tabActiva === 'categorias' && (
              <table className="admin-tabla">
                <thead>
                  <tr>
                    <th>ID</th>
                    <th>Nombre</th>
                    <th>Tipo</th>
                    <th>Estado</th>
                    <th>Acciones</th>
                  </tr>
                </thead>
                <tbody>
                  {categorias
                    .filter((c) => c.nombre.toLowerCase().includes(busqueda.toLowerCase()))
                    .map((c) => (
                      <tr key={c.id}>
                        <td>{c.id}</td>
                        <td><strong>{c.nombre}</strong></td>
                        <td>{c.esTramite ? 'Trámite Académico' : 'Soporte / Infraestructura'}</td>
                        <td>
                          <span className={c.activo ? 'badge-activo' : 'badge-inactivo'}>
                            {c.activo ? 'Activo' : 'Inactivo'}
                          </span>
                        </td>
                        <td>
                          <div className="admin-acciones">
                            <button
                              type="button"
                              className="btn btn-sm"
                              onClick={() => {
                                setModoEdicion(true);
                                setItemEditandoId(c.id);
                                setFormCategoria({ nombre: c.nombre });
                                setModalAbierto(true);
                              }}
                            >
                              Editar
                            </button>
                            <button
                              type="button"
                              className={`btn btn-sm ${c.activo ? 'btn-peligro' : ''}`}
                              onClick={() => toggleActivo(c.id, !c.activo)}
                            >
                              {c.activo ? 'Desactivar' : 'Activar'}
                            </button>
                          </div>
                        </td>
                      </tr>
                    ))}
                </tbody>
              </table>
            )}

            {/* Tabla Tipos de Solicitud */}
            {tabActiva === 'tipos' && (
              <table className="admin-tabla">
                <thead>
                  <tr>
                    <th>ID</th>
                    <th>Nombre</th>
                    <th>Categoría</th>
                    <th>Área</th>
                    <th>SLA (horas)</th>
                    <th>Adjunto</th>
                    <th>Estado</th>
                    <th>Acciones</th>
                  </tr>
                </thead>
                <tbody>
                  {tipos
                    .filter(
                      (t) =>
                        t.nombre.toLowerCase().includes(busqueda.toLowerCase()) ||
                        t.categoriaNombre.toLowerCase().includes(busqueda.toLowerCase())
                    )
                    .map((t) => (
                      <tr key={t.id}>
                        <td>{t.id}</td>
                        <td><strong>{t.nombre}</strong></td>
                        <td>{t.categoriaNombre}</td>
                        <td>{t.areaNombre}</td>
                        <td>{t.slaHoras} h</td>
                        <td>{t.requiereAdjunto ? 'Obligatorio' : 'Opcional'}</td>
                        <td>
                          <span className={t.activo ? 'badge-activo' : 'badge-inactivo'}>
                            {t.activo ? 'Activo' : 'Inactivo'}
                          </span>
                        </td>
                        <td>
                          <div className="admin-acciones">
                            <button
                              type="button"
                              className="btn btn-sm"
                              onClick={() => {
                                setModoEdicion(true);
                                setItemEditandoId(t.id);
                                setFormTipo({
                                  categoriaId: t.categoriaId,
                                  areaId: t.areaId,
                                  nombre: t.nombre,
                                  requiereAdjunto: t.requiereAdjunto,
                                  slaHoras: t.slaHoras,
                                });
                                setModalAbierto(true);
                              }}
                            >
                              Editar
                            </button>
                            <button
                              type="button"
                              className={`btn btn-sm ${t.activo ? 'btn-peligro' : ''}`}
                              onClick={() => toggleActivo(t.id, !t.activo)}
                            >
                              {t.activo ? 'Desactivar' : 'Activar'}
                            </button>
                          </div>
                        </td>
                      </tr>
                    ))}
                </tbody>
              </table>
            )}

            {/* Tabla Prioridades */}
            {tabActiva === 'prioridades' && (
              <table className="admin-tabla">
                <thead>
                  <tr>
                    <th>ID</th>
                    <th>Nombre</th>
                    <th>Nivel / Orden</th>
                    <th>Acciones</th>
                  </tr>
                </thead>
                <tbody>
                  {prioridades
                    .filter((p) => p.nombre.toLowerCase().includes(busqueda.toLowerCase()))
                    .map((p) => (
                      <tr key={p.id}>
                        <td>{p.id}</td>
                        <td><strong>{p.nombre}</strong></td>
                        <td>Nivel {p.orden}</td>
                        <td>
                          <button
                            type="button"
                            className="btn btn-sm"
                            onClick={() => {
                              setModoEdicion(true);
                              setItemEditandoId(p.id);
                              setFormPrioridad({ nombre: p.nombre, orden: p.orden });
                              setModalAbierto(true);
                            }}
                          >
                            Editar
                          </button>
                        </td>
                      </tr>
                    ))}
                </tbody>
              </table>
            )}

            {/* Tabla Estados */}
            {tabActiva === 'estados' && (
              <table className="admin-tabla">
                <thead>
                  <tr>
                    <th>ID</th>
                    <th>Nombre</th>
                    <th>Ámbito</th>
                    <th>Orden</th>
                    <th>¿Es Final?</th>
                    <th>Acciones</th>
                  </tr>
                </thead>
                <tbody>
                  {estados
                    .filter(
                      (e) =>
                        e.nombre.toLowerCase().includes(busqueda.toLowerCase()) ||
                        e.ambito.toLowerCase().includes(busqueda.toLowerCase())
                    )
                    .map((e) => (
                      <tr key={e.id}>
                        <td>{e.id}</td>
                        <td><strong>{e.nombre}</strong></td>
                        <td>{e.ambito}</td>
                        <td>{e.orden}</td>
                        <td>{e.esFinal ? 'Sí' : 'No'}</td>
                        <td>
                          <button
                            type="button"
                            className="btn btn-sm"
                            onClick={() => {
                              setModoEdicion(true);
                              setItemEditandoId(e.id);
                              setFormEstado({
                                nombre: e.nombre,
                                ambito: e.ambito,
                                orden: e.orden,
                                esFinal: e.esFinal,
                              });
                              setModalAbierto(true);
                            }}
                          >
                            Editar
                          </button>
                        </td>
                      </tr>
                    ))}
                </tbody>
              </table>
            )}

            {/* Tabla Transiciones */}
            {tabActiva === 'transiciones' && (
              <table className="admin-tabla">
                <thead>
                  <tr>
                    <th>ID</th>
                    <th>Estado Origen</th>
                    <th>Estado Destino</th>
                    <th>Rol Permitido</th>
                    <th>Ámbito</th>
                    <th>Requiere Motivo</th>
                    <th>Acciones</th>
                  </tr>
                </thead>
                <tbody>
                  {transiciones
                    .filter(
                      (tr) =>
                        tr.estadoOrigenNombre.toLowerCase().includes(busqueda.toLowerCase()) ||
                        tr.estadoDestinoNombre.toLowerCase().includes(busqueda.toLowerCase()) ||
                        (tr.rolNombre || '').toLowerCase().includes(busqueda.toLowerCase())
                    )
                    .map((tr) => (
                      <tr key={tr.id}>
                        <td>{tr.id}</td>
                        <td>{tr.estadoOrigenNombre}</td>
                        <td><strong>{tr.estadoDestinoNombre}</strong></td>
                        <td>{tr.rolNombre || 'Sistema (Automático)'}</td>
                        <td>{tr.ambito}</td>
                        <td>{tr.requiereMotivo ? 'Sí' : 'No'}</td>
                        <td>
                          <div className="admin-acciones">
                            <button
                              type="button"
                              className="btn btn-sm"
                              onClick={() => {
                                setModoEdicion(true);
                                setItemEditandoId(tr.id);
                                setFormTransicion({
                                  estadoOrigenId: tr.estadoOrigenId,
                                  estadoDestinoId: tr.estadoDestinoId,
                                  rolId: tr.rolId || '',
                                  ambito: tr.ambito,
                                  requiereMotivo: tr.requiereMotivo,
                                });
                                setModalAbierto(true);
                              }}
                            >
                              Editar
                            </button>
                            <button
                              type="button"
                              className="btn btn-sm btn-peligro"
                              onClick={() => eliminarTransicion(tr.id)}
                            >
                              Eliminar
                            </button>
                          </div>
                        </td>
                      </tr>
                    ))}
                </tbody>
              </table>
            )}

            {/* Tabla Ambientes */}
            {tabActiva === 'ambientes' && (
              <table className="admin-tabla">
                <thead>
                  <tr>
                    <th>ID</th>
                    <th>Nombre</th>
                    <th>Tipo</th>
                    <th>Pabellón</th>
                    <th>Piso</th>
                    <th>Capacidad</th>
                    <th>Estado</th>
                    <th>Acciones</th>
                  </tr>
                </thead>
                <tbody>
                  {ambientes
                    .filter(
                      (a) =>
                        a.nombre.toLowerCase().includes(busqueda.toLowerCase()) ||
                        a.pabellon.toLowerCase().includes(busqueda.toLowerCase())
                    )
                    .map((a) => (
                      <tr key={a.id}>
                        <td>{a.id}</td>
                        <td><strong>{a.nombre}</strong></td>
                        <td>{a.tipo}</td>
                        <td>{a.pabellon}</td>
                        <td>{a.piso ? `Piso ${a.piso}` : '—'}</td>
                        <td>{a.capacidad} personas</td>
                        <td>
                          <span className={a.activo ? 'badge-activo' : 'badge-inactivo'}>
                            {a.activo ? 'Activo' : 'Inactivo'}
                          </span>
                        </td>
                        <td>
                          <div className="admin-acciones">
                            <button
                              type="button"
                              className="btn btn-sm"
                              onClick={() => {
                                setModoEdicion(true);
                                setItemEditandoId(a.id);
                                setFormAmbiente({
                                  nombre: a.nombre,
                                  tipo: a.tipo,
                                  pabellon: a.pabellon,
                                  piso: a.piso || '',
                                  capacidad: a.capacidad,
                                });
                                setModalAbierto(true);
                              }}
                            >
                              Editar
                            </button>
                            <button
                              type="button"
                              className={`btn btn-sm ${a.activo ? 'btn-peligro' : ''}`}
                              onClick={() => toggleActivo(a.id, !a.activo)}
                            >
                              {a.activo ? 'Desactivar' : 'Activar'}
                            </button>
                          </div>
                        </td>
                      </tr>
                    ))}
                </tbody>
              </table>
            )}
          </div>
        )}
      </div>

      {/* Modal Dialog */}
      {modalAbierto && (
        <div className="modal-overlay" onClick={() => setModalAbierto(false)}>
          <div className="modal-contenido" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h3>
                {modoEdicion ? 'Editar' : 'Nuevo'} {tabActiva.slice(0, -1)}
              </h3>
              <button
                type="button"
                className="modal-cerrar"
                onClick={() => setModalAbierto(false)}
              >
                &times;
              </button>
            </div>
            <form onSubmit={guardar}>
              <div className="modal-body">
                {/* Form Usuario */}
                {tabActiva === 'usuarios' && (
                  <>
                    <div className="campo">
                      <label htmlFor="usr-nombre">Nombre completo</label>
                      <input
                        id="usr-nombre"
                        type="text"
                        required
                        value={formUsuario.nombre}
                        onChange={(e) => setFormUsuario({ ...formUsuario, nombre: e.target.value })}
                      />
                    </div>
                    <div className="campo">
                      <label htmlFor="usr-correo">Correo institucional</label>
                      <input
                        id="usr-correo"
                        type="email"
                        required
                        value={formUsuario.correo}
                        onChange={(e) => setFormUsuario({ ...formUsuario, correo: e.target.value })}
                      />
                    </div>
                    <div className="campo">
                      <label htmlFor="usr-pass">
                        {modoEdicion ? 'Contraseña (en blanco para mantener)' : 'Contraseña'}
                      </label>
                      <input
                        id="usr-pass"
                        type="password"
                        required={!modoEdicion}
                        minLength={8}
                        value={formUsuario.password}
                        onChange={(e) => setFormUsuario({ ...formUsuario, password: e.target.value })}
                      />
                    </div>
                    <div className="campo">
                      <label htmlFor="usr-rol">Rol asignado</label>
                      <select
                        id="usr-rol"
                        value={formUsuario.rol}
                        onChange={(e) => setFormUsuario({ ...formUsuario, rol: e.target.value })}
                      >
                        {ROLES_DISPONIBLES.map((r) => (
                          <option key={r} value={r}>
                            {r}
                          </option>
                        ))}
                      </select>
                    </div>
                    {(formUsuario.rol === 'TECNICO' || formUsuario.rol === 'PERSONAL_AREA') && (
                      <div className="campo">
                        <label htmlFor="usr-area">Área a la que pertenece (RN-10)</label>
                        <select
                          id="usr-area"
                          required
                          value={formUsuario.areaId}
                          onChange={(e) => setFormUsuario({ ...formUsuario, areaId: e.target.value })}
                        >
                          <option value="">Seleccione un área...</option>
                          {areas.map((a) => (
                            <option key={a.id} value={a.id}>
                              {a.nombre}
                            </option>
                          ))}
                        </select>
                      </div>
                    )}
                  </>
                )}

                {/* Form Área */}
                {tabActiva === 'areas' && (
                  <div className="campo">
                    <label htmlFor="area-nombre">Nombre del Área</label>
                    <input
                      id="area-nombre"
                      type="text"
                      required
                      value={formArea.nombre}
                      onChange={(e) => setFormArea({ nombre: e.target.value })}
                    />
                  </div>
                )}

                {/* Form Categoría */}
                {tabActiva === 'categorias' && (
                  <div className="campo">
                    <label htmlFor="cat-nombre">Nombre de la Categoría</label>
                    <input
                      id="cat-nombre"
                      type="text"
                      required
                      value={formCategoria.nombre}
                      onChange={(e) => setFormCategoria({ nombre: e.target.value })}
                    />
                  </div>
                )}

                {/* Form Tipo Solicitud */}
                {tabActiva === 'tipos' && (
                  <>
                    <div className="campo">
                      <label htmlFor="tipo-nombre">Nombre del Tipo de Solicitud</label>
                      <input
                        id="tipo-nombre"
                        type="text"
                        required
                        value={formTipo.nombre}
                        onChange={(e) => setFormTipo({ ...formTipo, nombre: e.target.value })}
                      />
                    </div>
                    <div className="campo">
                      <label htmlFor="tipo-cat">Categoría</label>
                      <select
                        id="tipo-cat"
                        required
                        value={formTipo.categoriaId}
                        onChange={(e) => setFormTipo({ ...formTipo, categoriaId: e.target.value })}
                      >
                        <option value="">Seleccione categoría...</option>
                        {categorias.map((c) => (
                          <option key={c.id} value={c.id}>
                            {c.nombre}
                          </option>
                        ))}
                      </select>
                    </div>
                    <div className="campo">
                      <label htmlFor="tipo-area">Área responsable</label>
                      <select
                        id="tipo-area"
                        required
                        value={formTipo.areaId}
                        onChange={(e) => setFormTipo({ ...formTipo, areaId: e.target.value })}
                      >
                        <option value="">Seleccione área...</option>
                        {areas.map((a) => (
                          <option key={a.id} value={a.id}>
                            {a.nombre}
                          </option>
                        ))}
                      </select>
                    </div>
                    <div className="campo">
                      <label htmlFor="tipo-sla">SLA (Horas límite de atención)</label>
                      <input
                        id="tipo-sla"
                        type="number"
                        min={1}
                        required
                        value={formTipo.slaHoras}
                        onChange={(e) => setFormTipo({ ...formTipo, slaHoras: Number(e.target.value) })}
                      />
                    </div>
                    <div className="campo" style={{ flexDirection: 'row', alignItems: 'center', gap: '0.5rem' }}>
                      <input
                        id="tipo-adj"
                        type="checkbox"
                        checked={formTipo.requiereAdjunto}
                        onChange={(e) => setFormTipo({ ...formTipo, requiereAdjunto: e.target.checked })}
                      />
                      <label htmlFor="tipo-adj" style={{ margin: 0, fontWeight: 500 }}>
                        Requiere archivo adjunto obligatorio
                      </label>
                    </div>
                  </>
                )}

                {/* Form Prioridad */}
                {tabActiva === 'prioridades' && (
                  <>
                    <div className="campo">
                      <label htmlFor="prio-nombre">Nombre de la Prioridad</label>
                      <input
                        id="prio-nombre"
                        type="text"
                        required
                        value={formPrioridad.nombre}
                        onChange={(e) => setFormPrioridad({ ...formPrioridad, nombre: e.target.value })}
                      />
                    </div>
                    <div className="campo">
                      <label htmlFor="prio-orden">Nivel / Orden (1 = más alta)</label>
                      <input
                        id="prio-orden"
                        type="number"
                        min={1}
                        required
                        value={formPrioridad.orden}
                        onChange={(e) => setFormPrioridad({ ...formPrioridad, orden: Number(e.target.value) })}
                      />
                    </div>
                  </>
                )}

                {/* Form Estado */}
                {tabActiva === 'estados' && (
                  <>
                    <div className="campo">
                      <label htmlFor="est-nombre">Nombre del Estado</label>
                      <input
                        id="est-nombre"
                        type="text"
                        required
                        value={formEstado.nombre}
                        onChange={(e) => setFormEstado({ ...formEstado, nombre: e.target.value })}
                      />
                    </div>
                    <div className="campo">
                      <label htmlFor="est-ambito">Ámbito</label>
                      <select
                        id="est-ambito"
                        value={formEstado.ambito}
                        onChange={(e) => setFormEstado({ ...formEstado, ambito: e.target.value })}
                      >
                        <option value="SOLICITUD">SOLICITUD</option>
                        <option value="RESERVA">RESERVA</option>
                        <option value="AMBOS">AMBOS</option>
                      </select>
                    </div>
                    <div className="campo">
                      <label htmlFor="est-orden">Orden visual</label>
                      <input
                        id="est-orden"
                        type="number"
                        min={1}
                        required
                        value={formEstado.orden}
                        onChange={(e) => setFormEstado({ ...formEstado, orden: Number(e.target.value) })}
                      />
                    </div>
                    <div className="campo" style={{ flexDirection: 'row', alignItems: 'center', gap: '0.5rem' }}>
                      <input
                        id="est-final"
                        type="checkbox"
                        checked={formEstado.esFinal}
                        onChange={(e) => setFormEstado({ ...formEstado, esFinal: e.target.checked })}
                      />
                      <label htmlFor="est-final" style={{ margin: 0, fontWeight: 500 }}>
                        Es estado final / terminal
                      </label>
                    </div>
                  </>
                )}

                {/* Form Transición */}
                {tabActiva === 'transiciones' && (
                  <>
                    <div className="campo">
                      <label htmlFor="tr-origen">Estado Origen</label>
                      <select
                        id="tr-origen"
                        required
                        value={formTransicion.estadoOrigenId}
                        onChange={(e) => setFormTransicion({ ...formTransicion, estadoOrigenId: e.target.value })}
                      >
                        <option value="">Seleccione estado origen...</option>
                        {estados.map((es) => (
                          <option key={es.id} value={es.id}>
                            {es.nombre} ({es.ambito})
                          </option>
                        ))}
                      </select>
                    </div>
                    <div className="campo">
                      <label htmlFor="tr-destino">Estado Destino</label>
                      <select
                        id="tr-destino"
                        required
                        value={formTransicion.estadoDestinoId}
                        onChange={(e) => setFormTransicion({ ...formTransicion, estadoDestinoId: e.target.value })}
                      >
                        <option value="">Seleccione estado destino...</option>
                        {estados.map((es) => (
                          <option key={es.id} value={es.id}>
                            {es.nombre} ({es.ambito})
                          </option>
                        ))}
                      </select>
                    </div>
                    <div className="campo">
                      <label htmlFor="tr-ambito">Ámbito</label>
                      <select
                        id="tr-ambito"
                        value={formTransicion.ambito}
                        onChange={(e) => setFormTransicion({ ...formTransicion, ambito: e.target.value })}
                      >
                        <option value="SOLICITUD">SOLICITUD</option>
                        <option value="RESERVA">RESERVA</option>
                      </select>
                    </div>
                    <div className="campo">
                      <label htmlFor="tr-rol">Rol que ejecuta la transición</label>
                      <select
                        id="tr-rol"
                        value={formTransicion.rolId}
                        onChange={(e) => setFormTransicion({ ...formTransicion, rolId: e.target.value })}
                      >
                        <option value="">Automático por el sistema</option>
                        <option value="1">SOLICITANTE</option>
                        <option value="2">TECNICO</option>
                        <option value="3">PERSONAL_AREA</option>
                        <option value="4">SUPERVISOR</option>
                        <option value="5">ADMIN</option>
                      </select>
                    </div>
                    <div className="campo" style={{ flexDirection: 'row', alignItems: 'center', gap: '0.5rem' }}>
                      <input
                        id="tr-motivo"
                        type="checkbox"
                        checked={formTransicion.requiereMotivo}
                        onChange={(e) => setFormTransicion({ ...formTransicion, requiereMotivo: e.target.checked })}
                      />
                      <label htmlFor="tr-motivo" style={{ margin: 0, fontWeight: 500 }}>
                        Requiere motivo / observación obligatoria (RN-06, RN-13)
                      </label>
                    </div>
                  </>
                )}

                {/* Form Ambiente */}
                {tabActiva === 'ambientes' && (
                  <>
                    <div className="campo">
                      <label htmlFor="amb-nombre">Nombre del Ambiente</label>
                      <input
                        id="amb-nombre"
                        type="text"
                        required
                        value={formAmbiente.nombre}
                        onChange={(e) => setFormAmbiente({ ...formAmbiente, nombre: e.target.value })}
                      />
                    </div>
                    <div className="campo">
                      <label htmlFor="amb-tipo">Tipo de Ambiente</label>
                      <select
                        id="amb-tipo"
                        value={formAmbiente.tipo}
                        onChange={(e) => setFormAmbiente({ ...formAmbiente, tipo: e.target.value })}
                      >
                        <option value="SALON">SALON</option>
                        <option value="LABORATORIO">LABORATORIO</option>
                        <option value="AUDITORIO">AUDITORIO</option>
                      </select>
                    </div>
                    <div className="campo">
                      <label htmlFor="amb-pab">Pabellón</label>
                      <select
                        id="amb-pab"
                        value={formAmbiente.pabellon}
                        onChange={(e) => setFormAmbiente({ ...formAmbiente, pabellon: e.target.value })}
                      >
                        <option value="ANTIGUO">ANTIGUO</option>
                        <option value="NUEVO">NUEVO</option>
                        <option value="CENTRAL">CENTRAL</option>
                      </select>
                    </div>
                    {formAmbiente.tipo !== 'AUDITORIO' && (
                      <div className="campo">
                        <label htmlFor="amb-piso">Piso (1 al 4)</label>
                        <input
                          id="amb-piso"
                          type="number"
                          min={1}
                          max={4}
                          value={formAmbiente.piso}
                          onChange={(e) => setFormAmbiente({ ...formAmbiente, piso: e.target.value })}
                        />
                      </div>
                    )}
                    <div className="campo">
                      <label htmlFor="amb-cap">Capacidad (personas)</label>
                      <input
                        id="amb-cap"
                        type="number"
                        min={1}
                        required
                        value={formAmbiente.capacidad}
                        onChange={(e) => setFormAmbiente({ ...formAmbiente, capacidad: Number(e.target.value) })}
                      />
                    </div>
                  </>
                )}
              </div>
              <div className="modal-footer">
                <button
                  type="button"
                  className="btn"
                  onClick={() => setModalAbierto(false)}
                >
                  Cancelar
                </button>
                <button type="submit" className="btn btn-primario">
                  {modoEdicion ? 'Actualizar' : 'Guardar'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
