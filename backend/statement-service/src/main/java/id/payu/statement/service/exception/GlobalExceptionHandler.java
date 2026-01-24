package id.payu.statement.service.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Global exception handler for Statement service
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(StatementException.class)
    public ResponseEntity<Object> handleStatementException(
            StatementException ex, WebRequest request) {

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("error_code", ex.getErrorCode());
        body.put("message", getIndonesianMessage(ex.getErrorCode()));
        body.put("path", request.getDescription(false).replace("uri=", ""));

        return new ResponseEntity<>(body, getStatusFromErrorCode(ex.getErrorCode()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleGenericException(
            Exception ex, WebRequest request) {

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("error_code", "STATEMENT_999");
        body.put("message", "Terjadi kesalahan sistem. Silakan coba lagi.");
        body.put("path", request.getDescription(false).replace("uri=", ""));

        return new ResponseEntity<>(body, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private String getIndonesianMessage(String errorCode) {
        return switch (errorCode) {
            case StatementException.STATEMENT_NOT_FOUND -> "Tagihan tidak ditemukan";
            case StatementException.STATEMENT_NOT_READY -> "Tagihan sedang diproses, silakan coba lagi nanti";
            case StatementException.STATEMENT_READ_FAILED -> "Gagal membaca file tagihan";
            case StatementException.STATEMENT_GENERATION_FAILED -> "Gagal membuat tagihan";
            default -> "Terjadi kesalahan pada layanan tagihan";
        };
    }

    private HttpStatus getStatusFromErrorCode(String errorCode) {
        return switch (errorCode) {
            case StatementException.STATEMENT_NOT_FOUND -> HttpStatus.NOT_FOUND;
            case StatementException.STATEMENT_NOT_READY -> HttpStatus.ACCEPTED;
            case StatementException.STATEMENT_READ_FAILED -> HttpStatus.INTERNAL_SERVER_ERROR;
            case StatementException.STATEMENT_GENERATION_FAILED -> HttpStatus.INTERNAL_SERVER_ERROR;
            default -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }
}
