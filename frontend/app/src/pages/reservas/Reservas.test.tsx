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

const AMBIENTES = [
  { id: 1, nombre: 'Auditorio', tipo: 'AUDITORIO', pabellon: 'CENTRAL', piso: null, capacidad: 300, activo: true },
  { id: 2, nombre: 'Laboratorio A-301', tipo: 'LABORATORIO', pabellon: 'ANTIGUO', piso: 3, capacidad: 30, activo: true },
];

const MIS_RESERVAS = {
  content: [
    {
      id: 10,
      codigo: 'RES-2026-0001',
      ambienteId: 1,
      ambienteNombre: 'Auditorio',
      usuarioId: 1,
      usuarioNombre: 'Ana',
      estadoId: 1,
      estadoNombre: 'Registrada',
      fecha: '2026-10-24',
      horaInicio: '10:00:00',
      horaFin: '12:00:00',
      motivo: 'Conferencia de IA',
      motivoRechazo: null,
      fechaRegistro: '2026-10-20T09:00:00',
      fechaResolucion: null,
    },
  ],
  page: { size: 10, number: 0, totalElements: 1, totalPages: 1 },
};

describe('MisReservasPage (HU-13)', () => {
  beforeEach(() => {
    guardarSesion({
      token: 'jwt-token',
      usuario: { id: 1, nombre: 'Ana', correo: 'ana@unmsm.edu.pe', rol: 'SOLICITANTE', areaId: null },
    });
  });

  afterEach(() => {
    sessionStorage.clear();
    vi.restoreAllMocks();
  });

  it('HU-13 scenario 1: lists current reservations and allows switching to new reservation form', async () => {
    fakeApi({
      'GET /api/reservas/mis-reservas': () => json(200, MIS_RESERVAS),
      'GET /api/ambientes': () => json(200, AMBIENTES),
      'GET /api/ambientes/1/disponibilidad': () =>
        json(200, {
          ambienteId: 1,
          ambienteNombre: 'Auditorio',
          fecha: '2026-10-24',
          reservasAprobadas: [],
        }),
      'GET /api/notificaciones/no-leidas/cantidad': () => json(200, { cantidad: 0 }),
    });

    renderEn('/reservas');

    expect(await screen.findByText('RES-2026-0001')).toBeInTheDocument();
    expect(screen.getByText('Conferencia de IA')).toBeInTheDocument();

    // Cambiar a pestaña Nueva Reserva
    await userEvent.click(screen.getByRole('tab', { name: 'Nueva Reserva' }));
    expect(await screen.findByRole('heading', { name: /Solicitar Reserva de Ambiente/i })).toBeInTheDocument();
  });

  it('HU-13 scenario 2: validates client hours (horaFin > horaInicio) before submitting', async () => {
    fakeApi({
      'GET /api/reservas/mis-reservas': () => json(200, MIS_RESERVAS),
      'GET /api/ambientes': () => json(200, AMBIENTES),
      'GET /api/ambientes/1/disponibilidad': () =>
        json(200, { ambienteId: 1, ambienteNombre: 'Auditorio', fecha: '2026-10-24', reservasAprobadas: [] }),
      'GET /api/notificaciones/no-leidas/cantidad': () => json(200, { cantidad: 0 }),
    });

    renderEn('/reservas');

    await userEvent.click(await screen.findByRole('tab', { name: 'Nueva Reserva' }));
    expect(await screen.findByLabelText(/Ambiente a reservar/i)).toBeInTheDocument();

    // Rellenar horas inválidas (fin <= inicio)
    const inicio = screen.getByLabelText(/Hora de inicio/i);
    const fin = screen.getByLabelText(/Hora de fin/i);
    const motivo = screen.getByLabelText(/Motivo de la actividad/i);

    await userEvent.clear(inicio);
    await userEvent.type(inicio, '14:00');
    await userEvent.clear(fin);
    await userEvent.type(fin, '12:00');
    await userEvent.type(motivo, 'Taller de programación');

    await userEvent.click(screen.getByRole('button', { name: 'Confirmar Reserva' }));

    expect(await screen.findByText(/La hora de fin debe ser posterior a la hora de inicio/i)).toBeInTheDocument();
  });
});

describe('AprobacionReservasPage (HU-14)', () => {
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

  it('HU-14 scenario 1: supervisor approves pending reservation', async () => {
    let aprobadaLlamada = false;
    vi.spyOn(window, 'confirm').mockReturnValue(true);

    fakeApi({
      'GET /api/reservas': () => json(200, MIS_RESERVAS),
      'PUT /api/reservas/10/aprobar': () => {
        aprobadaLlamada = true;
        return json(200, { ...MIS_RESERVAS.content[0], estadoNombre: 'Aprobada' });
      },
      'GET /api/notificaciones/no-leidas/cantidad': () => json(200, { cantidad: 0 }),
    });

    renderEn('/reservas/aprobacion');

    expect(await screen.findByText('RES-2026-0001')).toBeInTheDocument();
    const btnAprobar = screen.getByRole('button', { name: 'Aprobar' });
    await userEvent.click(btnAprobar);

    await waitFor(() => {
      expect(aprobadaLlamada).toBe(true);
    });
  });

  it('HU-14 scenario 2 & 3: supervisor rejects with mandatory reason (RN-13)', async () => {
    let rechazoLlamado = false;

    fakeApi({
      'GET /api/reservas': () => json(200, MIS_RESERVAS),
      'PUT /api/reservas/10/rechazar': () => {
        rechazoLlamado = true;
        return json(200, { ...MIS_RESERVAS.content[0], estadoNombre: 'Rechazada' });
      },
      'GET /api/notificaciones/no-leidas/cantidad': () => json(200, { cantidad: 0 }),
    });

    renderEn('/reservas/aprobacion');

    expect(await screen.findByText('RES-2026-0001')).toBeInTheDocument();
    await userEvent.click(screen.getByRole('button', { name: 'Rechazar' }));

    // Modal aparece
    expect(await screen.findByRole('heading', { name: /Rechazar Reserva RES-2026-0001/i })).toBeInTheDocument();

    // Intentar confirmar sin motivo -> bloqueado
    const btnConfirmar = screen.getByRole('button', { name: 'Confirmar Rechazo' });
    await userEvent.click(btnConfirmar);
    expect(await screen.findByText(/El motivo de rechazo es obligatorio/i)).toBeInTheDocument();
    expect(rechazoLlamado).toBe(false);

    // Escribir motivo y confirmar
    const motivoInput = screen.getByLabelText(/Motivo de rechazo \*/i);
    await userEvent.type(motivoInput, 'Mantenimiento preventivo programado');
    await userEvent.click(btnConfirmar);

    await waitFor(() => {
      expect(rechazoLlamado).toBe(true);
    });
  });
});
