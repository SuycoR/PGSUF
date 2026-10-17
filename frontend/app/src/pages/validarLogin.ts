export interface ErroresLogin {
  correo?: string;
  password?: string;
}

const CORREO_VALIDO = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

export function validarLogin(correo: string, password: string): ErroresLogin {
  const errores: ErroresLogin = {};
  if (!correo.trim()) {
    errores.correo = 'Ingrese su correo.';
  } else if (!CORREO_VALIDO.test(correo.trim())) {
    errores.correo = 'Ingrese un correo válido.';
  }
  if (!password) {
    errores.password = 'Ingrese su contraseña.';
  }
  return errores;
}
