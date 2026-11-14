import { act, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import { AuthProvider } from '../auth/AuthContext';
import { guardarSesion } from '../auth/sessionStorage';
import type { Notificacion } from '../api/types';
import { AppRoutes } from '../routes/AppRoutes';
import { fakeApi, json } from '../test/fakeApi';

function renderEn(ruta: string) {
  return render(
    <AuthProvider>
      <MemoryRouter initialEntries={[ruta]}>
        <AppRoutes />
      </MemoryRouter>
    </AuthProvider>,
  );
}

beforeEach(() => {
  guardarSesion({
    token: 't',
    usuario: { id: 6, nombre: 'Luis Tec', correo: 'tec@unmsm.edu.pe', rol: 'TECNICO', areaId: 1 },
  });
});

afterEach(() => {
  vi.useRealTimers();
  vi.unstubAllGlobals();
});

describe('NotificacionesPage and AppLayout bell', () => {
  it('HU-17 scenario 2 & 3: filters, marks one, marks all, updates bell counter, and opens solicitud', async () => {
    let items: Notificacion[] = [
      {
        id: 11,
        tipo: 'SOLICITUD_ASIGNADA',
        titulo: 'Solicitud asignada: SOL-2026-0011',
        mensaje: 'Se te asignó la solicitud SOL-2026-0011.',
        solicitudId: 15,
        reservaId: null,
        leida: false,
        fechaCreacion: '2026-10-20T10:00:00',
        fechaLectura: null,
      },
      {
        id: 12,
        tipo: 'COMENTARIO_NUEVO',
        titulo: 'Nuevo comentario en SOL-2026-0012',
        mensaje: 'Ana agregó un comentario.',
        solicitudId: 16,
        reservaId: null,
        leida: false,
        fechaCreacion: '2026-10-20T10:05:00',
        fechaLectura: null,
      },
    ];

    const fetchMock = fakeApi({
      'GET /api/notificaciones/no-leidas/cantidad': () =>
        json(200, { cantidad: items.filter((n) => !n.leida).length }),
      'GET /api/notificaciones': (init) => {
        void init;
        const ultimaUrl = String(fetchMock.mock.calls.at(-1)?.[0] ?? '');
        const leidaMatch = new URL(ultimaUrl, 'http://localhost').searchParams.get('leida');
        const filtradas =
          leidaMatch === null
            ? items
            : items.filter((n) => String(n.leida) === leidaMatch);
        return json(200, {
          content: filtradas,
          page: { size: 20, number: 0, totalElements: filtradas.length, totalPages: 1 },
        });
      },
      'PUT /api/notificaciones/11/leida': () => {
        items = items.map((n) => (n.id === 11 ? { ...n, leida: true } : n));
        return new Response(null, { status: 204 });
      },
      'PUT /api/notificaciones/leer-todas': () => {
        items = items.map((n) => ({ ...n, leida: true }));
        return new Response(null, { status: 204 });
      },
    });

    renderEn('/notificaciones');

    // Bell shows 2 unread
    expect(await screen.findByRole('link', { name: 'Notificaciones (2 sin leer)' })).toBeInTheDocument();
    expect(await screen.findByText('Solicitud asignada: SOL-2026-0011')).toBeInTheDocument();

    // Mark notification 11 as read -> bell updates to 1
    const botonesMarcar = screen.getAllByRole('button', { name: 'Marcar como leída' });
    await userEvent.click(botonesMarcar[0]);

    await waitFor(() => {
      expect(screen.getByRole('link', { name: 'Notificaciones (1 sin leer)' })).toBeInTheDocument();
    });

    // Filter only unread -> only notification 12 remains visible
    await userEvent.selectOptions(screen.getByLabelText('Estado'), 'false');
    await waitFor(() => {
      expect(screen.queryByText('Solicitud asignada: SOL-2026-0011')).not.toBeInTheDocument();
    });
    expect(screen.getByText('Nuevo comentario en SOL-2026-0012')).toBeInTheDocument();

    // Mark all as read -> bell counter goes to 0
    await userEvent.click(screen.getByRole('button', { name: 'Marcar todas como leídas' }));
    await waitFor(() => {
      expect(screen.getByRole('link', { name: 'Notificaciones' })).toBeInTheDocument();
    });
  });

  it('HU-17 scenario 2: clicking Ver solicitud on unread notification marks it read and navigates', async () => {
    let leida = false;
    fakeApi({
      'GET /api/notificaciones/no-leidas/cantidad': () => json(200, { cantidad: leida ? 0 : 1 }),
      'GET /api/notificaciones': () =>
        json(200, {
          content: [
            {
              id: 21,
              tipo: 'SOLICITUD_ASIGNADA',
              titulo: 'Solicitud asignada: SOL-2026-0021',
              mensaje: 'Se te asignó la solicitud.',
              solicitudId: 15,
              reservaId: null,
              leida,
              fechaCreacion: '2026-10-20T10:00:00',
              fechaLectura: null,
            },
          ],
          page: { size: 20, number: 0, totalElements: 1, totalPages: 1 },
        }),
      'PUT /api/notificaciones/21/leida': () => {
        leida = true;
        return new Response(null, { status: 204 });
      },
      'GET /api/solicitudes/15': () =>
        json(200, {
          id: 15,
          codigo: 'SOL-2026-0015',
          categoria: 'Soporte técnico e infraestructura',
          tipo: 'Proyector',
          area: 'Soporte técnico',
          requiereAdjunto: false,
          prioridad: 'Media',
          estado: 'Asignada',
          estadoFinal: false,
          descripcion: 'No enciende',
          ubicacion: 'A-101',
          respuesta: null,
          solicitante: { id: 1, nombre: 'Ana' },
          responsable: { id: 6, nombre: 'Luis Tec' },
          fechaRegistro: '2026-10-17T09:30:00',
          fechaLimite: '2026-10-18T09:30:00',
          fechaCierre: null,
          vencida: false,
        }),
      'GET /api/solicitudes/15/historial': () => json(200, []),
      'GET /api/solicitudes/15/evidencias': () => json(200, []),
      'GET /api/solicitudes/15/comentarios': () => json(200, []),
    });

    renderEn('/notificaciones');

    await userEvent.click(await screen.findByRole('button', { name: 'Ver solicitud' }));

    expect(await screen.findByRole('heading', { name: 'SOL-2026-0015' })).toBeInTheDocument();
    expect(leida).toBe(true);
    expect(screen.getByRole('link', { name: 'Notificaciones' })).toBeInTheDocument();
  });

  it('HU-17 scenario 1: polls unread count every 30 seconds', async () => {
    vi.useFakeTimers();
    let cantidad = 0;
    fakeApi({
      'GET /api/notificaciones/no-leidas/cantidad': () => json(200, { cantidad }),
      'GET /api/notificaciones': () =>
        json(200, { content: [], page: { size: 20, number: 0, totalElements: 0, totalPages: 0 } }),
    });

    renderEn('/notificaciones');

    await act(async () => {
      await Promise.resolve();
    });
    expect(screen.getByRole('link', { name: 'Notificaciones' })).toBeInTheDocument();

    cantidad = 3;
    await act(async () => {
      await vi.advanceTimersByTimeAsync(30_000);
    });
    expect(screen.getByRole('link', { name: 'Notificaciones (3 sin leer)' })).toBeInTheDocument();
  });
});
