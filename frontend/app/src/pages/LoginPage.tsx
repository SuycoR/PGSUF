import { useState, type FormEvent } from 'react';
import { Navigate, useNavigate } from 'react-router';

import { ApiError, apiFetch } from '../api/client';
import type { Sesion } from '../auth/types';
import { useAuth } from '../auth/useAuth';
import { inicioPara } from '../routes/menu';
import './LoginPage.css';
import { validarLogin, type ErroresLogin } from './validarLogin';

export function LoginPage() {
  const { usuario, iniciarSesion } = useAuth();
  const navigate = useNavigate();
  const [correo, setCorreo] = useState('');
  const [password, setPassword] = useState('');
  const [errores, setErrores] = useState<ErroresLogin>({});
  const [errorServidor, setErrorServidor] = useState<string | null>(null);
  const [enviando, setEnviando] = useState(false);

  if (usuario) {
    return <Navigate to={inicioPara(usuario.rol)} replace />;
  }

  async function onSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setErrorServidor(null);

    const encontrados = validarLogin(correo, password);
    setErrores(encontrados);
    if (Object.keys(encontrados).length > 0) {
      return; // HU-01, scenario 3: the request is not sent
    }

    setEnviando(true);
    try {
      const sesion = await apiFetch<Sesion>('/auth/login', {
        method: 'POST',
        body: JSON.stringify({ correo: correo.trim(), password }),
      });
      iniciarSesion(sesion);
      navigate(inicioPara(sesion.usuario.rol), { replace: true });
    } catch (error) {
      setErrorServidor(error instanceof ApiError ? error.message : 'Ocurrió un error inesperado.');
    } finally {
      setEnviando(false);
    }
  }

  return (
    <div className="login">
      <form className="tarjeta login-tarjeta" onSubmit={onSubmit} noValidate>
        <h1>Iniciar sesión</h1>
        <p className="login-subtitulo">Servicios Universitarios</p>

        {errorServidor && (
          <div className="alerta-error" role="alert">
            {errorServidor}
          </div>
        )}

        <div className="campo">
          <label htmlFor="correo">Correo institucional</label>
          <input
            id="correo"
            type="email"
            autoComplete="username"
            value={correo}
            onChange={(e) => setCorreo(e.target.value)}
            aria-invalid={Boolean(errores.correo)}
            aria-describedby={errores.correo ? 'correo-error' : undefined}
          />
          {errores.correo && (
            <span id="correo-error" className="campo-error">
              {errores.correo}
            </span>
          )}
        </div>

        <div className="campo">
          <label htmlFor="password">Contraseña</label>
          <input
            id="password"
            type="password"
            autoComplete="current-password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            aria-invalid={Boolean(errores.password)}
            aria-describedby={errores.password ? 'password-error' : undefined}
          />
          {errores.password && (
            <span id="password-error" className="campo-error">
              {errores.password}
            </span>
          )}
        </div>

        <button type="submit" className="btn btn-primario login-boton" disabled={enviando}>
          {enviando ? 'Ingresando…' : 'Ingresar'}
        </button>
      </form>
    </div>
  );
}
