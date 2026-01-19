package id.payu.auth.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Global exception handler for Auth Service.
 * Handles domain exceptions and provides structured error responses.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handle AuthDomainException (all domain-specific errors)
     */
    @ExceptionHandler(AuthDomainException.class)
    public ResponseEntity<Map<String, Object>> handleDomainException(AuthDomainException ex) {
        HttpStatus status = determineHttpStatus(ex);
        
        log.warn("Auth domain error [{}]: {}", ex.getErrorCode(), ex.getMessage());
        
        return ResponseEntity.status(status)
                .body(buildErrorResponse(ex.getErrorCode(), ex.getUserMessage()));
    }

    /**
     * Handle validation exception specifically for BAD_REQUEST
     */
    @ExceptionHandler(AuthDomainException.InvalidPasswordException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidPassword(AuthDomainException.InvalidPasswordException ex) {
        log.warn("Password validation error [{}]: {}", ex.getErrorCode(), ex.getMessage());
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(buildErrorResponse(ex.getErrorCode(), ex.getUserMessage()));
    }

    /**
     * Handle IllegalArgumentException (backward compatibility)
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleBadRequests(IllegalArgumentException ex) {
        log.warn("Bad request: {}", ex.getMessage());
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(buildErrorResponse("AUTH_VAL_000", ex.getMessage()));
    }

    /**
     * Handle generic exceptions
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneral(Exception ex) {
        log.error("Unexpected error occurred", ex);
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(buildErrorResponse("AUTH_SYS_999", "Terjadi kesalahan sistem, silakan coba lagi"));
    }

    /**
     * Determine HTTP status based on exception type
     */
    private HttpStatus determineHttpStatus(AuthDomainException ex) {
        String errorCode = ex.getErrorCode();
        
        // VAL errors (4000-4099) -> 400 Bad Request
        if (errorCode.contains("_VAL_")) {
            return HttpStatus.BAD_REQUEST;
        }
        
        // BUS errors (4100-4199) -> varies
        if (errorCode.contains("_BUS_")) {
            // Account locked, rate limited -> 429 Too Many Requests
            if (ex instanceof AuthDomainException.AccountLockedException ||
                ex instanceof AuthDomainException.RateLimitExceededException) {
                return HttpStatus.TOO_MANY_REQUESTS;
            }
            // Invalid credentials, token expired -> 401 Unauthorized
            if (ex instanceof AuthDomainException.InvalidCredentialsException ||
                ex instanceof AuthDomainException.TokenExpiredException ||
                ex instanceof AuthDomainException.InvalidTokenException ||
                ex instanceof AuthDomainException.RefreshTokenExpiredException) {
                return HttpStatus.UNAUTHORIZED;
            }
            // User already exists -> 409 Conflict
            if (ex instanceof AuthDomainException.UserAlreadyExistsException) {
                return HttpStatus.CONFLICT;
            }
            // Account disabled -> 403 Forbidden
            if (ex instanceof AuthDomainException.AccountDisabledException) {
                return HttpStatus.FORBIDDEN;
            }
            return HttpStatus.BAD_REQUEST;
        }
        
        // EXT errors (4200-4299) -> 502 Bad Gateway or 503 Service Unavailable
        if (errorCode.contains("_EXT_")) {
            if (ex instanceof AuthDomainException.KeycloakUnavailableException) {
                return HttpStatus.SERVICE_UNAVAILABLE;
            }
            return HttpStatus.BAD_GATEWAY;
        }
        
        // SYS errors (4900-4999) -> 500 Internal Server Error
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }

    /**
     * Build standardized error response
     */
    private Map<String, Object> buildErrorResponse(String errorCode, String message) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", false);
        response.put("errorCode", errorCode);
        response.put("message", message);
        response.put("timestamp", Instant.now().toString());
        return response;
    }
}
