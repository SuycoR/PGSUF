package pe.edu.unmsm.servicios.common;

import org.springframework.http.HttpStatus;

/**
 * A broken business rule (RN-xx). Carries the HTTP status and a stable error code
 * that the frontend can rely on, for example {@code TRANSICION_NO_PERMITIDA}.
 */
public class BusinessException extends RuntimeException {

    private final HttpStatus status;
    private final String codigo;
    private final String titulo;

    public BusinessException(HttpStatus status, String codigo, String titulo, String detalle) {
        super(detalle);
        this.status = status;
        this.codigo = codigo;
        this.titulo = titulo;
    }

    public static BusinessException notFound(String titulo, String detalle) {
        return new BusinessException(HttpStatus.NOT_FOUND, "NO_ENCONTRADO", titulo, detalle);
    }

    public static BusinessException conflict(String codigo, String titulo, String detalle) {
        return new BusinessException(HttpStatus.CONFLICT, codigo, titulo, detalle);
    }

    public static BusinessException unprocessable(String codigo, String titulo, String detalle) {
        return new BusinessException(HttpStatus.UNPROCESSABLE_ENTITY, codigo, titulo, detalle);
    }

    public static BusinessException forbidden(String detalle) {
        return new BusinessException(HttpStatus.FORBIDDEN, "ACCESO_DENEGADO", "Acceso denegado", detalle);
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCodigo() {
        return codigo;
    }

    public String getTitulo() {
        return titulo;
    }
}
