import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router';
import { afterEach, describe, expect, it, vi } from 'vitest';

import { AuthProvider } from '../auth/AuthContext';
import { AppRoutes } from '../routes/AppRoutes';
import { validarRegistro } from './validarRegistro';

function json(status: number, body: unknown): Response {
  return new Response(JSON.stringify(body), { status, headers: { 'Content-Type': 'application/json' } });
}

function renderRegistro() {
  return render(
    <AuthProvider>
      <MemoryRouter initialEntries={['/registro']}>
        <AppRoutes />
      </MemoryRouter>
    </AuthProvider>,
  );
}

async function completar(nombre: string, correo: string, password: string) {
  await userEvent.type(screen.getByLabelText('Nombre completo'), nombre);
  await userEvent.type(screen.getByLabelText('Correo institucional'), correo);
  await userEvent.type(screen.getByLabelText('Contraseña'), password);
  await userEvent.click(screen.getByRole('button', { name: 'Crear cuenta' }));
}

afterEach(() => {
  vi.unstubAllGlobals();
});

describe('validarRegistro', () => {
  it('accepts valid data', () => {
    expect(validarRegistro('Ana', 'ana@unmsm.edu.pe', 'Estudiante1')).toEqual({});
  });

  it('HU-02 scenario 3: rejects a short password', () => {
    expect(validarRegistro('Ana', 'ana@unmsm.edu.pe', 'corta').password).toBe(
      'La contraseña debe tener entre 8 y 72 caracteres.',
    );
  });
});

describe('RegistroPage', () => {
  it('HU-02 scenario 1: creates the account and returns to login with a confirmation', async () => {
    const fetchMock = vi.fn().mockResolvedValue(json(201, { id: 5, rol: 'SOLICITANTE' }));
    vi.stubGlobal('fetch', fetchMock);
    renderRegistro();

    await completar('Ana Estudiante', 'ana@unmsm.edu.pe', 'Estudiante1');

    expect(await screen.findByRole('status')).toHaveTextContent('Cuenta creada');
    expect(screen.getByRole('heading', { name: 'Iniciar sesión' })).toBeInTheDocument();
    const [url, init] = fetchMock.mock.calls[0];
    expect(url).toBe('/api/auth/registro');
    expect(JSON.parse(init.body)).toEqual({ nombre: 'Ana Estudiante', correo: 'ana@unmsm.edu.pe', password: 'Estudiante1' });
  });

  it('HU-02 scenario 2: shows the duplicate email message from the server', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(
      json(409, { detail: 'El correo ya está registrado.', codigo: 'CORREO_DUPLICADO' }),
    ));
    renderRegistro();

    await completar('Ana', 'ana@unmsm.edu.pe', 'Estudiante1');

    expect(await screen.findByRole('alert')).toHaveTextContent('El correo ya está registrado.');
  });

  it('does not call the API while the form is invalid', async () => {
    const fetchMock = vi.fn();
    vi.stubGlobal('fetch', fetchMock);
    renderRegistro();

    await userEvent.click(screen.getByRole('button', { name: 'Crear cuenta' }));

    expect(screen.getByText('Ingrese su nombre.')).toBeInTheDocument();
    expect(fetchMock).not.toHaveBeenCalled();
  });
});
