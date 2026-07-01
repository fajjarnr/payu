package id.payu.dispute.config;

import id.payu.api.common.exception.problem.Rfc9457GlobalExceptionHandler;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@RestControllerAdvice
@org.springframework.core.annotation.Order(0)
public class Rfc9457DisputeExceptionHandler extends Rfc9457GlobalExceptionHandler {

    @Override
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<id.payu.api.common.exception.problem.ProblemDetail> handleAccessDenied(
            AccessDeniedException ex, HttpServletRequest request) {
        log.warn("Access denied in {}: {}", request.getRequestURI(), ex.getMessage());
        return respondWith(HttpStatus.FORBIDDEN, "Forbidden", "Insufficient permissions", "DISP_403", request);
    }

    @Override
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<id.payu.api.common.exception.problem.ProblemDetail> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        String errors = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .collect(Collectors.joining(", "));
        log.warn("Validation failed: {}", errors);
        return respondWith(HttpStatus.BAD_REQUEST, "Bad request", errors, "DISP_400", request);
    }

    @Override
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<id.payu.api.common.exception.problem.ProblemDetail> handleConstraintViolation(
            ConstraintViolationException ex, HttpServletRequest request) {
        log.warn("Constraint violation: {}", ex.getMessage());
        return respondWith(HttpStatus.BAD_REQUEST, "Bad request", ex.getMessage(), "DISP_400", request);
    }

    @Override
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<id.payu.api.common.exception.problem.ProblemDetail> handleIllegalArgumentException(
            IllegalArgumentException ex, HttpServletRequest request) {
        log.warn("Invalid argument: {}", ex.getMessage());
        return respondWith(HttpStatus.BAD_REQUEST, "Bad request", ex.getMessage(), "DISP_400", request);
    }

    @Override
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<id.payu.api.common.exception.problem.ProblemDetail> handleIllegalStateException(
            IllegalStateException ex, HttpServletRequest request) {
        log.warn("Illegal state: {}", ex.getMessage());
        return respondWith(HttpStatus.CONFLICT, "Conflict", ex.getMessage(), "DISP_409", request);
    }

    @Override
    @ExceptionHandler(Exception.class)
    public ResponseEntity<id.payu.api.common.exception.problem.ProblemDetail> handleGenericException(
            Exception ex, HttpServletRequest request) {
        log.error("Unhandled exception in dispute-service: {} - {}", ex.getClass().getSimpleName(),
                ex.getMessage() != null ? ex.getMessage() : "no message");
        return respondWith(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error",
                "An unexpected error occurred", "DISP_500", request);
    }
}
