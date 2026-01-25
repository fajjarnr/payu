package id.payu.account.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Global exception handler for Account Service.
 * Provides standardized error responses with error codes.
 */

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Handle domain-specific exceptions with proper error codes.
     */
    @ExceptionHandler(AccountDomainException.class)
    public ResponseEntity<Map<String, Object>> handleAccountDomainException(AccountDomainException ex) {
        HttpStatus status = determineHttpStatus(ex.getErrorCode());
        
        log.warn("Domain exception: code={}, message={}", ex.getErrorCode(), ex.getMessage());
        
        return buildErrorResponse(status, ex.getErrorCode(), ex.getUserMessage(), null);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException ex) {
        log.warn("Bad request: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.BAD_REQUEST, "ACCT_VAL_999", ex.getMessage(), null);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalState(IllegalStateException ex) {
        log.warn("Conflict: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.CONFLICT, "ACCT_BUS_999", ex.getMessage(), null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
            errors.put(error.getField(), error.getDefaultMessage()));

        log.warn("Validation failed: {}", errors);
        return buildErrorResponse(HttpStatus.BAD_REQUEST, "ACCT_VAL_000", "Validation failed", errors);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneralError(Exception ex) {
        log.error("Unexpected error occurred", ex);
        return buildErrorResponse(
            HttpStatus.INTERNAL_SERVER_ERROR, 
            "ACCT_SYS_999", 
            "Terjadi kesalahan sistem, silakan coba lagi", 
            null
        );
    }

    private ResponseEntity<Map<String, Object>> buildErrorResponse(
            HttpStatus status, 
            String errorCode, 
            String message, 
            Object details) {
        
        Map<String, Object> body = new HashMap<>();
        body.put("success", false);
        body.put("error", Map.of(
            "code", errorCode,
            "message", message
        ));
        body.put("meta", Map.of(
            "requestId", UUID.randomUUID().toString(),
            "timestamp", Instant.now().toString()
        ));
        
        if (details != null) {
            body.put("details", details);
        }
        
        return new ResponseEntity<>(body, status);
    }

    /**
     * Determine HTTP status based on error code.
     * - VAL: 400 Bad Request
     * - BUS: 422 Unprocessable Entity
     * - EXT: 502 Bad Gateway (external service failure)
     * - SYS: 500 Internal Server Error
     */
    private HttpStatus determineHttpStatus(String errorCode) {
        if (errorCode.contains("_VAL_")) {
            return HttpStatus.BAD_REQUEST;
        } else if (errorCode.contains("_BUS_")) {
            return HttpStatus.UNPROCESSABLE_ENTITY;
        } else if (errorCode.contains("_EXT_")) {
            return HttpStatus.BAD_GATEWAY;
        } else if (errorCode.contains("_SYS_")) {
            return HttpStatus.INTERNAL_SERVER_ERROR;
        }
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }
}
