import { Link } from 'react-router';

export function SinPermisoPage() {
  return (
    <section className="tarjeta">
      <h1>Sin permiso</h1>
      <p>Su rol no tiene acceso a esta sección.</p>
      <Link to="/">Volver al inicio</Link>
    </section>
  );
}

export function NoEncontradaPage() {
  return (
    <section className="tarjeta">
      <h1>Página no encontrada</h1>
      <Link to="/">Volver al inicio</Link>
    </section>
  );
}
