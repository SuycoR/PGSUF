package pe.edu.unmsm.servicios.common;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Translates every error into an RFC 7807 ProblemDetail (see docs/07-api.md).
 * Stack traces and internal exception details are logged, never returned to the client.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    public record CampoError(String campo, String mensaje) {
    }

    @ExceptionHandler(DemasiadosIntentosException.class)
    public ResponseEntity<ProblemDetail> handleTooManyRequests(DemasiadosIntentosException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.TOO_MANY_REQUESTS, ex.getMessage());
        problem.setTitle(ex.getTitulo());
        problem.setProperty("codigo", ex.getCodigo());
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header(HttpHeaders.RETRY_AFTER, String.valueOf(ex.getRetryAfterSeconds()))
                .body(problem);
    }

    @ExceptionHandler(BusinessException.class)
    public ProblemDetail handleBusiness(BusinessException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(ex.getStatus(), ex.getMessage());
        problem.setTitle(ex.getTitulo());
        problem.setProperty("codigo", ex.getCodigo());
        return problem;
    }

    @ExceptionHandler(AuthenticationException.class)
    public ProblemDetail handleAuthentication(AuthenticationException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED,
                "Debe iniciar sesión para acceder a este recurso.");
        problem.setTitle("No autenticado");
        problem.setProperty("codigo", "NO_AUTENTICADO");
        return problem;
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail handleAccessDenied(AccessDeniedException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN,
                "No tiene permiso para realizar esta acción.");
        problem.setTitle("Acceso denegado");
        problem.setProperty("codigo", "ACCESO_DENEGADO");
        return problem;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception ex) {
        log.error("Unexpected error", ex);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR,
                "Ocurrió un error inesperado. Intente nuevamente.");
        problem.setTitle("Error interno");
        problem.setProperty("codigo", "ERROR_INTERNO");
        return problem;
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                "El cuerpo de la solicitud no es válido o tiene un formato incorrecto.");
        problem.setTitle("Solicitud mal formada");
        problem.setProperty("codigo", "SOLICITUD_INVALIDA");
        return handleExceptionInternal(ex, problem, headers, HttpStatus.BAD_REQUEST, request);
    }

    @Override
    protected ResponseEntity<Object> handleTypeMismatch(TypeMismatchException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                "Uno de los parámetros de la solicitud tiene un formato inválido.");
        problem.setTitle("Parámetro inválido");
        problem.setProperty("codigo", "PARAMETRO_INVALIDO");
        return handleExceptionInternal(ex, problem, headers, HttpStatus.BAD_REQUEST, request);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        List<CampoError> errores = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> new CampoError(
                        error.getField(),
                        error.isBindingFailure() ? "Valor inválido." : error.getDefaultMessage()))
                .toList();

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                "Uno o más campos son inválidos.");
        problem.setTitle("Datos inválidos");
        problem.setProperty("codigo", "DATOS_INVALIDOS");
        problem.setProperty("errores", errores);
        return handleExceptionInternal(ex, problem, headers, HttpStatus.BAD_REQUEST, request);
    }
}
