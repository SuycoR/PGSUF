import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router';
import { describe, expect, it } from 'vitest';

import { AuthProvider } from '../auth/AuthContext';
import { guardarSesion } from '../auth/sessionStorage';
import type { Rol } from '../auth/types';
import { AppRoutes } from './AppRoutes';

function renderEn(ruta: string, rol?: Rol) {
  if (rol) {
    guardarSesion({
      token: 't',
      usuario: { id: 1, nombre: 'Usuario Prueba', correo: 'u@unmsm.edu.pe', rol, areaId: null },
    });
  }
  return render(
    <AuthProvider>
      <MemoryRouter initialEntries={[ruta]}>
        <AppRoutes />
      </MemoryRouter>
    </AuthProvider>,
  );
}

describe('AppRoutes', () => {
  it('sends a visitor without session to the login page', () => {
    renderEn('/bandeja');
    expect(screen.getByRole('heading', { name: 'Iniciar sesión' })).toBeInTheDocument();
  });

  it('blocks a page that the role cannot access', () => {
    renderEn('/dashboard', 'SOLICITANTE');
    expect(screen.getByRole('heading', { name: 'Sin permiso' })).toBeInTheDocument();
  });

  it('shows only the menu options of the role', () => {
    renderEn('/dashboard', 'SUPERVISOR');
    const menu = screen.getByRole('navigation', { name: 'Menú principal' });
    expect(menu).toHaveTextContent('Dashboard');
    expect(menu).toHaveTextContent('Aprobación de reservas');
    expect(menu).not.toHaveTextContent('Administración');
  });

  it('opens the landing page of each role from the root', () => {
    renderEn('/', 'TECNICO');
    expect(screen.getByRole('heading', { name: 'Solicitudes asignadas' })).toBeInTheDocument();
  });
});
