import type { Rol } from '../auth/types';

export interface OpcionMenu {
  ruta: string;
  etiqueta: string;
  roles: readonly Rol[];
}

const TODOS: readonly Rol[] = ['SOLICITANTE', 'TECNICO', 'PERSONAL_AREA', 'SUPERVISOR', 'ADMIN'];

/** Single source for both the sidebar and the role guards of each route. */
export const MENU: readonly OpcionMenu[] = [
  { ruta: '/solicitudes', etiqueta: 'Mis solicitudes', roles: TODOS },
  { ruta: '/solicitudes/nueva', etiqueta: 'Nueva solicitud', roles: TODOS },
  { ruta: '/reservas', etiqueta: 'Mis reservas', roles: TODOS },
  { ruta: '/asignadas', etiqueta: 'Solicitudes asignadas', roles: ['TECNICO'] },
  { ruta: '/bandeja', etiqueta: 'Bandeja de solicitudes', roles: ['SUPERVISOR', 'PERSONAL_AREA', 'ADMIN'] },
  { ruta: '/reservas/aprobacion', etiqueta: 'Aprobación de reservas', roles: ['SUPERVISOR', 'ADMIN'] },
  { ruta: '/dashboard', etiqueta: 'Dashboard', roles: ['SUPERVISOR', 'ADMIN'] },
  { ruta: '/admin', etiqueta: 'Administración', roles: ['ADMIN'] },
];

export function menuPara(rol: Rol): OpcionMenu[] {
  return MENU.filter((opcion) => opcion.roles.includes(rol));
}

export function rolesDe(ruta: string): readonly Rol[] {
  const opcion = MENU.find((o) => o.ruta === ruta);
  if (!opcion) {
    throw new Error(`Route ${ruta} is not declared in MENU`);
  }
  return opcion.roles;
}

/** Landing page after login. */
export function inicioPara(rol: Rol): string {
  switch (rol) {
    case 'TECNICO':
      return '/asignadas';
    case 'PERSONAL_AREA':
      return '/bandeja';
    case 'SUPERVISOR':
    case 'ADMIN':
      return '/dashboard';
    default:
      return '/solicitudes';
  }
}
