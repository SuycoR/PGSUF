import { useCallback, useEffect, useState } from 'react';
import { NavLink, Outlet } from 'react-router';

import { NOTIFICACIONES_ACTUALIZADAS, notificaciones } from '../../api/notificaciones';
import { useAuth } from '../../auth/useAuth';
import { menuPara } from '../../routes/menu';
import './AppLayout.css';

const INTERVALO_CAMPANA_MS = 30_000;

export function AppLayout() {
  const { usuario, cerrarSesion } = useAuth();
  const [noLeidas, setNoLeidas] = useState(0);

  const cargarNoLeidas = useCallback(() => {
    if (!usuario) return;
    notificaciones
      .cantidadNoLeidas()
      .then((res) => {
        if (typeof res?.cantidad === 'number') {
          setNoLeidas(res.cantidad);
        }
      })
      .catch(() => {
        // Polling failure must not interrupt navigation.
      });
  }, [usuario]);

  useEffect(() => {
    if (!usuario) return;
    cargarNoLeidas();
    const timer = window.setInterval(cargarNoLeidas, INTERVALO_CAMPANA_MS);
    window.addEventListener(NOTIFICACIONES_ACTUALIZADAS, cargarNoLeidas);
    return () => {
      window.clearInterval(timer);
      window.removeEventListener(NOTIFICACIONES_ACTUALIZADAS, cargarNoLeidas);
    };
  }, [usuario, cargarNoLeidas]);

  if (!usuario) {
    return null;
  }

  const etiquetaNotificaciones =
    noLeidas > 0 ? `Notificaciones (${noLeidas} sin leer)` : 'Notificaciones';

  return (
    <div className="layout">
      <header className="layout-header">
        <span className="layout-marca">Servicios Universitarios</span>
        <div className="layout-usuario">
          <NavLink
            to="/notificaciones"
            className="layout-notificaciones"
            aria-label={etiquetaNotificaciones}
          >
            <svg
              className="layout-campana-icono"
              viewBox="0 0 24 24"
              width="18"
              height="18"
              fill="none"
              stroke="currentColor"
              strokeWidth="2"
              strokeLinecap="round"
              strokeLinejoin="round"
              aria-hidden="true"
            >
              <path d="M18 8a6 6 0 0 0-12 0c0 7-3 9-3 9h18s-3-2-3-9" />
              <path d="M13.73 21a2 2 0 0 1-3.46 0" />
            </svg>
            <span className="layout-notificaciones-texto">Notificaciones</span>
            {noLeidas > 0 && (
              <span className="layout-notificaciones-badge" aria-hidden="true">
                {noLeidas > 99 ? '99+' : noLeidas}
              </span>
            )}
          </NavLink>
          <span className="layout-usuario-nombre">{usuario.nombre}</span>
          <button type="button" className="btn layout-salir" onClick={cerrarSesion}>
            Salir
          </button>
        </div>
      </header>

      <nav className="layout-sidebar" aria-label="Menú principal">
        <ul>
          {menuPara(usuario.rol).map((opcion) => (
            <li key={opcion.ruta}>
              <NavLink to={opcion.ruta} end>
                {opcion.etiqueta}
              </NavLink>
            </li>
          ))}
        </ul>
      </nav>

      <main className="layout-contenido">
        <Outlet />
      </main>
    </div>
  );
}
