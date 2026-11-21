import { render, screen } from '@testing-library/react';
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

const DASHBOARD_DATA = {
  totalRegistradas: 25,
  totalPendientes: 10,
  totalAtendidas: 15,
  totalVencidas: 3,
  porcentajeResueltas: 60.0,
  tiempoPromedioAtencionHoras: 4.5,
  porCategoria: [
    { id: 1, nombre: 'Soporte técnico e infraestructura', cantidad: 18 },
    { id: 2, nombre: 'Rectificaciones', cantidad: 7 },
  ],
  porPrioridad: [
    { id: 1, nombre: 'Alta', cantidad: 8 },
    { id: 2, nombre: 'Media', cantidad: 12 },
    { id: 3, nombre: 'Baja', cantidad: 5 },
  ],
  porResponsable: [
    { id: 10, nombre: 'Juan Técnico', cantidad: 14 },
    { id: null, nombre: 'Sin asignar', cantidad: 11 },
  ],
};

const DASHBOARD_VACIO = {
  totalRegistradas: 0,
  totalPendientes: 0,
  totalAtendidas: 0,
  totalVencidas: 0,
  porcentajeResueltas: 0.0,
  tiempoPromedioAtencionHoras: 0.0,
  porCategoria: [],
  porPrioridad: [],
  porResponsable: [],
};

describe('DashboardPage (HU-11)', () => {
  beforeEach(() => {
    guardarSesion({
      token: 'jwt-token',
      usuario: { id: 3, nombre: 'Supervisor Uno', correo: 'sup@unmsm.edu.pe', rol: 'SUPERVISOR', areaId: null },
    });
  });

  afterEach(() => {
    sessionStorage.clear();
    vi.restoreAllMocks();
  });

  it('HU-11 scenario 1: displays KPIs and breakdown bars when data exists', async () => {
    fakeApi({
      'GET /api/dashboard': () => json(200, DASHBOARD_DATA),
      'GET /api/notificaciones/no-leidas/cantidad': () => json(200, { cantidad: 0 }),
    });

    renderEn('/dashboard');

    expect(await screen.findByRole('heading', { name: 'Dashboard' })).toBeInTheDocument();
    expect(screen.getByTestId('kpi-registradas')).toHaveTextContent('25');
    expect(screen.getByTestId('kpi-pendientes')).toHaveTextContent('10');
    expect(screen.getByTestId('kpi-atendidas')).toHaveTextContent('15');
    expect(screen.getByTestId('kpi-vencidas')).toHaveTextContent('3');
    expect(screen.getByTestId('kpi-porcentaje')).toHaveTextContent('60%');
    expect(screen.getByTestId('kpi-tiempo-promedio')).toHaveTextContent('4.5 h');

    // Desgloses
    expect(screen.getByText('Soporte técnico e infraestructura')).toBeInTheDocument();
    expect(screen.getByText('Juan Técnico')).toBeInTheDocument();
    expect(screen.getByText('Sin asignar')).toBeInTheDocument();
  });

  it('HU-11 scenario 2: displays zeroes when period has no data', async () => {
    fakeApi({
      'GET /api/dashboard': () => json(200, DASHBOARD_VACIO),
      'GET /api/notificaciones/no-leidas/cantidad': () => json(200, { cantidad: 0 }),
    });

    renderEn('/dashboard');

    expect(await screen.findByRole('heading', { name: 'Dashboard' })).toBeInTheDocument();
    expect(screen.getByTestId('kpi-registradas')).toHaveTextContent('0');
    expect(screen.getByTestId('kpi-porcentaje')).toHaveTextContent('0%');
    expect(screen.getAllByText('Sin datos en este período.')).toHaveLength(3);
  });

  it('validates date range when desde is after hasta', async () => {
    fakeApi({
      'GET /api/dashboard': () => json(200, DASHBOARD_DATA),
      'GET /api/notificaciones/no-leidas/cantidad': () => json(200, { cantidad: 0 }),
    });

    renderEn('/dashboard');

    const desdeInput = await screen.findByLabelText('Desde');
    const hastaInput = screen.getByLabelText('Hasta');

    await userEvent.type(desdeInput, '2026-11-20');
    await userEvent.type(hastaInput, '2026-11-10');
    await userEvent.click(screen.getByRole('button', { name: 'Filtrar período' }));

    expect(await screen.findByText(/La fecha 'Desde' no puede ser posterior a 'Hasta'/i)).toBeInTheDocument();
  });
});
