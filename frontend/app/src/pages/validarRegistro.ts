export interface ErroresRegistro {
  nombre?: string;
  correo?: string;
  password?: string;
}

const CORREO_VALIDO = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

/** Mirrors the backend rules of HU-02 so the user gets feedback before sending. */
export function validarRegistro(nombre: string, correo: string, password: string): ErroresRegistro {
  const errores: ErroresRegistro = {};
  if (!nombre.trim()) {
    errores.nombre = 'Ingrese su nombre.';
  } else if (nombre.trim().length > 120) {
    errores.nombre = 'El nombre admite hasta 120 caracteres.';
  }
  if (!correo.trim()) {
    errores.correo = 'Ingrese su correo.';
  } else if (!CORREO_VALIDO.test(correo.trim())) {
    errores.correo = 'Ingrese un correo válido.';
  }
  if (password.length < 8 || password.length > 72) {
    errores.password = 'La contraseña debe tener entre 8 y 72 caracteres.';
  }
  return errores;
}
