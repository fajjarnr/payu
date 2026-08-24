package id.payu.transaction.config;

import id.payu.api.common.exception.problem.ProblemDetail;
import id.payu.api.common.exception.problem.Rfc9457GlobalExceptionHandler;
import id.payu.transaction.exception.TransactionDomainException.AmlHighRiskBlockedException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * ITER-56 (READY-024): RFC 9457-compliant exception handler for transaction-service.
 *
 * <p>Extends the shared {@link Rfc9457GlobalExceptionHandler} to opt into
 * {@code application/problem+json} response format per RFC 9457 + PayU extensions
 * (error_code, trace_id, timestamp).
 *
 * <p>Replaces the legacy {@code GlobalExceptionHandler} that produced PayU's
 * {@code ApiResponse}-wrapped errors. Both handlers are kept temporarily to
 * avoid breaking existing clients; can be removed in a future iter.
 */
@RestControllerAdvice
@org.springframework.core.annotation.Order(0)
public class Rfc9457TransactionExceptionHandler extends Rfc9457GlobalExceptionHandler {

    /**
     * ADR-0030: fraud risk score > 85 (CRITICAL_RISK) — auto-blocked transfer maps
     * to 403 Forbidden with the ADR-specified error code.
     */
    @ExceptionHandler(AmlHighRiskBlockedException.class)
    public ResponseEntity<ProblemDetail> handleAmlHighRiskBlocked(
            AmlHighRiskBlockedException ex, HttpServletRequest request) {
        return respondWith(HttpStatus.FORBIDDEN, "Forbidden", ex.getMessage(), ex.getCode(), request);
    }
}
