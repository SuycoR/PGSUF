import { apiFetch } from './client';
import type { DashboardResponse } from './types';

export const dashboardApi = {
  obtener(desde?: string, hasta?: string) {
    const params = new URLSearchParams();
    if (desde) params.set('desde', desde);
    if (hasta) params.set('hasta', hasta);
    const qs = params.toString();
    return apiFetch<DashboardResponse>(`/dashboard${qs ? `?${qs}` : ''}`);
  },
};
