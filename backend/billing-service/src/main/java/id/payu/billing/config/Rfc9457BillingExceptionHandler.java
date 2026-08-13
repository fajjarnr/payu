package id.payu.billing.config;

import id.payu.api.common.exception.problem.Rfc9457GlobalExceptionHandler;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@org.springframework.core.annotation.Order(0)
public class Rfc9457BillingExceptionHandler extends Rfc9457GlobalExceptionHandler {

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<id.payu.api.common.exception.problem.ProblemDetail> handleDataIntegrityViolation(
            DataIntegrityViolationException ex, HttpServletRequest request) {
        log.warn("Data integrity violation in {}: {}", request.getRequestURI(), ex.getMessage());
        return respondWith(HttpStatus.CONFLICT, "Conflict",
                "Resource already exists or constraint violated", "BIL_409", request);
    }
}
