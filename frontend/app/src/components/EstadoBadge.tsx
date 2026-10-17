import './EstadoBadge.css';

const VARIANTE: Record<string, string> = {
  Registrada: 'pendiente',
  'En evaluación': 'en-curso',
  Asignada: 'en-curso',
  'En atención': 'en-curso',
  Resuelta: 'resuelta',
  Cerrada: 'resuelta',
  Aprobada: 'resuelta',
  Rechazada: 'rechazada',
};

export function EstadoBadge({ estado }: { estado: string }) {
  return <span className={`estado-badge estado-${VARIANTE[estado] ?? 'pendiente'}`}>{estado}</span>;
}
