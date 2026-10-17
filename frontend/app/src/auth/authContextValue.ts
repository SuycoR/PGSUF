import { createContext } from 'react';

import type { Sesion, Usuario } from './types';

export interface AuthContextValue {
  usuario: Usuario | null;
  iniciarSesion: (sesion: Sesion) => void;
  cerrarSesion: () => void;
}

export const AuthContext = createContext<AuthContextValue | null>(null);
