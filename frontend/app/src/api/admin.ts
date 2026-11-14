import { apiFetch } from './client';
import type {
  ActualizarUsuarioAdminRequest,
  AmbienteAdmin,
  AreaAdmin,
  CategoriaAdmin,
  CrearUsuarioAdminRequest,
  EstadoAdmin,
  Page,
  PrioridadAdmin,
  TipoSolicitudAdmin,
  TransicionAdmin,
  UsuarioAdmin,
} from './types';

export const adminApi = {
  // Usuarios
  listarUsuarios(page = 0, size = 20) {
    return apiFetch<Page<UsuarioAdmin>>(`/admin/usuarios?page=${page}&size=${size}`);
  },
  obtenerUsuario(id: number) {
    return apiFetch<UsuarioAdmin>(`/admin/usuarios/${id}`);
  },
  crearUsuario(datos: CrearUsuarioAdminRequest) {
    return apiFetch<UsuarioAdmin>('/admin/usuarios', {
      method: 'POST',
      body: JSON.stringify(datos),
    });
  },
  actualizarUsuario(id: number, datos: ActualizarUsuarioAdminRequest) {
    return apiFetch<UsuarioAdmin>(`/admin/usuarios/${id}`, {
      method: 'PUT',
      body: JSON.stringify(datos),
    });
  },
  cambiarActivoUsuario(id: number, activo: boolean) {
    return apiFetch<UsuarioAdmin>(`/admin/usuarios/${id}/activo`, {
      method: 'PUT',
      body: JSON.stringify({ activo }),
    });
  },

  // Áreas
  listarAreas() {
    return apiFetch<AreaAdmin[]>('/admin/areas');
  },
  crearArea(nombre: string) {
    return apiFetch<AreaAdmin>('/admin/areas', {
      method: 'POST',
      body: JSON.stringify({ nombre }),
    });
  },
  actualizarArea(id: number, nombre: string) {
    return apiFetch<AreaAdmin>(`/admin/areas/${id}`, {
      method: 'PUT',
      body: JSON.stringify({ nombre }),
    });
  },
  cambiarActivoArea(id: number, activo: boolean) {
    return apiFetch<AreaAdmin>(`/admin/areas/${id}/activo`, {
      method: 'PUT',
      body: JSON.stringify({ activo }),
    });
  },

  // Categorías
  listarCategorias() {
    return apiFetch<CategoriaAdmin[]>('/admin/categorias');
  },
  crearCategoria(nombre: string) {
    return apiFetch<CategoriaAdmin>('/admin/categorias', {
      method: 'POST',
      body: JSON.stringify({ nombre }),
    });
  },
  actualizarCategoria(id: number, nombre: string) {
    return apiFetch<CategoriaAdmin>(`/admin/categorias/${id}`, {
      method: 'PUT',
      body: JSON.stringify({ nombre }),
    });
  },
  cambiarActivoCategoria(id: number, activo: boolean) {
    return apiFetch<CategoriaAdmin>(`/admin/categorias/${id}/activo`, {
      method: 'PUT',
      body: JSON.stringify({ activo }),
    });
  },

  // Tipos de solicitud
  listarTiposSolicitud() {
    return apiFetch<TipoSolicitudAdmin[]>('/admin/tipos-solicitud');
  },
  crearTipoSolicitud(datos: { categoriaId: number; areaId: number; nombre: string; requiereAdjunto?: boolean; slaHoras: number }) {
    return apiFetch<TipoSolicitudAdmin>('/admin/tipos-solicitud', {
      method: 'POST',
      body: JSON.stringify(datos),
    });
  },
  actualizarTipoSolicitud(id: number, datos: { categoriaId: number; areaId: number; nombre: string; requiereAdjunto?: boolean; slaHoras: number }) {
    return apiFetch<TipoSolicitudAdmin>(`/admin/tipos-solicitud/${id}`, {
      method: 'PUT',
      body: JSON.stringify(datos),
    });
  },
  cambiarActivoTipoSolicitud(id: number, activo: boolean) {
    return apiFetch<TipoSolicitudAdmin>(`/admin/tipos-solicitud/${id}/activo`, {
      method: 'PUT',
      body: JSON.stringify({ activo }),
    });
  },

  // Prioridades
  listarPrioridades() {
    return apiFetch<PrioridadAdmin[]>('/admin/prioridades');
  },
  crearPrioridad(datos: { nombre: string; orden: number }) {
    return apiFetch<PrioridadAdmin>('/admin/prioridades', {
      method: 'POST',
      body: JSON.stringify(datos),
    });
  },
  actualizarPrioridad(id: number, datos: { nombre: string; orden: number }) {
    return apiFetch<PrioridadAdmin>(`/admin/prioridades/${id}`, {
      method: 'PUT',
      body: JSON.stringify(datos),
    });
  },

  // Estados
  listarEstados() {
    return apiFetch<EstadoAdmin[]>('/admin/estados');
  },
  crearEstado(datos: { nombre: string; ambito: string; orden: number; esFinal?: boolean }) {
    return apiFetch<EstadoAdmin>('/admin/estados', {
      method: 'POST',
      body: JSON.stringify(datos),
    });
  },
  actualizarEstado(id: number, datos: { nombre: string; ambito: string; orden: number; esFinal?: boolean }) {
    return apiFetch<EstadoAdmin>(`/admin/estados/${id}`, {
      method: 'PUT',
      body: JSON.stringify(datos),
    });
  },

  // Transiciones
  listarTransiciones() {
    return apiFetch<TransicionAdmin[]>('/admin/transiciones');
  },
  crearTransicion(datos: { estadoOrigenId: number; estadoDestinoId: number; rolId?: number | null; ambito: string; requiereMotivo?: boolean }) {
    return apiFetch<TransicionAdmin>('/admin/transiciones', {
      method: 'POST',
      body: JSON.stringify(datos),
    });
  },
  actualizarTransicion(id: number, datos: { estadoOrigenId: number; estadoDestinoId: number; rolId?: number | null; ambito: string; requiereMotivo?: boolean }) {
    return apiFetch<TransicionAdmin>(`/admin/transiciones/${id}`, {
      method: 'PUT',
      body: JSON.stringify(datos),
    });
  },
  eliminarTransicion(id: number) {
    return apiFetch<void>(`/admin/transiciones/${id}`, {
      method: 'DELETE',
    });
  },

  // Ambientes
  listarAmbientes() {
    return apiFetch<AmbienteAdmin[]>('/admin/ambientes');
  },
  crearAmbiente(datos: { nombre: string; tipo: string; pabellon: string; piso?: number | null; capacidad: number }) {
    return apiFetch<AmbienteAdmin>('/admin/ambientes', {
      method: 'POST',
      body: JSON.stringify(datos),
    });
  },
  actualizarAmbiente(id: number, datos: { nombre: string; tipo: string; pabellon: string; piso?: number | null; capacidad: number }) {
    return apiFetch<AmbienteAdmin>(`/admin/ambientes/${id}`, {
      method: 'PUT',
      body: JSON.stringify(datos),
    });
  },
  cambiarActivoAmbiente(id: number, activo: boolean) {
    return apiFetch<AmbienteAdmin>(`/admin/ambientes/${id}/activo`, {
      method: 'PUT',
      body: JSON.stringify({ activo }),
    });
  },
};
