package id.payu.api.common.exception.problem;

import id.payu.api.common.exception.BusinessException;
import id.payu.api.common.exception.ConflictException;
import id.payu.api.common.exception.ExternalServiceException;
import id.payu.api.common.exception.InsufficientFundsException;
import id.payu.api.common.exception.RateLimitExceededException;
import id.payu.api.common.exception.ResourceNotFoundException;
import id.payu.api.common.exception.ServiceUnavailableException;
import id.payu.api.common.exception.ValidationException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.util.List;
import java.util.stream.Collectors;

/**
 * RFC 9457-compliant global exception handler.
 *
 * <p>Subclasses {@code @RestControllerAdvice} services can extend this class
 * to opt into RFC 9457 Problem Details ({@code application/problem+json})
 * response format. PayU extensions (error_code, trace_id, timestamp) are included.
 *
 * <p>To use: simply remove your service's custom {@code GlobalExceptionHandler}
 * and extend this class. All standard Spring exceptions are handled here.
 *
 * <p>Subclasses can override {@link #handleBusinessException} to add service-specific
 * mappings while preserving the RFC 9457 envelope.
 *
 * @see <a href="https://www.rfc-editor.org/rfc/rfc9457.html">RFC 9457</a>
 * @see ProblemDetail
 * @since 1.8.69
 */
@RestControllerAdvice
public class Rfc9457GlobalExceptionHandler {

    protected static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(Rfc9457GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ProblemDetail> handleBusinessException(
            BusinessException ex, HttpServletRequest request) {
        log.warn("Business exception in {}: code={} message={}",
                request.getRequestURI(), ex.getCode(), ex.getMessage());
        HttpStatus status = determineHttpStatus(ex);
        return respondWith(status, titleFor(status), ex.getMessage(), ex.getCode(), request);
    }

    @ExceptionHandler(InsufficientFundsException.class)
    public ResponseEntity<ProblemDetail> handleInsufficientFunds(
            InsufficientFundsException ex, HttpServletRequest request) {
        log.warn("Insufficient funds in {}: {}", request.getRequestURI(), ex.getMessage());
        return respondWith(HttpStatus.UNPROCESSABLE_ENTITY, "Insufficient funds",
                ex.getMessage(), ex.getCode(), request);
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ProblemDetail> handleValidationException(
            ValidationException ex, HttpServletRequest request) {
        log.warn("Validation exception in {}: {}", request.getRequestURI(), ex.getMessage());
        List<FieldViolation> violations = ex.getFieldErrors() != null
                ? ex.getFieldErrors().stream()
                    .map(fe -> FieldViolation.builder()
                            .field(fe.getField())
                            .message(fe.getMessage())
                            .rejectedValue(fe.getRejectedValue())
                            .build())
                    .collect(Collectors.toList())
                : null;
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(ProblemDetail.of(HttpStatus.BAD_REQUEST, "Validation failed",
                        ex.getMessage(), ex.getCode(), request, violations));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        log.warn("Bean validation failed for {}: {}", request.getRequestURI(), ex.getMessage());
        List<FieldViolation> violations = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> FieldViolation.builder()
                        .field(error.getField())
                        .message(error.getDefaultMessage())
                        .rejectedValue(error.getRejectedValue())
                        .code(error.getCode())
                        .build())
                .collect(Collectors.toList());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(ProblemDetail.of(HttpStatus.BAD_REQUEST, "Validation failed",
                        "Request body failed validation", "VALIDATION_ERROR",
                        request, violations));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ProblemDetail> handleConstraintViolation(
            ConstraintViolationException ex, HttpServletRequest request) {
        log.warn("Constraint violation in {}: {}", request.getRequestURI(), ex.getMessage());
        List<FieldViolation> violations = ex.getConstraintViolations().stream()
                .map(this::convertToFieldViolation)
                .collect(Collectors.toList());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(ProblemDetail.of(HttpStatus.BAD_REQUEST, "Validation failed",
                        "Request parameters failed validation", "VALIDATION_ERROR",
                        request, violations));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ProblemDetail> handleIllegalArgumentException(
            IllegalArgumentException ex, HttpServletRequest request) {
        log.warn("Illegal argument in {}: {}", request.getRequestURI(), ex.getMessage());
        return respondWith(HttpStatus.BAD_REQUEST, "Invalid argument",
                ex.getMessage(), "INVALID_ARGUMENT", request);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ProblemDetail> handleIllegalStateException(
            IllegalStateException ex, HttpServletRequest request) {
        log.warn("Illegal state in {}: {}", request.getRequestURI(), ex.getMessage());
        return respondWith(HttpStatus.CONFLICT, "Invalid state",
                ex.getMessage(), "INVALID_STATE", request);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ProblemDetail> handleMissingParameter(
            MissingServletRequestParameterException ex, HttpServletRequest request) {
        log.warn("Missing parameter in {}: {}", request.getRequestURI(), ex.getParameterName());
        String detail = String.format("Required parameter '%s' is missing", ex.getParameterName());
        return respondWith(HttpStatus.BAD_REQUEST, "Missing parameter", detail,
                "MISSING_PARAMETER", request);
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ProblemDetail> handleMissingRequestHeader(
            MissingRequestHeaderException ex, HttpServletRequest request) {
        log.warn("Missing required header in {}: {}", request.getRequestURI(), ex.getHeaderName());
        String detail = String.format("Required header '%s' is missing", ex.getHeaderName());
        return respondWith(HttpStatus.BAD_REQUEST, "Missing required header", detail,
                "MISSING_REQUIRED_HEADER", request);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ProblemDetail> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        log.warn("Type mismatch for parameter in {}: {}", request.getRequestURI(), ex.getName());
        String detail = String.format("Parameter '%s' has invalid value '%s'",
                ex.getName(), ex.getValue());
        return respondWith(HttpStatus.BAD_REQUEST, "Invalid parameter type", detail,
                "INVALID_PARAMETER", request);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ProblemDetail> handleMessageNotReadable(
            HttpMessageNotReadableException ex, HttpServletRequest request) {
        log.warn("Malformed request in {}: {}", request.getRequestURI(), ex.getMessage());
        return respondWith(HttpStatus.BAD_REQUEST, "Malformed request",
                "Request body is malformed or unreadable", "INVALID_JSON", request);
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ProblemDetail> handleNotFound(
            NoHandlerFoundException ex, HttpServletRequest request) {
        log.warn("Resource not found: {}", ex.getRequestURL());
        return respondWith(HttpStatus.NOT_FOUND, "Resource not found",
                "The requested resource was not found", "NOT_FOUND", request);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ProblemDetail> handleMethodNotSupported(
            HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {
        log.info("Method not allowed for {}: requested={} allowed={}",
                request.getRequestURI(), ex.getMethod(), ex.getSupportedHttpMethods());
        String supportedMethods = ex.getSupportedHttpMethods() != null
                ? ex.getSupportedHttpMethods().stream().map(Object::toString)
                        .collect(Collectors.joining(", "))
                : "unknown";
        String detail = String.format("Method %s not allowed. Supported: %s",
                ex.getMethod(), supportedMethods);
        return respondWith(HttpStatus.METHOD_NOT_ALLOWED, "Method not allowed",
                detail, "METHOD_NOT_ALLOWED", request);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ProblemDetail> handleAccessDenied(
            AccessDeniedException ex, HttpServletRequest request) {
        log.warn("Access denied in {}: {}", request.getRequestURI(), ex.getMessage());
        return respondWith(HttpStatus.FORBIDDEN, "Forbidden",
                "Insufficient permissions", "ACCESS_DENIED", request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleGenericException(
            Exception ex, HttpServletRequest request) {
        // SECURITY: Only log exception type + message, not stack trace
        log.error("Unexpected error in {}: {} - {}",
                request.getRequestURI(),
                ex.getClass().getSimpleName(),
                ex.getMessage() != null ? ex.getMessage() : "no message");
        return respondWith(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error",
                "An unexpected error occurred. Please try again later.",
                "INTERNAL_ERROR", request);
    }

    // --- helpers ---

    protected ResponseEntity<ProblemDetail> respondWith(HttpStatus status, String title, String detail,
                                                        String errorCode, HttpServletRequest request) {
        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(ProblemDetail.of(status, title, detail, errorCode, request));
    }

    private String titleFor(HttpStatus status) {
        if (status == null) return "Error";
        return switch (status) {
            case BAD_REQUEST -> "Bad request";
            case UNAUTHORIZED -> "Unauthorized";
            case FORBIDDEN -> "Forbidden";
            case NOT_FOUND -> "Resource not found";
            case CONFLICT -> "Conflict";
            case UNPROCESSABLE_ENTITY -> "Unprocessable entity";
            case TOO_MANY_REQUESTS -> "Too many requests";
            case BAD_GATEWAY -> "Bad gateway";
            case SERVICE_UNAVAILABLE -> "Service unavailable";
            default -> status.getReasonPhrase() != null ? status.getReasonPhrase() : "Error";
        };
    }

    private HttpStatus determineHttpStatus(BusinessException ex) {
        if (ex instanceof ResourceNotFoundException) return HttpStatus.NOT_FOUND;
        if (ex instanceof ConflictException) return HttpStatus.CONFLICT;
        if (ex instanceof RateLimitExceededException) return HttpStatus.TOO_MANY_REQUESTS;
        if (ex instanceof ServiceUnavailableException) return HttpStatus.SERVICE_UNAVAILABLE;
        if (ex instanceof ExternalServiceException) return HttpStatus.BAD_GATEWAY;
        if (ex instanceof ValidationException) return HttpStatus.BAD_REQUEST;
        if (ex instanceof InsufficientFundsException) return HttpStatus.UNPROCESSABLE_ENTITY;

        String code = ex.getCode();
        if (code == null) return HttpStatus.INTERNAL_SERVER_ERROR;
        if (code.contains("_VAL_") || code.contains("VALIDATION")) return HttpStatus.BAD_REQUEST;
        if (code.contains("_BUS_")) return HttpStatus.UNPROCESSABLE_ENTITY;
        if (code.contains("_EXT_")) return HttpStatus.BAD_GATEWAY;
        if (code.contains("NOT_FOUND") || code.endsWith("_404")) return HttpStatus.NOT_FOUND;
        if (code.contains("FORBIDDEN")) return HttpStatus.FORBIDDEN;
        if (code.contains("UNAUTHORIZED")) return HttpStatus.UNAUTHORIZED;
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }

    private FieldViolation convertToFieldViolation(ConstraintViolation<?> violation) {
        String field = violation.getPropertyPath() != null
                ? violation.getPropertyPath().toString()
                : "unknown";
        if (field.contains(".")) {
            field = field.substring(field.lastIndexOf('.') + 1);
        }
        return FieldViolation.builder()
                .field(field)
                .message(violation.getMessage())
                .rejectedValue(violation.getInvalidValue())
                .build();
    }
}
