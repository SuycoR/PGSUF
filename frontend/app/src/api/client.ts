import { leerToken } from '../auth/sessionStorage';

/** Event fired when the API answers 401: the session context listens to it and logs out. */
export const SESION_EXPIRADA = 'servicios:sesion-expirada';

export interface CampoError {
  campo: string;
  mensaje: string;
}

/** ProblemDetail (RFC 7807) as returned by the backend (see docs/07-api.md). */
interface ProblemDetail {
  title?: string;
  detail?: string;
  status?: number;
  codigo?: string;
  errores?: CampoError[];
}

export class ApiError extends Error {
  readonly status: number;
  readonly codigo: string;
  readonly errores: CampoError[];

  constructor(status: number, codigo: string, mensaje: string, errores: CampoError[] = []) {
    super(mensaje);
    this.name = 'ApiError';
    this.status = status;
    this.codigo = codigo;
    this.errores = errores;
  }
}

const BASE_URL = '/api';

export async function apiFetch<T>(path: string, init: RequestInit = {}): Promise<T> {
  const response = await enviar(path, init);
  if (response.status === 204) {
    return undefined as T;
  }
  return (await response.json()) as T;
}

/** Downloads a protected file: a plain link cannot send the Authorization header. */
export async function apiDescargar(path: string): Promise<Blob> {
  const response = await enviar(path, {});
  return response.blob();
}

async function enviar(path: string, init: RequestInit): Promise<Response> {
  const headers = new Headers(init.headers);
  const token = leerToken();
  if (token) {
    headers.set('Authorization', `Bearer ${token}`);
  }
  // FormData sets its own multipart boundary; only JSON bodies need the header.
  if (init.body && !(init.body instanceof FormData) && !headers.has('Content-Type')) {
    headers.set('Content-Type', 'application/json');
  }

  let response: Response;
  try {
    response = await fetch(`${BASE_URL}${path}`, { ...init, headers });
  } catch {
    throw new ApiError(0, 'SIN_CONEXION', 'No se pudo conectar con el servidor. Intente nuevamente.');
  }

  if (response.status === 401 && token) {
    window.dispatchEvent(new Event(SESION_EXPIRADA));
  }

  if (!response.ok) {
    throw await toApiError(response);
  }
  return response;
}

async function toApiError(response: Response): Promise<ApiError> {
  try {
    const problem = (await response.json()) as ProblemDetail;
    return new ApiError(
      response.status,
      problem.codigo ?? 'ERROR',
      problem.detail ?? problem.title ?? 'Ocurrió un error.',
      problem.errores ?? [],
    );
  } catch {
    return new ApiError(response.status, 'ERROR', 'Ocurrió un error inesperado.');
  }
}
