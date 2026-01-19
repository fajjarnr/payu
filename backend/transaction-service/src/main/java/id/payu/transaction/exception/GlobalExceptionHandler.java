package id.payu.transaction.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Global exception handler for Transaction Service.
 * Provides standardized error responses with error codes following TXN_xxx_xxx taxonomy.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handle domain-specific exceptions with proper error codes.
     */
    @ExceptionHandler(TransactionDomainException.class)
    public ResponseEntity<ErrorResponse> handleTransactionDomainException(TransactionDomainException ex) {
        HttpStatus status = determineHttpStatus(ex.getErrorCode());
        
        log.warn("Transaction domain exception: code={}, message={}", ex.getErrorCode(), ex.getMessage());
        
        ErrorResponse error = ErrorResponse.builder()
                .success(false)
                .error(ErrorDetail.builder()
                        .code(ex.getErrorCode())
                        .message(ex.getUserMessage())
                        .build())
                .meta(Meta.builder()
                        .requestId(UUID.randomUUID().toString())
                        .timestamp(Instant.now())
                        .build())
                .build();
        
        return ResponseEntity.status(status).body(error);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException ex) {
        log.warn("Bad request: {}", ex.getMessage());
        
        ErrorResponse error = ErrorResponse.builder()
                .success(false)
                .error(ErrorDetail.builder()
                        .code("TXN_VAL_999")
                        .message(ex.getMessage())
                        .build())
                .meta(Meta.builder()
                        .requestId(UUID.randomUUID().toString())
                        .timestamp(Instant.now())
                        .build())
                .build();
        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalStateException(IllegalStateException ex) {
        log.warn("Business error: {}", ex.getMessage());
        
        ErrorResponse error = ErrorResponse.builder()
                .success(false)
                .error(ErrorDetail.builder()
                        .code("TXN_BUS_999")
                        .message(ex.getMessage())
                        .build())
                .meta(Meta.builder()
                        .requestId(UUID.randomUUID().toString())
                        .timestamp(Instant.now())
                        .build())
                .build();
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(error);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException ex) {
        Map<String, Object> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        log.warn("Validation failed: {}", errors);
        
        ErrorResponse error = ErrorResponse.builder()
                .success(false)
                .error(ErrorDetail.builder()
                        .code("TXN_VAL_000")
                        .message("Validation failed")
                        .details(errors)
                        .build())
                .meta(Meta.builder()
                        .requestId(UUID.randomUUID().toString())
                        .timestamp(Instant.now())
                        .build())
                .build();
        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        log.error("Unexpected error occurred", ex);

        ErrorResponse error = ErrorResponse.builder()
                .success(false)
                .error(ErrorDetail.builder()
                        .code("TXN_SYS_999")
                        .message("Terjadi kesalahan sistem, silakan coba lagi")
                        .build())
                .meta(Meta.builder()
                        .requestId(UUID.randomUUID().toString())
                        .timestamp(Instant.now())
                        .build())
                .build();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

    /**
     * Determine HTTP status based on error code.
     * - VAL: 400 Bad Request
     * - BUS: 422 Unprocessable Entity
     * - BAL: 402 Payment Required (balance issues)
     * - EXT: 502 Bad Gateway (external service failure)
     * - SYS: 500 Internal Server Error
     */
    private HttpStatus determineHttpStatus(String errorCode) {
        if (errorCode.contains("_VAL_")) {
            return HttpStatus.BAD_REQUEST;
        } else if (errorCode.contains("_BUS_")) {
            return HttpStatus.UNPROCESSABLE_ENTITY;
        } else if (errorCode.contains("_BAL_")) {
            return HttpStatus.PAYMENT_REQUIRED;
        } else if (errorCode.contains("_EXT_")) {
            return HttpStatus.BAD_GATEWAY;
        } else if (errorCode.contains("_SYS_")) {
            return HttpStatus.INTERNAL_SERVER_ERROR;
        }
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }
}
