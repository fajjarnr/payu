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

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException ex) {
        ErrorResponse error = ErrorResponse.builder()
                .success(false)
                .error(ErrorDetail.builder()
                        .code("VALIDATION_ERROR")
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
        ErrorResponse error = ErrorResponse.builder()
                .success(false)
                .error(ErrorDetail.builder()
                        .code("BUSINESS_ERROR")
                        .message(ex.getMessage())
                        .build())
                .meta(Meta.builder()
                        .requestId(UUID.randomUUID().toString())
                        .timestamp(Instant.now())
                        .build())
                .build();
        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException ex) {
        Map<String, Object> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        ErrorResponse error = ErrorResponse.builder()
                .success(false)
                .error(ErrorDetail.builder()
                        .code("VALIDATION_ERROR")
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
                        .code("INTERNAL_SERVER_ERROR")
                        .message("An unexpected error occurred")
                        .build())
                .meta(Meta.builder()
                        .requestId(UUID.randomUUID().toString())
                        .timestamp(Instant.now())
                        .build())
                .build();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}
