const FECHA_HORA = new Intl.DateTimeFormat('es-PE', {
  day: '2-digit',
  month: '2-digit',
  year: 'numeric',
  hour: '2-digit',
  minute: '2-digit',
});

/** The API sends local Lima times without offset (e.g. "2026-10-17T09:30:00"). */
export function formatearFechaHora(iso: string): string {
  return FECHA_HORA.format(new Date(iso));
}

export function formatearTamano(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(0)} KB`;
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
}
