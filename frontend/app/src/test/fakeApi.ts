import { vi } from 'vitest';

type Handler = (init: RequestInit | undefined) => Response | Promise<Response>;

export function json(status: number, body: unknown): Response {
  return new Response(JSON.stringify(body), { status, headers: { 'Content-Type': 'application/json' } });
}

/**
 * Minimal fake backend: maps "METHOD /path" (query string ignored) to a handler.
 * Unknown routes answer 404 so a missing stub fails loudly in the test.
 */
export function fakeApi(rutas: Record<string, Handler>) {
  const fetchMock = vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
    const url = new URL(String(input), 'http://localhost');
    const clave = `${init?.method ?? 'GET'} ${url.pathname}`;
    const handler = rutas[clave];
    return handler ? handler(init) : json(404, { detail: `No stub for ${clave}`, codigo: 'NO_ENCONTRADO' });
  });
  vi.stubGlobal('fetch', fetchMock);
  return fetchMock;
}

export const CATALOGOS = {
  'GET /api/catalogos/categorias': () =>
    json(200, [
      { id: 1, nombre: 'Soporte técnico e infraestructura', esTramite: false },
      { id: 2, nombre: 'Rectificaciones', esTramite: true },
    ]),
  'GET /api/catalogos/prioridades': () =>
    json(200, [
      { id: 1, nombre: 'Alta', orden: 1 },
      { id: 2, nombre: 'Media', orden: 2 },
    ]),
  'GET /api/catalogos/estados': () =>
    json(200, [
      { id: 1, nombre: 'Registrada', ambito: 'AMBOS', orden: 1, esFinal: false },
      { id: 2, nombre: 'En evaluación', ambito: 'SOLICITUD', orden: 2, esFinal: false },
    ]),
  'GET /api/catalogos/tipos-solicitud': () =>
    json(200, [
      { id: 10, nombre: 'Proyector', categoriaId: 1, area: 'Soporte técnico', requiereAdjunto: false, slaHoras: 24 },
      { id: 20, nombre: 'Rectificación de nota', categoriaId: 2, area: 'Secretaría académica', requiereAdjunto: true, slaHoras: 120 },
    ]),
};
