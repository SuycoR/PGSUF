/** Contracts of the REST API (docs/07-api.md). */

export interface Page<T> {
  content: T[];
  page: { size: number; number: number; totalElements: number; totalPages: number };
}

export interface Categoria {
  id: number;
  nombre: string;
  esTramite: boolean;
}

export interface TipoSolicitud {
  id: number;
  nombre: string;
  categoriaId: number;
  area: string;
  requiereAdjunto: boolean;
  slaHoras: number;
}

export interface Prioridad {
  id: number;
  nombre: string;
  orden: number;
}

export interface Estado {
  id: number;
  nombre: string;
  ambito: string;
  orden: number;
  esFinal: boolean;
}

export interface SolicitudCreada {
  id: number;
  codigo: string;
  estado: string;
  fechaRegistro: string;
  fechaLimite: string;
}

export interface SolicitudResumen {
  id: number;
  codigo: string;
  categoria: string;
  tipo: string;
  prioridad: string;
  estado: string;
  estadoFinal: boolean;
  fechaRegistro: string;
  fechaLimite: string;
  vencida: boolean;
}

export interface PersonaResumen {
  id: number;
  nombre: string;
}

export interface SolicitudDetalle {
  id: number;
  codigo: string;
  categoria: string;
  tipo: string;
  area: string;
  requiereAdjunto: boolean;
  prioridad: string;
  estado: string;
  estadoFinal: boolean;
  descripcion: string;
  ubicacion: string;
  respuesta: string | null;
  solicitante: PersonaResumen;
  responsable: PersonaResumen | null;
  fechaRegistro: string;
  fechaLimite: string;
  fechaCierre: string | null;
  vencida: boolean;
}

export interface Historial {
  id: number;
  estadoAnterior: string | null;
  estadoNuevo: string;
  usuario: string;
  observacion: string | null;
  fecha: string;
}

export interface Evidencia {
  id: number;
  nombreArchivo: string;
  tipoMime: string;
  tamanoBytes: number;
  fechaSubida: string;
  subidoPor: string;
}
