package id.payu.api.common.exception;

import id.payu.api.common.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Global exception handler for all PayU API endpoints.
 * Converts exceptions into standardized ApiResponse format with appropriate HTTP status codes.
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * Handles business exceptions with custom error codes.
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(
            BusinessException ex,
            HttpServletRequest request
    ) {
        log.warn("Business exception in {}: {}", request.getRequestURI(), ex.getMessage());

        HttpStatus status = determineHttpStatus(ex);
        ApiResponse<Void> response = ApiResponse.error(ex.getCode(), ex.getMessage());

        return ResponseEntity.status(status).body(response);
    }

    /**
     * Handles validation exceptions from @Valid annotation with field errors.
     */
    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(
            ValidationException ex,
            HttpServletRequest request
    ) {
        log.warn("Validation exception in {}: {}", request.getRequestURI(), ex.getMessage());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ex.getCode(), ex.getMessage(), ex.getFieldErrors()));
    }

    /**
     * Handles bean validation errors from @Valid annotation.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            HttpServletRequest request
    ) {
        log.warn("Validation failed for {}: {}", request.getRequestURI(), ex.getMessage());

        List<id.payu.api.common.response.FieldError> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> id.payu.api.common.response.FieldError.builder()
                        .field(error.getField())
                        .message(error.getDefaultMessage())
                        .rejectedValue(error.getRejectedValue())
                        .build())
                .collect(Collectors.toList());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("VALIDATION_ERROR", "Request validation failed", fieldErrors));
    }

    /**
     * Handles constraint violations from @Validated.
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolation(
            ConstraintViolationException ex,
            HttpServletRequest request
    ) {
        log.warn("Constraint violation in {}: {}", request.getRequestURI(), ex.getMessage());

        List<id.payu.api.common.response.FieldError> fieldErrors = ex.getConstraintViolations().stream()
                .map(this::convertToFieldError)
                .collect(Collectors.toList());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("VALIDATION_ERROR", "Request validation failed", fieldErrors));
    }

    /**
     * Handles missing request parameters.
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingParameter(
            MissingServletRequestParameterException ex,
            HttpServletRequest request
    ) {
        log.warn("Missing parameter in {}: {}", request.getRequestURI(), ex.getParameterName());

        String message = String.format("Required parameter '%s' is missing", ex.getParameterName());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("MISSING_PARAMETER", message));
    }

    /**
     * Handles type mismatch in request parameters.
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex,
            HttpServletRequest request
    ) {
        log.warn("Type mismatch for parameter in {}: {}", request.getRequestURI(), ex.getName());

        String message = String.format("Parameter '%s' has invalid value '%s'",
                ex.getName(), ex.getValue());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("INVALID_PARAMETER", message));
    }

    /**
     * Handles malformed JSON requests.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleMessageNotReadable(
            HttpMessageNotReadableException ex,
            HttpServletRequest request
    ) {
        log.warn("Malformed request in {}: {}", request.getRequestURI(), ex.getMessage());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("INVALID_JSON", "Request body is malformed or unreadable"));
    }

    /**
     * Handles resource not found (404).
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFound(
            NoHandlerFoundException ex,
            HttpServletRequest request
    ) {
        log.warn("Resource not found: {}", ex.getRequestURL());

        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error("NOT_FOUND", "The requested resource was not found"));
    }

    /**
     * Handles all other unexpected exceptions.
     *
     * SECURITY: Stack traces are NOT logged to prevent information leakage.
     * Full exception details should be logged to a secure logging system.
     * For production, integrate with a centralized logging system (e.g., Loki, ELK).
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGenericException(
            Exception ex,
            HttpServletRequest request
    ) {
        // SECURITY: Only log the exception type and message, NOT the stack trace
        // This prevents potential information disclosure in logs
        log.error("Unexpected error in {}: {} - {}",
                request.getRequestURI(),
                ex.getClass().getSimpleName(),
                getSafeErrorMessage(ex));

        // For security auditing, log a correlation ID without sensitive details
        String correlationId = request.getHeader("X-Correlation-ID");
        if (correlationId == null) {
            correlationId = java.util.UUID.randomUUID().toString();
        }
        log.info("Error correlation ID: {} for URI: {}", correlationId, request.getRequestURI());

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("INTERNAL_ERROR", "An unexpected error occurred. Please try again later."));
    }

    /**
     * Extracts a safe error message that doesn't contain sensitive information.
     *
     * @param ex The exception
     * @return A safe error message
     */
    private String getSafeErrorMessage(Exception ex) {
        String message = ex.getMessage();

        // Filter out potentially sensitive information
        if (message == null) {
            return "No message";
        }

        // Remove file paths, passwords, tokens, etc.
        return message
                .replaceAll("password[^,]*", "password=***")
                .replaceAll("secret[^,]*", "secret=***")
                .replaceAll("token[^,]*", "token=***")
                .replaceAll("/[a-zA-Z0-9_/\\-]+", "[path]")  // Mask file paths
                .replaceAll("('[^']*')", "[value]");  // Mask quoted strings
    }

    /**
     * Determines the appropriate HTTP status code based on exception type and error code.
     */
    private HttpStatus determineHttpStatus(BusinessException ex) {
        String code = ex.getCode();

        // Check exception type first
        if (ex instanceof ResourceNotFoundException) {
            return HttpStatus.NOT_FOUND;
        } else if (ex instanceof ConflictException) {
            return HttpStatus.CONFLICT;
        } else if (ex instanceof RateLimitExceededException) {
            return HttpStatus.TOO_MANY_REQUESTS;
        } else if (ex instanceof ExternalServiceException) {
            return HttpStatus.BAD_GATEWAY;
        } else if (ex instanceof ValidationException) {
            return HttpStatus.BAD_REQUEST;
        }

        // Check error code pattern
        if (code.contains("_VAL_") || code.contains("VALIDATION")) {
            return HttpStatus.BAD_REQUEST;
        } else if (code.contains("_BUS_")) {
            return HttpStatus.UNPROCESSABLE_ENTITY;
        } else if (code.contains("_EXT_")) {
            return HttpStatus.BAD_GATEWAY;
        } else if (code.contains("NOT_FOUND")) {
            return HttpStatus.NOT_FOUND;
        } else if (code.contains("FORBIDDEN")) {
            return HttpStatus.FORBIDDEN;
        } else if (code.contains("UNAUTHORIZED")) {
            return HttpStatus.UNAUTHORIZED;
        }

        return HttpStatus.INTERNAL_SERVER_ERROR;
    }

    /**
     * Converts ConstraintViolation to FieldError.
     */
    private id.payu.api.common.response.FieldError convertToFieldError(ConstraintViolation<?> violation) {
        String field = violation.getPropertyPath() != null
                ? violation.getPropertyPath().toString()
                : "unknown";

        // Remove class name from property path if present
        if (field.contains(".")) {
            field = field.substring(field.lastIndexOf('.') + 1);
        }

        return id.payu.api.common.response.FieldError.builder()
                .field(field)
                .message(violation.getMessage())
                .rejectedValue(violation.getInvalidValue())
                .build();
    }
}
