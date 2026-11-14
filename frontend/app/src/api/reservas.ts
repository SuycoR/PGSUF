import { apiFetch } from './client';
import type {
  Ambiente,
  CrearReservaRequest,
  DisponibilidadResponse,
  Page,
  ReservaResponse,
  ReservaResumen,
} from './types';

export interface FiltrosAmbientes {
  tipo?: string;
  pabellon?: string;
  piso?: number;
}

export interface FiltrosBandejaReservas {
  estadoId?: number;
  ambienteId?: number;
  fecha?: string;
  page?: number;
}

export const reservasApi = {
  listarAmbientes(filtros?: FiltrosAmbientes) {
    const params = new URLSearchParams();
    if (filtros?.tipo) params.set('tipo', filtros.tipo);
    if (filtros?.pabellon) params.set('pabellon', filtros.pabellon);
    if (filtros?.piso !== undefined) params.set('piso', String(filtros.piso));
    const qs = params.toString();
    return apiFetch<Ambiente[]>(`/ambientes${qs ? `?${qs}` : ''}`);
  },

  consultarDisponibilidad(ambienteId: number, fecha: string) {
    return apiFetch<DisponibilidadResponse>(`/ambientes/${ambienteId}/disponibilidad?fecha=${fecha}`);
  },

  crear(datos: CrearReservaRequest) {
    return apiFetch<ReservaResponse>('/reservas', {
      method: 'POST',
      body: JSON.stringify(datos),
    });
  },

  mias(page = 0) {
    const params = new URLSearchParams({ page: String(page), size: '10', sort: 'fecha,desc' });
    return apiFetch<Page<ReservaResumen>>(`/reservas/mis-reservas?${params}`);
  },

  bandeja({ estadoId, ambienteId, fecha, page = 0 }: FiltrosBandejaReservas = {}) {
    const params = new URLSearchParams({ page: String(page), size: '10', sort: 'fecha,desc' });
    if (estadoId) params.set('estadoId', String(estadoId));
    if (ambienteId) params.set('ambienteId', String(ambienteId));
    if (fecha) params.set('fecha', fecha);
    return apiFetch<Page<ReservaResumen>>(`/reservas?${params}`);
  },

  detalle(id: number) {
    return apiFetch<ReservaResponse>(`/reservas/${id}`);
  },

  aprobar(id: number) {
    return apiFetch<ReservaResponse>(`/reservas/${id}/aprobar`, {
      method: 'PUT',
    });
  },

  rechazar(id: number, motivo: string) {
    return apiFetch<ReservaResponse>(`/reservas/${id}/rechazar`, {
      method: 'PUT',
      body: JSON.stringify({ motivo }),
    });
  },
};
