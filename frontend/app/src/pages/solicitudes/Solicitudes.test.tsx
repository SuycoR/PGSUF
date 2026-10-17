import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import { AuthProvider } from '../../auth/AuthContext';
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
