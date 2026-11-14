import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import { AuthProvider } from '../../auth/AuthContext';
import type { Historial, SolicitudDetalle, TransicionDisponible } from '../../api/types';
import { guardarSesion } from '../../auth/sessionStorage';
import { AppRoutes } from '../../routes/AppRoutes';
import { CATALOGOS, fakeApi, json } from '../../test/fakeApi';
import { validarSolicitud, type FormularioSolicitud } from './validarSolicitud';

function renderEn(ruta: string) {
  return render(
    <AuthProvider>
      <MemoryRouter initialEntries={[ruta]}>
        <AppRoutes />
      </MemoryRouter>
    </AuthProvider>,
  );
}

const DETALLE = {
  id: 15,
  codigo: 'SOL-2026-0015',
  categoria: 'Rectificaciones',
  tipo: 'Rectificación de nota',
  area: 'Secretaría académica',
  requiereAdjunto: true,
  prioridad: 'Media',
  estado: 'En evaluación',
  estadoFinal: false,
  descripcion: 'Mi nota del parcial no coincide',
  ubicacion: 'Facultad de Sistemas',
  respuesta: null,
  solicitante: { id: 1, nombre: 'Ana' },
  responsable: null,
  fechaRegistro: '2026-10-17T09:30:00',
  fechaLimite: '2026-10-22T09:30:00',
  fechaCierre: null,
  vencida: false,
};

const HISTORIAL = [
  { id: 1, estadoAnterior: null, estadoNuevo: 'Registrada', usuario: 'Ana', observacion: null, fecha: '2026-10-17T09:30:00' },
  { id: 2, estadoAnterior: 'Registrada', estadoNuevo: 'En evaluación', usuario: 'Sistema', observacion: 'Derivada automáticamente a Secretaría académica', fecha: '2026-10-17T09:30:00' },
];

const DETALLE_RUTAS = {
  'GET /api/solicitudes/15': () => json(200, DETALLE),
  'GET /api/solicitudes/15/historial': () => json(200, HISTORIAL),
  'GET /api/solicitudes/15/evidencias': () =>
    json(200, [{ id: 7, nombreArchivo: 'sustento.pdf', tipoMime: 'application/pdf', tamanoBytes: 2048, fechaSubida: '2026-10-17T09:30:00', subidoPor: 'Ana' }]),
};

beforeEach(() => {
  guardarSesion({ token: 't', usuario: { id: 1, nombre: 'Ana', correo: 'ana@unmsm.edu.pe', rol: 'SOLICITANTE', areaId: null } });
});

afterEach(() => {
  vi.unstubAllGlobals();
});

describe('validarSolicitud', () => {
  const base: FormularioSolicitud = {
    categoriaId: '1', tipoSolicitudId: '10', prioridadId: '2', ubicacion: 'Salón A-101', descripcion: 'No enciende', archivos: [],
  };

  it('accepts a technical request without files', () => {
    expect(validarSolicitud(base, false)).toEqual({});
  });

  it('HU-15 scenario 2: a procedure needs its supporting document', () => {
    expect(validarSolicitud(base, true).archivos).toBe('Debe adjuntar el documento de sustento.');
  });

  it('rejects other formats and more than five files', () => {
    const exe = new File(['MZ'], 'programa.exe');
    const png = () => new File(['x'], 'foto.png', { type: 'image/png' });
    expect(validarSolicitud({ ...base, archivos: [exe] }, false).archivos).toBe('"programa.exe" no es PDF, JPG o PNG.');
    expect(validarSolicitud({ ...base, archivos: [png(), png(), png(), png(), png(), png()] }, false).archivos).toBe(
      'Puede adjuntar hasta 5 archivos.',
    );
  });
});

describe('NuevaSolicitudPage', () => {
  async function completarTramite() {
    await userEvent.selectOptions(await screen.findByLabelText('Categoría'), 'Rectificaciones');
    await userEvent.selectOptions(await screen.findByLabelText('Tipo de solicitud'), 'Rectificación de nota');
    await userEvent.type(screen.getByLabelText('Ubicación'), 'Facultad de Sistemas');
    await userEvent.selectOptions(screen.getByLabelText('Prioridad'), 'Media');
    await userEvent.type(screen.getByLabelText('Descripción'), 'Mi nota del parcial no coincide');
  }

  it('HU-15 scenario 2: blocks a procedure without document and sends nothing', async () => {
    const fetchMock = fakeApi(CATALOGOS);
    renderEn('/solicitudes/nueva');

    await completarTramite();
    expect(screen.getByText('Documento de sustento (obligatorio)')).toBeInTheDocument();
    await userEvent.click(screen.getByRole('button', { name: 'Registrar solicitud' }));

    expect(screen.getByText('Debe adjuntar el documento de sustento.')).toBeInTheDocument();
    expect(fetchMock.mock.calls.some(([, init]) => init?.method === 'POST')).toBe(false);
  });

  it('HU-04 scenario 1: sends data and files, then opens the new request', async () => {
    let enviado: FormData | undefined;
    fakeApi({
      ...CATALOGOS,
      ...DETALLE_RUTAS,
      'POST /api/solicitudes': (init) => {
        enviado = init?.body as FormData;
        return json(201, { id: 15, codigo: 'SOL-2026-0015', estado: 'En evaluación', fechaRegistro: '', fechaLimite: '' });
      },
    });
    renderEn('/solicitudes/nueva');

    await completarTramite();
    const pdf = new File(['%PDF-1.4'], 'sustento.pdf', { type: 'application/pdf' });
    await userEvent.upload(screen.getByLabelText('Documento de sustento (obligatorio)'), pdf);
    await userEvent.click(screen.getByRole('button', { name: 'Registrar solicitud' }));

    expect(await screen.findByRole('status')).toHaveTextContent('Solicitud SOL-2026-0015 registrada');
    const datos = JSON.parse(await (enviado!.get('datos') as Blob).text());
    expect(datos).toEqual({ tipoSolicitudId: 20, prioridadId: 2, descripcion: 'Mi nota del parcial no coincide', ubicacion: 'Facultad de Sistemas' });
    expect((enviado!.getAll('archivos')[0] as File).name).toBe('sustento.pdf');
  });

  it('HU-04 scenario 3: a server error keeps the entered data', async () => {
    fakeApi({ ...CATALOGOS, 'POST /api/solicitudes': () => json(503, { detail: 'Servicio no disponible' }) });
    renderEn('/solicitudes/nueva');

    await userEvent.selectOptions(await screen.findByLabelText('Categoría'), 'Soporte técnico e infraestructura');
    await userEvent.selectOptions(await screen.findByLabelText('Tipo de solicitud'), 'Proyector');
    await userEvent.type(screen.getByLabelText('Ubicación'), 'Salón A-101');
    await userEvent.selectOptions(screen.getByLabelText('Prioridad'), 'Alta');
    await userEvent.type(screen.getByLabelText('Descripción'), 'No enciende');
    await userEvent.click(screen.getByRole('button', { name: 'Registrar solicitud' }));

    expect(await screen.findByRole('alert')).toHaveTextContent('Servicio no disponible');
    expect(screen.getByLabelText('Descripción')).toHaveValue('No enciende');
  });
});

describe('MisSolicitudesPage', () => {
  it('HU-05 scenario 2: shows the empty message', async () => {
    fakeApi({ ...CATALOGOS, 'GET /api/solicitudes/mis-solicitudes': () => json(200, { content: [], page: { size: 10, number: 0, totalElements: 0, totalPages: 0 } }) });
    renderEn('/solicitudes');

    expect(await screen.findByText('Aún no tienes solicitudes.')).toBeInTheDocument();
  });

  it('HU-05 scenario 1: lists code, type, state and the overdue mark', async () => {
    fakeApi({
      ...CATALOGOS,
      'GET /api/solicitudes/mis-solicitudes': () =>
        json(200, {
          content: [{ id: 15, codigo: 'SOL-2026-0015', categoria: 'Soporte técnico e infraestructura', tipo: 'Proyector', prioridad: 'Alta', estado: 'Registrada', estadoFinal: false, fechaRegistro: '2026-10-17T09:30:00', fechaLimite: '2026-10-18T09:30:00', vencida: true }],
          page: { size: 10, number: 0, totalElements: 1, totalPages: 1 },
        }),
    });
    renderEn('/solicitudes');

    const fila = (await screen.findByRole('link', { name: 'SOL-2026-0015' })).closest('tr')!;
    expect(within(fila).getByText('Proyector')).toBeInTheDocument();
    expect(within(fila).getByText('Registrada')).toBeInTheDocument();
    expect(within(fila).getByText('Vencida')).toBeInTheDocument();
  });
});

describe('SolicitudDetallePage', () => {
  it('HU-06 scenario 1: shows data, evidence and the automatic derivation in the history', async () => {
    fakeApi(DETALLE_RUTAS);
    renderEn('/solicitudes/15');

    expect(await screen.findByRole('heading', { name: 'SOL-2026-0015' })).toBeInTheDocument();
    expect(screen.getByText('Secretaría académica')).toBeInTheDocument();
    expect(screen.getByText(/sustento\.pdf/)).toBeInTheDocument();
    expect(screen.getByText('Derivada automáticamente a Secretaría académica')).toBeInTheDocument();
    expect(screen.getByText(/· Sistema/)).toBeInTheDocument();
  });

  it('HU-06 scenario 2: an unknown or foreign request shows not found', async () => {
    fakeApi({});
    renderEn('/solicitudes/999');

    await waitFor(() => expect(screen.getByRole('heading', { name: 'Solicitud no encontrada.' })).toBeInTheDocument());
  });
});

describe('BandejaPage', () => {
  it('HU-07: lists requests, filters by vencidas and prioridad, and links to detail', async () => {
    guardarSesion({ token: 't', usuario: { id: 3, nombre: 'Sup', correo: 'sup@unmsm.edu.pe', rol: 'SUPERVISOR', areaId: null } });
    const fetchMock = fakeApi({
      ...CATALOGOS,
      'GET /api/solicitudes': () =>
        json(200, {
          content: [
            {
              id: 21,
              codigo: 'SOL-2026-0021',
              categoria: 'Soporte técnico e infraestructura',
              tipo: 'Proyector',
              prioridad: 'Alta',
              estado: 'En evaluación',
              estadoFinal: false,
              fechaRegistro: '2026-10-17T09:30:00',
              fechaLimite: '2026-10-18T09:30:00',
              vencida: true,
            },
          ],
          page: { size: 20, number: 0, totalElements: 1, totalPages: 1 },
        }),
    });
    renderEn('/bandeja');

    const fila = (await screen.findByRole('link', { name: 'SOL-2026-0021' })).closest('tr')!;
    expect(within(fila).getByText('Alta')).toBeInTheDocument();
    expect(within(fila).getByText('Vencida')).toBeInTheDocument();

    await userEvent.selectOptions(screen.getByLabelText('Prioridad'), '1');
    await userEvent.selectOptions(screen.getByLabelText('Vencidas'), 'true');

    await waitFor(() => {
      expect(
        fetchMock.mock.calls.some(([url]) => String(url).includes('/api/solicitudes?') && String(url).includes('prioridadId=1') && String(url).includes('vencidas=true')),
      ).toBe(true);
    });
  });
});

describe('AsignadasPage', () => {
  it('HU-09: lists requests assigned to the technician', async () => {
    guardarSesion({ token: 't', usuario: { id: 6, nombre: 'Luis Tec', correo: 'tec@unmsm.edu.pe', rol: 'TECNICO', areaId: 1 } });
    fakeApi({
      ...CATALOGOS,
      'GET /api/solicitudes/asignadas': () =>
        json(200, {
          content: [
            {
              id: 30,
              codigo: 'SOL-2026-0030',
              categoria: 'Soporte técnico e infraestructura',
              tipo: 'Proyector',
              prioridad: 'Media',
              estado: 'Asignada',
              estadoFinal: false,
              fechaRegistro: '2026-10-18T10:00:00',
              fechaLimite: '2026-10-19T10:00:00',
              vencida: false,
            },
          ],
          page: { size: 10, number: 0, totalElements: 1, totalPages: 1 },
        }),
    });
    renderEn('/asignadas');

    expect(await screen.findByRole('link', { name: 'SOL-2026-0030' })).toBeInTheDocument();
    expect(screen.getByText('Asignada')).toBeInTheDocument();
  });
});

describe('SolicitudDetallePage gestion', () => {
  it('HU-08 & HU-07: supervisor assigns technician, updates priority, and validates rejection reason', async () => {
    guardarSesion({ token: 't', usuario: { id: 3, nombre: 'Sup', correo: 'sup@unmsm.edu.pe', rol: 'SUPERVISOR', areaId: null } });
    let detalleActual: SolicitudDetalle = {
      ...DETALLE,
      id: 40,
      codigo: 'SOL-2026-0040',
      categoria: 'Soporte técnico e infraestructura',
      tipo: 'Proyector',
      area: 'Soporte técnico',
      areaId: 1,
      prioridad: 'Media',
      prioridadId: 2,
      estado: 'En evaluación',
    };
    let historialActual: Historial[] = [
      { id: 1, estadoAnterior: null, estadoNuevo: 'Registrada', usuario: 'Ana', observacion: null, fecha: '2026-10-17T09:30:00' },
    ];
    let transicionesActuales: TransicionDisponible[] = [
      { estadoDestinoId: 3, nombre: 'Asignada', esFinal: false, requiereMotivo: false },
      { estadoDestinoId: 8, nombre: 'Rechazada', esFinal: true, requiereMotivo: true },
    ];

    fakeApi({
      ...CATALOGOS,
      'GET /api/solicitudes/40': () => json(200, detalleActual),
      'GET /api/solicitudes/40/historial': () => json(200, historialActual),
      'GET /api/solicitudes/40/evidencias': () => json(200, []),
      'GET /api/solicitudes/40/transiciones': () => json(200, transicionesActuales),
      'GET /api/usuarios/tecnicos': () =>
        json(200, [{ id: 6, nombre: 'Luis Tec', correo: 'tec@unmsm.edu.pe', rol: 'TECNICO', areaId: 1, activo: true }]),
      'PUT /api/solicitudes/40/prioridad': (init) => {
        const body = JSON.parse(String(init?.body)) as { prioridadId: number };
        detalleActual = { ...detalleActual, prioridad: body.prioridadId === 1 ? 'Alta' : 'Media', prioridadId: body.prioridadId };
        return json(200, detalleActual);
      },
      'PUT /api/solicitudes/40/asignar': () => {
        detalleActual = { ...detalleActual, estado: 'Asignada', responsable: { id: 6, nombre: 'Luis Tec' } };
        historialActual = [
          ...historialActual,
          { id: 2, estadoAnterior: 'En evaluación', estadoNuevo: 'Asignada', usuario: 'Sup', observacion: 'Asignada a Luis Tec', fecha: '2026-10-17T10:00:00' },
        ];
        transicionesActuales = [];
        return json(200, detalleActual);
      },
    });
    renderEn('/solicitudes/40');

    expect(await screen.findByLabelText('Técnico responsable')).toBeInTheDocument();
    // Asignada is handled by the Asignar panel, not duplicated as a bare state button
    expect(screen.queryByRole('button', { name: 'Asignada' })).not.toBeInTheDocument();

    // Rejection without reason is blocked client-side
    await userEvent.click(screen.getByRole('button', { name: 'Rechazada' }));
    expect(screen.getByRole('alert')).toHaveTextContent('Debe indicar el motivo para pasar a Rechazada.');

    // Update priority
    await userEvent.selectOptions(screen.getByLabelText('Prioridad'), '1');
    await userEvent.click(screen.getByRole('button', { name: 'Actualizar prioridad' }));
    expect(await screen.findByRole('status')).toHaveTextContent('Prioridad actualizada a Alta.');

    // Assign technician and refresh history
    await userEvent.selectOptions(screen.getByLabelText('Técnico responsable'), '6');
    await userEvent.click(screen.getByRole('button', { name: 'Asignar' }));
    expect(await screen.findByRole('status')).toHaveTextContent('Solicitud asignada a Luis Tec.');
    expect(screen.getByText('Asignada a Luis Tec')).toBeInTheDocument();
  });

  it('HU-09 & HU-15: technician transitions state and uploads additional evidence', async () => {
    guardarSesion({ token: 't', usuario: { id: 6, nombre: 'Luis Tec', correo: 'tec@unmsm.edu.pe', rol: 'TECNICO', areaId: 1 } });
    let detalleActual: SolicitudDetalle = {
      ...DETALLE,
      id: 41,
      codigo: 'SOL-2026-0041',
      estado: 'Asignada',
      responsable: { id: 6, nombre: 'Luis Tec' },
    };
    let evidenciasActuales = [
      { id: 1, nombreArchivo: 'inicial.png', tipoMime: 'image/png', tamanoBytes: 1024, fechaSubida: '2026-10-17T09:30:00', subidoPor: 'Ana' },
    ];
    let historialActual: Historial[] = [
      { id: 1, estadoAnterior: null, estadoNuevo: 'Asignada', usuario: 'Sup', observacion: null, fecha: '2026-10-17T09:30:00' },
    ];

    fakeApi({
      ...CATALOGOS,
      'GET /api/solicitudes/41': () => json(200, detalleActual),
      'GET /api/solicitudes/41/historial': () => json(200, historialActual),
      'GET /api/solicitudes/41/evidencias': () => json(200, evidenciasActuales),
      'GET /api/solicitudes/41/transiciones': () =>
        json(200, [{ estadoDestinoId: 4, nombre: 'En atención', esFinal: false, requiereMotivo: false }]),
      'PUT /api/solicitudes/41/estado': () => {
        detalleActual = { ...detalleActual, estado: 'En atención' };
        historialActual = [
          ...historialActual,
          { id: 2, estadoAnterior: 'Asignada', estadoNuevo: 'En atención', usuario: 'Luis Tec', observacion: 'Revisando en salón', fecha: '2026-10-17T10:15:00' },
        ];
        return json(200, detalleActual);
      },
      'POST /api/solicitudes/41/evidencias': () => {
        const nueva = { id: 2, nombreArchivo: 'reparado.png', tipoMime: 'image/png', tamanoBytes: 512, fechaSubida: '2026-10-17T10:20:00', subidoPor: 'Luis Tec' };
        evidenciasActuales = [...evidenciasActuales, nueva];
        return json(201, [nueva]);
      },
    });
    renderEn('/solicitudes/41');

    expect(await screen.findByRole('button', { name: 'En atención' })).toBeInTheDocument();
    expect(screen.queryByLabelText('Técnico responsable')).not.toBeInTheDocument();

    await userEvent.type(screen.getByLabelText('Comentario u observación'), 'Revisando en salón');
    await userEvent.click(screen.getByRole('button', { name: 'En atención' }));
    expect(await screen.findByRole('status')).toHaveTextContent('Estado actualizado a En atención.');
    expect(screen.getByText('Revisando en salón')).toBeInTheDocument();

    const png = new File(['x'], 'reparado.png', { type: 'image/png' });
    await userEvent.upload(screen.getByLabelText(/Agregar evidencias/), png);
    await userEvent.click(screen.getByRole('button', { name: 'Subir evidencias' }));
    expect(await screen.findByText(/reparado\.png/)).toBeInTheDocument();
  });
});
