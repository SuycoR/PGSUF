import { apiFetch } from './client';
import type { CantidadNoLeidas, Notificacion, Page } from './types';

/** Dispatched when notifications are marked read so the header bell refreshes immediately. */
export const NOTIFICACIONES_ACTUALIZADAS = 'servicios:notificaciones-actualizadas';

export interface FiltrosNotificaciones {
  leida?: boolean;
  page?: number;
}

export const notificaciones = {
  listar({ leida, page = 0 }: FiltrosNotificaciones = {}) {
    const params = new URLSearchParams({ page: String(page), size: '20', sort: 'fechaCreacion,desc' });
    if (leida !== undefined) params.set('leida', String(leida));
    return apiFetch<Page<Notificacion>>(`/notificaciones?${params}`);
  },
  cantidadNoLeidas: () => apiFetch<CantidadNoLeidas>('/notificaciones/no-leidas/cantidad'),
  async marcarLeida(id: number) {
    await apiFetch<void>(`/notificaciones/${id}/leida`, { method: 'PUT' });
    window.dispatchEvent(new Event(NOTIFICACIONES_ACTUALIZADAS));
  },
  async marcarTodasLeidas() {
    await apiFetch<void>('/notificaciones/leer-todas', { method: 'PUT' });
    window.dispatchEvent(new Event(NOTIFICACIONES_ACTUALIZADAS));
  },
};
