import { apiDescargar, apiFetch } from './client';
import type {
  Evidencia,
  Historial,
  Page,
  SolicitudCreada,
  SolicitudDetalle,
  SolicitudResumen,
  Tecnico,
  TransicionDisponible,
} from './types';

export interface NuevaSolicitud {
  tipoSolicitudId: number;
  prioridadId: number;
  descripcion: string;
  ubicacion: string;
}

export interface FiltrosSolicitudes {
  estadoId?: number;
  categoriaId?: number;
  page?: number;
}

export interface FiltrosBandeja extends FiltrosSolicitudes {
  prioridadId?: number;
  vencidas?: boolean;
}

export interface FiltrosAsignadas {
  estadoId?: number;
  page?: number;
}

export const solicitudes = {
  /** Multipart request: JSON part "datos" plus up to 5 files in "archivos". */
  registrar(datos: NuevaSolicitud, archivos: File[]) {
    const form = new FormData();
    form.append('datos', new Blob([JSON.stringify(datos)], { type: 'application/json' }));
    archivos.forEach((archivo) => form.append('archivos', archivo));
    return apiFetch<SolicitudCreada>('/solicitudes', { method: 'POST', body: form });
  },

  mias({ estadoId, categoriaId, page = 0 }: FiltrosSolicitudes) {
    const params = new URLSearchParams({ page: String(page), size: '10', sort: 'fechaRegistro,desc' });
    if (estadoId) params.set('estadoId', String(estadoId));
    if (categoriaId) params.set('categoriaId', String(categoriaId));
    return apiFetch<Page<SolicitudResumen>>(`/solicitudes/mis-solicitudes?${params}`);
  },

  bandeja({ estadoId, categoriaId, prioridadId, vencidas, page = 0 }: FiltrosBandeja) {
    const params = new URLSearchParams({ page: String(page), size: '20', sort: 'fechaRegistro,desc' });
    if (estadoId) params.set('estadoId', String(estadoId));
    if (categoriaId) params.set('categoriaId', String(categoriaId));
    if (prioridadId) params.set('prioridadId', String(prioridadId));
    if (vencidas !== undefined) params.set('vencidas', String(vencidas));
    return apiFetch<Page<SolicitudResumen>>(`/solicitudes?${params}`);
  },

  asignadas({ estadoId, page = 0 }: FiltrosAsignadas) {
    const params = new URLSearchParams({ page: String(page), size: '10', sort: 'fechaRegistro,desc' });
    if (estadoId) params.set('estadoId', String(estadoId));
    return apiFetch<Page<SolicitudResumen>>(`/solicitudes/asignadas?${params}`);
  },

  detalle: (id: number) => apiFetch<SolicitudDetalle>(`/solicitudes/${id}`),
  historial: (id: number) => apiFetch<Historial[]>(`/solicitudes/${id}/historial`),
  evidencias: (id: number) => apiFetch<Evidencia[]>(`/solicitudes/${id}/evidencias`),
  transiciones: (id: number) => apiFetch<TransicionDisponible[]>(`/solicitudes/${id}/transiciones`),
  asignar: (id: number, responsableId: number) =>
    apiFetch<SolicitudDetalle>(`/solicitudes/${id}/asignar`, {
      method: 'PUT',
      body: JSON.stringify({ responsableId }),
    }),
  cambiarEstado: (id: number, estadoId: number, comentario?: string) =>
    apiFetch<SolicitudDetalle>(`/solicitudes/${id}/estado`, {
      method: 'PUT',
      body: JSON.stringify({ estadoId, ...(comentario?.trim() ? { comentario: comentario.trim() } : {}) }),
    }),
  cambiarPrioridad: (id: number, prioridadId: number) =>
    apiFetch<SolicitudDetalle>(`/solicitudes/${id}/prioridad`, {
      method: 'PUT',
      body: JSON.stringify({ prioridadId }),
    }),
  agregarEvidencias(id: number, archivos: File[]) {
    const form = new FormData();
    archivos.forEach((archivo) => form.append('archivos', archivo));
    return apiFetch<Evidencia[]>(`/solicitudes/${id}/evidencias`, { method: 'POST', body: form });
  },
  tecnicos: (areaId?: number) =>
    apiFetch<Tecnico[]>(areaId ? `/usuarios/tecnicos?areaId=${areaId}` : '/usuarios/tecnicos'),
  descargarEvidencia: (id: number) => apiDescargar(`/evidencias/${id}/descarga`),
};
