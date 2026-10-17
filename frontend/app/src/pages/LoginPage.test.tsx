import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router';
import { afterEach, describe, expect, it, vi } from 'vitest';

import { AuthProvider } from '../auth/AuthContext';
import { LoginPage } from './LoginPage';
import { validarLogin } from './validarLogin';

function renderLogin() {
  return render(
    <AuthProvider>
      <MemoryRouter>
        <LoginPage />
      </MemoryRouter>
    </AuthProvider>,
  );
}

afterEach(() => {
  vi.unstubAllGlobals();
});

describe('validarLogin', () => {
  it('requires both fields', () => {
    expect(validarLogin('', '')).toEqual({
      correo: 'Ingrese su correo.',
      password: 'Ingrese su contraseña.',
    });
  });

  it('rejects a malformed email', () => {
    expect(validarLogin('ana@', 'secreto').correo).toBe('Ingrese un correo válido.');
  });
});

describe('LoginPage', () => {
  it('HU-01 scenario 3: empty fields show validation and send no request', async () => {
    const fetchMock = vi.fn();
    vi.stubGlobal('fetch', fetchMock);
    renderLogin();

    await userEvent.click(screen.getByRole('button', { name: 'Ingresar' }));

    expect(screen.getByText('Ingrese su correo.')).toBeInTheDocument();
    expect(screen.getByText('Ingrese su contraseña.')).toBeInTheDocument();
    expect(fetchMock).not.toHaveBeenCalled();
  });

  it('HU-01 scenario 2: shows the server message for wrong credentials', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response(
      JSON.stringify({ detail: 'Credenciales inválidas', codigo: 'CREDENCIALES_INVALIDAS' }),
      { status: 401, headers: { 'Content-Type': 'application/json' } },
    )));
    renderLogin();

    await userEvent.type(screen.getByLabelText('Correo institucional'), 'ana@unmsm.edu.pe');
    await userEvent.type(screen.getByLabelText('Contraseña'), 'incorrecta');
    await userEvent.click(screen.getByRole('button', { name: 'Ingresar' }));

    expect(await screen.findByRole('alert')).toHaveTextContent('Credenciales inválidas');
  });
});
