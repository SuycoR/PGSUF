import { NavLink, Outlet } from 'react-router';

import { useAuth } from '../../auth/useAuth';
import { menuPara } from '../../routes/menu';
import './AppLayout.css';

export function AppLayout() {
  const { usuario, cerrarSesion } = useAuth();
  if (!usuario) {
    return null;
  }

  return (
    <div className="layout">
      <header className="layout-header">
        <span className="layout-marca">Servicios Universitarios</span>
        <div className="layout-usuario">
          <NavLink to="/notificaciones" className="layout-notificaciones" aria-label="Notificaciones">
            Notificaciones
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
