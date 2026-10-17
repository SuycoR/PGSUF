import type { ReactNode } from 'react';
import { Navigate, Route, Routes } from 'react-router';

import { useAuth } from '../auth/useAuth';
import { AppLayout } from '../components/layout/AppLayout';
import { EnConstruccion } from '../pages/EnConstruccion';
import { LoginPage } from '../pages/LoginPage';
import { RegistroPage } from '../pages/RegistroPage';
import { MisSolicitudesPage } from '../pages/solicitudes/MisSolicitudesPage';
import { NuevaSolicitudPage } from '../pages/solicitudes/NuevaSolicitudPage';
import { SolicitudDetallePage } from '../pages/solicitudes/SolicitudDetallePage';
import { NoEncontradaPage, SinPermisoPage } from '../pages/SinPermisoPage';
import { inicioPara, rolesDe } from './menu';
import { RequireAuth } from './RequireAuth';

function Inicio() {
  const { usuario } = useAuth();
  return <Navigate to={usuario ? inicioPara(usuario.rol) : '/login'} replace />;
}

function protegida(ruta: string, elemento: ReactNode) {
  return <RequireAuth roles={rolesDe(ruta)}>{elemento}</RequireAuth>;
}

export function AppRoutes() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route path="/registro" element={<RegistroPage />} />
      <Route path="/" element={<Inicio />} />

      <Route
        element={
          <RequireAuth>
            <AppLayout />
          </RequireAuth>
        }
      >
        <Route path="/solicitudes" element={protegida('/solicitudes', <MisSolicitudesPage />)} />
        <Route path="/solicitudes/nueva" element={protegida('/solicitudes/nueva', <NuevaSolicitudPage />)} />
        {/* Any authenticated user may open a detail page; the backend decides visibility (RN-20). */}
        <Route path="/solicitudes/:id" element={<SolicitudDetallePage />} />
        <Route
          path="/reservas"
          element={protegida('/reservas', <EnConstruccion titulo="Mis reservas" historia="HU-13" sprint="S5" />)}
        />
        <Route
          path="/asignadas"
          element={protegida('/asignadas', <EnConstruccion titulo="Solicitudes asignadas" historia="HU-09" sprint="S3" />)}
        />
        <Route
          path="/bandeja"
          element={protegida('/bandeja', <EnConstruccion titulo="Bandeja de solicitudes" historia="HU-07" sprint="S3" />)}
        />
        <Route
          path="/reservas/aprobacion"
          element={protegida('/reservas/aprobacion', <EnConstruccion titulo="Aprobación de reservas" historia="HU-14" sprint="S5" />)}
        />
        <Route
          path="/dashboard"
          element={protegida('/dashboard', <EnConstruccion titulo="Dashboard" historia="HU-11" sprint="S7" />)}
        />
        <Route
          path="/admin"
          element={protegida('/admin', <EnConstruccion titulo="Administración" historia="HU-12" sprint="S7" />)}
        />
        <Route
          path="/notificaciones"
          element={<EnConstruccion titulo="Notificaciones" historia="HU-17" sprint="S6" />}
        />
        <Route path="/sin-permiso" element={<SinPermisoPage />} />
        <Route path="*" element={<NoEncontradaPage />} />
      </Route>
    </Routes>
  );
}
