import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import { AuthProvider } from '../../auth/AuthContext';
import { guardarSesion } from '../../auth/sessionStorage';
import { AppRoutes } from '../../routes/AppRoutes';
import { fakeApi, json } from '../../test/fakeApi';

function renderEn(ruta: string) {
  return render(
    <AuthProvider>
      <MemoryRouter initialEntries={[ruta]}>
        <AppRoutes />
      </MemoryRouter>
    </AuthProvider>,
  );
}

const USUARIOS = {
  content: [
    { id: 1, nombre: 'Admin General', correo: 'admin@unmsm.edu.pe', rol: 'ADMIN', areaId: null, activo: true },
    { id: 2, nombre: 'Juan Técnico', correo: 'juan@unmsm.edu.pe', rol: 'TECNICO', areaId: 1, activo: true },
  ],
  page: { size: 20, number: 0, totalElements: 2, totalPages: 1 },
};

const AREAS = [
  { id: 1, nombre: 'Soporte técnico', activo: true },
  { id: 2, nombre: 'Secretaría académica', activo: true },
];

const CATEGORIAS = [
  { id: 1, nombre: 'Soporte técnico e infraestructura', activo: true, esTramite: false },
];

const ESTADOS = [
  { id: 1, nombre: 'Registrada', ambito: 'SOLICITUD', orden: 1, esFinal: false },
];

describe('AdminPage (HU-12)', () => {
  beforeEach(() => {
    guardarSesion({
      token: 'jwt-admin',
      usuario: { id: 1, nombre: 'Admin General', correo: 'admin@unmsm.edu.pe', rol: 'ADMIN', areaId: null },
    });
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('muestra la página de administración y lista de usuarios por defecto', async () => {
    fakeApi({
      'GET /api/admin/usuarios': () => json(200, USUARIOS),
      'GET /api/admin/areas': () => json(200, AREAS),
      'GET /api/admin/categorias': () => json(200, CATEGORIAS),
      'GET /api/admin/estados': () => json(200, ESTADOS),
      'GET /api/notificaciones/no-leidas/cantidad': () => json(200, { cantidad: 0 }),
    });

    renderEn('/admin');

    expect(await screen.findByRole('heading', { name: 'Administración del Sistema' })).toBeInTheDocument();
    expect(await screen.findByText('Juan Técnico')).toBeInTheDocument();
    expect(screen.getByText('admin@unmsm.edu.pe')).toBeInTheDocument();
  });

  it('permite cambiar a la pestaña de Áreas y listar sus registros', async () => {
    const user = userEvent.setup();
    fakeApi({
      'GET /api/admin/usuarios': () => json(200, USUARIOS),
      'GET /api/admin/areas': () => json(200, AREAS),
      'GET /api/admin/categorias': () => json(200, CATEGORIAS),
      'GET /api/admin/estados': () => json(200, ESTADOS),
      'GET /api/notificaciones/no-leidas/cantidad': () => json(200, { cantidad: 0 }),
    });

    renderEn('/admin');

    const botonAreas = await screen.findByRole('button', { name: /Áreas/i });
    await user.click(botonAreas);

    expect(await screen.findByText('Secretaría académica')).toBeInTheDocument();
  });

  it('abre el modal de nuevo registro al hacer click en "+ Nuevo registro"', async () => {
    const user = userEvent.setup();
    fakeApi({
      'GET /api/admin/usuarios': () => json(200, USUARIOS),
      'GET /api/admin/areas': () => json(200, AREAS),
      'GET /api/admin/categorias': () => json(200, CATEGORIAS),
      'GET /api/admin/estados': () => json(200, ESTADOS),
      'GET /api/notificaciones/no-leidas/cantidad': () => json(200, { cantidad: 0 }),
    });

    renderEn('/admin');

    const botonNuevo = await screen.findByRole('button', { name: /\+ Nuevo registro/i });
    await user.click(botonNuevo);

    expect(await screen.findByLabelText(/Nombre completo/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/Correo institucional/i)).toBeInTheDocument();
  });

  it('permite alternar el estado activo/desactivar de un registro', async () => {
    const user = userEvent.setup();
    let cambiado = false;
    fakeApi({
      'GET /api/admin/usuarios': () =>
        json(200, {
          ...USUARIOS,
          content: [
            { ...USUARIOS.content[0], activo: !cambiado },
            USUARIOS.content[1],
          ],
        }),
      'PUT /api/admin/usuarios/1/activo': () => {
        cambiado = true;
        return json(200, { ...USUARIOS.content[0], activo: false });
      },
      'GET /api/admin/areas': () => json(200, AREAS),
      'GET /api/admin/categorias': () => json(200, CATEGORIAS),
      'GET /api/admin/estados': () => json(200, ESTADOS),
      'GET /api/notificaciones/no-leidas/cantidad': () => json(200, { cantidad: 0 }),
    });

    renderEn('/admin');

    const botonesDesactivar = await screen.findAllByRole('button', { name: 'Desactivar' });
    await user.click(botonesDesactivar[0]);

    await waitFor(() => {
      expect(screen.getByText(/desactivado con éxito/i)).toBeInTheDocument();
    });
  });
});
