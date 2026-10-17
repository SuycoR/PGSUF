import { useState, type FormEvent } from 'react';
import { Link, Navigate, useNavigate } from 'react-router';

import { ApiError, apiFetch } from '../api/client';
import type { Usuario } from '../auth/types';
import { useAuth } from '../auth/useAuth';
import { inicioPara } from '../routes/menu';
import './LoginPage.css';
import { validarRegistro, type ErroresRegistro } from './validarRegistro';

const CAMPOS_BACKEND: Record<string, keyof ErroresRegistro> = {
  nombre: 'nombre',
  correo: 'correo',
  password: 'password',
};

export function RegistroPage() {
  const { usuario } = useAuth();
  const navigate = useNavigate();
  const [nombre, setNombre] = useState('');
  const [correo, setCorreo] = useState('');
  const [password, setPassword] = useState('');
  const [errores, setErrores] = useState<ErroresRegistro>({});
  const [errorServidor, setErrorServidor] = useState<string | null>(null);
  const [enviando, setEnviando] = useState(false);

  if (usuario) {
    return <Navigate to={inicioPara(usuario.rol)} replace />;
  }

  async function onSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setErrorServidor(null);

    const encontrados = validarRegistro(nombre, correo, password);
    setErrores(encontrados);
    if (Object.keys(encontrados).length > 0) {
      return;
    }

    setEnviando(true);
    try {
      await apiFetch<Usuario>('/auth/registro', {
        method: 'POST',
        body: JSON.stringify({ nombre: nombre.trim(), correo: correo.trim(), password }),
      });
      navigate('/login', { replace: true, state: { mensaje: 'Cuenta creada. Ya puede iniciar sesión.' } });
    } catch (error) {
      if (error instanceof ApiError && error.errores.length > 0) {
        const delServidor: ErroresRegistro = {};
        for (const { campo, mensaje } of error.errores) {
          const clave = CAMPOS_BACKEND[campo];
          if (clave) {
            delServidor[clave] = mensaje;
          }
        }
        setErrores(delServidor);
      }
      setErrorServidor(error instanceof ApiError ? error.message : 'Ocurrió un error inesperado.');
    } finally {
      setEnviando(false);
    }
  }

  return (
    <div className="login">
      <form className="tarjeta login-tarjeta" onSubmit={onSubmit} noValidate>
        <h1>Crear cuenta</h1>
        <p className="login-subtitulo">Para estudiantes, docentes y personal administrativo</p>

        {errorServidor && (
          <div className="alerta-error" role="alert">
            {errorServidor}
          </div>
        )}

        <div className="campo">
          <label htmlFor="nombre">Nombre completo</label>
          <input
            id="nombre"
            autoComplete="name"
            value={nombre}
            onChange={(e) => setNombre(e.target.value)}
            aria-invalid={Boolean(errores.nombre)}
            aria-describedby={errores.nombre ? 'nombre-error' : undefined}
          />
          {errores.nombre && <span id="nombre-error" className="campo-error">{errores.nombre}</span>}
        </div>

        <div className="campo">
          <label htmlFor="correo">Correo institucional</label>
          <input
            id="correo"
            type="email"
            autoComplete="email"
            value={correo}
            onChange={(e) => setCorreo(e.target.value)}
            aria-invalid={Boolean(errores.correo)}
            aria-describedby={errores.correo ? 'correo-error' : undefined}
          />
          {errores.correo && <span id="correo-error" className="campo-error">{errores.correo}</span>}
        </div>

        <div className="campo">
          <label htmlFor="password">Contraseña</label>
          <input
            id="password"
            type="password"
            autoComplete="new-password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            aria-invalid={Boolean(errores.password)}
            aria-describedby="password-ayuda"
          />
          <span id="password-ayuda" className={errores.password ? 'campo-error' : 'campo-ayuda'}>
            {errores.password ?? 'Entre 8 y 72 caracteres.'}
          </span>
        </div>

        <button type="submit" className="btn btn-primario login-boton" disabled={enviando}>
          {enviando ? 'Creando cuenta…' : 'Crear cuenta'}
        </button>

        <p className="login-pie">
          ¿Ya tiene cuenta? <Link to="/login">Inicie sesión</Link>
        </p>
      </form>
    </div>
  );
}
