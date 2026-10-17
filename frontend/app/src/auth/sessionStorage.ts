import type { Sesion } from './types';

// sessionStorage is cleared when the tab closes (decision in docs/04-arquitectura.md).
const KEY = 'servicios.sesion';

export function leerSesion(): Sesion | null {
  try {
    const raw = sessionStorage.getItem(KEY);
    return raw ? (JSON.parse(raw) as Sesion) : null;
  } catch {
    return null;
  }
}

export function guardarSesion(sesion: Sesion): void {
  sessionStorage.setItem(KEY, JSON.stringify(sesion));
}

export function borrarSesion(): void {
  sessionStorage.removeItem(KEY);
}

export function leerToken(): string | null {
  return leerSesion()?.token ?? null;
}
