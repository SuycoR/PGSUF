export const MAXIMO_ARCHIVOS = 5;
export const TAMANO_MAXIMO = 5 * 1024 * 1024;
const EXTENSIONES = ['pdf', 'jpg', 'jpeg', 'png'];

export interface FormularioSolicitud {
  categoriaId: string;
  tipoSolicitudId: string;
  prioridadId: string;
  ubicacion: string;
  descripcion: string;
  archivos: File[];
}

export type ErroresSolicitud = Partial<Record<keyof FormularioSolicitud, string>>;

/**
 * Client-side checks of HU-04 / HU-15. The server validates again, including the real file content;
 * here only the extension and size can be checked.
 */
export function validarSolicitud(form: FormularioSolicitud, requiereAdjunto: boolean): ErroresSolicitud {
  const errores: ErroresSolicitud = {};
  if (!form.categoriaId) errores.categoriaId = 'Seleccione una categoría.';
  if (!form.tipoSolicitudId) errores.tipoSolicitudId = 'Seleccione el tipo de solicitud.';
  if (!form.prioridadId) errores.prioridadId = 'Seleccione la prioridad.';

  if (!form.ubicacion.trim()) errores.ubicacion = 'Indique la ubicación.';
  else if (form.ubicacion.trim().length > 150) errores.ubicacion = 'La ubicación admite hasta 150 caracteres.';

  if (!form.descripcion.trim()) errores.descripcion = 'Describa el problema o trámite.';
  else if (form.descripcion.trim().length > 2000) errores.descripcion = 'La descripción admite hasta 2000 caracteres.';

  const errorArchivos = validarArchivos(form.archivos, requiereAdjunto);
  if (errorArchivos) errores.archivos = errorArchivos;
  return errores;
}

function validarArchivos(archivos: File[], requiereAdjunto: boolean): string | null {
  if (requiereAdjunto && archivos.length === 0) {
    return 'Debe adjuntar el documento de sustento.';
  }
  if (archivos.length > MAXIMO_ARCHIVOS) {
    return `Puede adjuntar hasta ${MAXIMO_ARCHIVOS} archivos.`;
  }
  for (const archivo of archivos) {
    const extension = archivo.name.split('.').pop()?.toLowerCase() ?? '';
    if (!EXTENSIONES.includes(extension)) {
      return `"${archivo.name}" no es PDF, JPG o PNG.`;
    }
    if (archivo.size > TAMANO_MAXIMO) {
      return `"${archivo.name}" supera los 5 MB.`;
    }
  }
  return null;
}
