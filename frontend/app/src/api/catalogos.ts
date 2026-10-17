import { apiFetch } from './client';
import type { Categoria, Estado, Prioridad, TipoSolicitud } from './types';

export const catalogos = {
  categorias: () => apiFetch<Categoria[]>('/catalogos/categorias'),
  tiposSolicitud: (categoriaId: number) =>
    apiFetch<TipoSolicitud[]>(`/catalogos/tipos-solicitud?categoriaId=${categoriaId}`),
  prioridades: () => apiFetch<Prioridad[]>('/catalogos/prioridades'),
  estadosSolicitud: () => apiFetch<Estado[]>('/catalogos/estados?ambito=SOLICITUD'),
};
