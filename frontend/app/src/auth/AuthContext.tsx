import { useCallback, useEffect, useMemo, useState, type ReactNode } from 'react';

import { SESION_EXPIRADA } from '../api/client';
import { AuthContext, type AuthContextValue } from './authContextValue';
import { borrarSesion, guardarSesion, leerSesion } from './sessionStorage';
import type { Sesion } from './types';

export function AuthProvider({ children }: { children: ReactNode }) {
  const [sesion, setSesion] = useState<Sesion | null>(() => leerSesion());

  const iniciarSesion = useCallback((nueva: Sesion) => {
    guardarSesion(nueva);
    setSesion(nueva);
  }, []);

  const cerrarSesion = useCallback(() => {
    borrarSesion();
    setSesion(null);
  }, []);

  useEffect(() => {
    window.addEventListener(SESION_EXPIRADA, cerrarSesion);
    return () => window.removeEventListener(SESION_EXPIRADA, cerrarSesion);
  }, [cerrarSesion]);

  const value = useMemo<AuthContextValue>(
    () => ({ usuario: sesion?.usuario ?? null, iniciarSesion, cerrarSesion }),
    [sesion, iniciarSesion, cerrarSesion],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}
