import type { ReactNode } from 'react';
import { Navigate, useLocation } from 'react-router';

import type { Rol } from '../auth/types';
import { useAuth } from '../auth/useAuth';

interface Props {
  roles?: readonly Rol[];
  children: ReactNode;
}

/**
 * Hides pages from users without a session or without the right role.
 * This is only for user experience: the backend enforces the real authorization.
 */
export function RequireAuth({ roles, children }: Props) {
  const { usuario } = useAuth();
  const location = useLocation();

  if (!usuario) {
    return <Navigate to="/login" replace state={{ desde: location.pathname }} />;
  }
  if (roles && !roles.includes(usuario.rol)) {
    return <Navigate to="/sin-permiso" replace />;
  }
  return <>{children}</>;
}
