package id.payu.backoffice.config;

import id.payu.api.common.exception.problem.ProblemDetail;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * ARCH-ERR-001: RFC 9457 problem+json exception handler for backoffice-service.
 * Unique PayU error codes (BO_xxx) are preserved as error_code members.
 */
@RestControllerAdvice
public class BackofficeExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(BackofficeExceptionHandler.class);

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ProblemDetail> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        log.warn("Access denied: {}", ex.getMessage());
        return respond(HttpStatus.FORBIDDEN, "BO_403", "Insufficient permissions", request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        String errors = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .collect(Collectors.joining(", "));
        log.warn("Validation failed: {}", errors);
        return respond(HttpStatus.BAD_REQUEST, "BO_400", errors, request);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ProblemDetail> handleConstraintViolation(ConstraintViolationException ex, HttpServletRequest request) {
        log.warn("Constraint violation: {}", ex.getMessage());
        return respond(HttpStatus.BAD_REQUEST, "BO_400", ex.getMessage(), request);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ProblemDetail> handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest request) {
        log.warn("Invalid argument: {}", ex.getMessage());
        return respond(HttpStatus.BAD_REQUEST, "BO_400", ex.getMessage(), request);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ProblemDetail> handleIllegalState(IllegalStateException ex, HttpServletRequest request) {
        log.warn("Illegal state: {}", ex.getMessage());
        return respond(HttpStatus.CONFLICT, "BO_409", ex.getMessage(), request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleGeneric(Exception ex, HttpServletRequest request) {
        log.error("Unhandled exception in backoffice-service", ex);
        return respond(HttpStatus.INTERNAL_SERVER_ERROR, "BO_500",
                "An unexpected error occurred", request);
    }

    private ResponseEntity<ProblemDetail> respond(HttpStatus status, String errorCode, String message,
                                                  HttpServletRequest request) {
        return ResponseEntity.status(status)
                .header("Content-Type", "application/problem+json")
                .body(ProblemDetail.of(status, status.getReasonPhrase(), message, errorCode, request));
    }
}
