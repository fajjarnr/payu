package id.payu.integration.adapter.web.exception;

import id.payu.api.common.exception.problem.Rfc9457GlobalExceptionHandler;
import id.payu.integration.application.service.IntegrationService.MessageNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@RestControllerAdvice
@org.springframework.core.annotation.Order(0)
public class Rfc9457IntegrationExceptionHandler extends Rfc9457GlobalExceptionHandler {

    @ExceptionHandler(MessageNotFoundException.class)
    public ResponseEntity<id.payu.api.common.exception.problem.ProblemDetail> handleMessageNotFound(
            MessageNotFoundException ex, HttpServletRequest request) {
        log.info("Message not found in {}: {}", request.getRequestURI(), ex.getMessage());
        return respondWith(HttpStatus.NOT_FOUND, "Resource not found", ex.getMessage(), "INT_404", request);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<id.payu.api.common.exception.problem.ProblemDetail> handleDataIntegrityViolation(
            DataIntegrityViolationException ex, HttpServletRequest request) {
        log.warn("Data integrity violation in {}: {}", request.getRequestURI(), ex.getMessage());
        return respondWith(HttpStatus.CONFLICT, "Conflict",
                "Resource already exists or constraint violated", "INT_409", request);
    }

    @Override
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<id.payu.api.common.exception.problem.ProblemDetail> handleAccessDenied(
            AccessDeniedException ex, HttpServletRequest request) {
        log.warn("Access denied in {}: {}", request.getRequestURI(), ex.getMessage());
        return respondWith(HttpStatus.FORBIDDEN, "Forbidden", "Insufficient permissions", "INT_403", request);
    }

    @Override
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<id.payu.api.common.exception.problem.ProblemDetail> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        String errors = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .collect(Collectors.joining(", "));
        log.warn("Validation failed: {}", errors);
        return respondWith(HttpStatus.BAD_REQUEST, "Bad request", errors, "INT_400", request);
    }

    @Override
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<id.payu.api.common.exception.problem.ProblemDetail> handleConstraintViolation(
            ConstraintViolationException ex, HttpServletRequest request) {
        log.warn("Constraint violation: {}", ex.getMessage());
        return respondWith(HttpStatus.BAD_REQUEST, "Bad request", ex.getMessage(), "INT_400", request);
    }

    @Override
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<id.payu.api.common.exception.problem.ProblemDetail> handleMessageNotReadable(
            HttpMessageNotReadableException ex, HttpServletRequest request) {
        String message = ex.getMostSpecificCause() != null
                ? ex.getMostSpecificCause().getMessage()
                : "Malformed JSON request";
        log.warn("Malformed request body: {}", message);
        return respondWith(HttpStatus.BAD_REQUEST, "Bad request", message, "INT_400", request);
    }

    @Override
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<id.payu.api.common.exception.problem.ProblemDetail> handleIllegalArgumentException(
            IllegalArgumentException ex, HttpServletRequest request) {
        log.warn("Invalid argument: {}", ex.getMessage());
        return respondWith(HttpStatus.BAD_REQUEST, "Bad request", ex.getMessage(), "INT_400", request);
    }

    @Override
    @ExceptionHandler(Exception.class)
    public ResponseEntity<id.payu.api.common.exception.problem.ProblemDetail> handleGenericException(
            Exception ex, HttpServletRequest request) {
        log.error("Unhandled exception in integration-service: {} - {}", ex.getClass().getSimpleName(),
                ex.getMessage() != null ? ex.getMessage() : "no message");
        return respondWith(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error",
                "An unexpected error occurred", "INT_500", request);
    }
}
