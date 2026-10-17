export const ROLES = ['SOLICITANTE', 'TECNICO', 'PERSONAL_AREA', 'SUPERVISOR', 'ADMIN'] as const;

export type Rol = (typeof ROLES)[number];

export interface Usuario {
  id: number;
  nombre: string;
  correo: string;
  rol: Rol;
  areaId: number | null;
}

export interface Sesion {
  token: string;
  usuario: Usuario;
}
