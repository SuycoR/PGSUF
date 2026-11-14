import { apiFetch } from './client';
import type { Comentario } from './types';

export const comentarios = {
  listar: (solicitudId: number) => apiFetch<Comentario[]>(`/solicitudes/${solicitudId}/comentarios`),
  crear: (solicitudId: number, texto: string) =>
    apiFetch<Comentario>(`/solicitudes/${solicitudId}/comentarios`, {
      method: 'POST',
      body: JSON.stringify({ texto }),
    }),
};
