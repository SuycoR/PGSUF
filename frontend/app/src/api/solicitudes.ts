import { apiDescargar, apiFetch } from './client';
import type { Evidencia, Historial, Page, SolicitudCreada, SolicitudDetalle, SolicitudResumen } from './types';

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

  detalle: (id: number) => apiFetch<SolicitudDetalle>(`/solicitudes/${id}`),
  historial: (id: number) => apiFetch<Historial[]>(`/solicitudes/${id}/historial`),
  evidencias: (id: number) => apiFetch<Evidencia[]>(`/solicitudes/${id}/evidencias`),
  descargarEvidencia: (id: number) => apiDescargar(`/evidencias/${id}/descarga`),
};
