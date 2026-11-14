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
  areaId?: number;
  requiereAdjunto: boolean;
  prioridad: string;
  prioridadId?: number;
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

export interface TransicionDisponible {
  estadoDestinoId: number;
  nombre: string;
  esFinal: boolean;
  requiereMotivo: boolean;
}

export interface Tecnico {
  id: number;
  nombre: string;
  correo: string;
  rol: string;
  areaId: number | null;
  activo: boolean;
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

export interface Comentario {
  id: number;
  autorId: number;
  autor: string;
  autorRol: string;
  texto: string;
  fecha: string;
}

export type TipoNotificacion =
  | 'SOLICITUD_REGISTRADA'
  | 'SOLICITUD_DERIVADA'
  | 'SOLICITUD_ASIGNADA'
  | 'ESTADO_CAMBIADO'
  | 'COMENTARIO_NUEVO'
  | 'RESERVA_REGISTRADA'
  | 'RESERVA_RESUELTA';

export interface Notificacion {
  id: number;
  tipo: TipoNotificacion | string;
  titulo: string;
  mensaje: string;
  solicitudId: number | null;
  reservaId: number | null;
  leida: boolean;
  fechaCreacion: string;
  fechaLectura: string | null;
}

export interface CantidadNoLeidas {
  cantidad: number;
}

export type TipoAmbiente = 'AUDITORIO' | 'LABORATORIO' | 'SALON';
export type Pabellon = 'ANTIGUO' | 'NUEVO' | 'CENTRAL';

export interface Ambiente {
  id: number;
  nombre: string;
  tipo: TipoAmbiente;
  pabellon: Pabellon;
  piso: number | null;
  capacidad: number;
  activo: boolean;
}

export interface IntervaloHorario {
  horaInicio: string;
  horaFin: string;
  codigo: string;
}

export interface DisponibilidadResponse {
  ambienteId: number;
  ambienteNombre: string;
  fecha: string;
  reservasAprobadas: IntervaloHorario[];
}

export interface CrearReservaRequest {
  ambienteId: number;
  fecha: string;
  horaInicio: string;
  horaFin: string;
  motivo: string;
}

export interface RechazarReservaRequest {
  motivo: string;
}

export interface ReservaResponse {
  id: number;
  codigo: string;
  ambiente: {
    id: number;
    nombre: string;
    tipo: TipoAmbiente;
    pabellon: Pabellon;
    piso: number | null;
  };
  usuario: {
    id: number;
    nombre: string;
    correo: string;
  };
  aprobador: {
    id: number;
    nombre: string;
    correo: string;
  } | null;
  estado: {
    id: number;
    nombre: string;
  };
  fecha: string;
  horaInicio: string;
  horaFin: string;
  motivo: string;
  motivoRechazo: string | null;
  fechaRegistro: string;
  fechaResolucion: string | null;
}

export interface ReservaResumen {
  id: number;
  codigo: string;
  ambienteId: number;
  ambienteNombre: string;
  usuarioId: number;
  usuarioNombre: string;
  estadoId: number;
  estadoNombre: string;
  fecha: string;
  horaInicio: string;
  horaFin: string;
  motivo: string;
  motivoRechazo: string | null;
  fechaRegistro: string;
  fechaResolucion: string | null;
}

export interface KpiItem {
  id: number | null;
  nombre: string;
  cantidad: number;
}

export interface DashboardResponse {
  totalRegistradas: number;
  totalPendientes: number;
  totalAtendidas: number;
  totalVencidas: number;
  porcentajeResueltas: number;
  tiempoPromedioAtencionHoras: number;
  porCategoria: KpiItem[];
  porPrioridad: KpiItem[];
  porResponsable: KpiItem[];
}
