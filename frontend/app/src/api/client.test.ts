import { afterEach, describe, expect, it, vi } from 'vitest';

import { guardarSesion } from '../auth/sessionStorage';
import { ApiError, SESION_EXPIRADA, apiFetch } from './client';

function respuesta(status: number, body?: unknown): Response {
  return new Response(body === undefined ? null : JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/json' },
  });
}

afterEach(() => {
  vi.unstubAllGlobals();
});

describe('apiFetch', () => {
  it('sends the JWT and returns the parsed body', async () => {
    guardarSesion({
      token: 'abc',
      usuario: { id: 1, nombre: 'Ana', correo: 'ana@unmsm.edu.pe', rol: 'SOLICITANTE', areaId: null },
    });
    const fetchMock = vi.fn().mockResolvedValue(respuesta(200, { id: 15 }));
    vi.stubGlobal('fetch', fetchMock);

    const data = await apiFetch<{ id: number }>('/solicitudes/15');

    expect(data).toEqual({ id: 15 });
    const [url, init] = fetchMock.mock.calls[0];
    expect(url).toBe('/api/solicitudes/15');
    expect(new Headers(init.headers).get('Authorization')).toBe('Bearer abc');
  });

  it('turns a ProblemDetail into an ApiError with code and field errors', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(respuesta(400, {
      title: 'Datos inválidos',
      detail: 'Uno o más campos son inválidos.',
      codigo: 'DATOS_INVALIDOS',
      errores: [{ campo: 'descripcion', mensaje: 'es obligatorio' }],
    })));

    const error = await apiFetch('/solicitudes', { method: 'POST', body: '{}' }).catch((e: unknown) => e);

    expect(error).toBeInstanceOf(ApiError);
    expect(error).toMatchObject({
      status: 400,
      codigo: 'DATOS_INVALIDOS',
      message: 'Uno o más campos son inválidos.',
      errores: [{ campo: 'descripcion', mensaje: 'es obligatorio' }],
    });
  });

  it('announces an expired session when an authenticated call gets 401', async () => {
    guardarSesion({
      token: 'vencido',
      usuario: { id: 1, nombre: 'Ana', correo: 'ana@unmsm.edu.pe', rol: 'SOLICITANTE', areaId: null },
    });
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(respuesta(401, { codigo: 'NO_AUTENTICADO' })));
    const listener = vi.fn();
    window.addEventListener(SESION_EXPIRADA, listener);

    await expect(apiFetch('/solicitudes')).rejects.toBeInstanceOf(ApiError);

    expect(listener).toHaveBeenCalledOnce();
    window.removeEventListener(SESION_EXPIRADA, listener);
  });

  it('reports a connection error when the server is unreachable', async () => {
    vi.stubGlobal('fetch', vi.fn().mockRejectedValue(new TypeError('Failed to fetch')));

    await expect(apiFetch('/solicitudes')).rejects.toMatchObject({ status: 0, codigo: 'SIN_CONEXION' });
  });
});
