package id.payu.account.application.service;

import id.payu.account.domain.port.in.VerifyNikUseCase;
import id.payu.account.domain.port.out.KycVerificationPort;
import id.payu.account.dto.VerifyNikRequest;
import id.payu.account.dto.VerifyNikResponse;
import id.payu.account.exception.AccountDomainException;
import id.payu.security.annotation.Audited;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import id.payu.security.annotation.AuditOperation;

/**
 * Application service for NIK verification operations.
 * Implements the VerifyNikUseCase port with resilience patterns.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NikVerificationService implements VerifyNikUseCase {

    private final KycVerificationPort kycVerificationPort;

    @Override
    @Async
    @CircuitBreaker(name = "dukcapilService", fallbackMethod = "verifyNikFallback")
    @Retry(name = "dukcapilService")
    @TimeLimiter(name = "dukcapilService")
    @Bulkhead(name = "dukcapilService", fallbackMethod = "verifyNikFallback")
    @Audited(
        operation = AuditOperation.READ,
        entityType = "NikVerification",
        maskData = true
    )
    public CompletableFuture<VerifyNikResponse> verifyNik(VerifyNikRequest request) {
        log.info("Processing NIK verification for: ********{}",
            request.nik() != null && request.nik().length() >= 4 ? request.nik().substring(request.nik().length() - 4) : "");

        // Validate NIK format
        validateNikFormat(request.nik());

        // Call the adapter
        VerifyNikResponse response = kycVerificationPort.verifyNik(request);

        log.info("NIK verification completed with status: {}, verified: {}",
            response.status(), response.verified());

        return CompletableFuture.completedFuture(response);
    }

    /**
     * Fallback method when Dukcapil service is unavailable.
     */
    private CompletableFuture<VerifyNikResponse> verifyNikFallback(VerifyNikRequest request, Throwable throwable) {
        log.error("Dukcapil service unavailable during NIK verification: {}", throwable.getMessage());

        // Return a service unavailable response instead of throwing exception
        VerifyNikResponse fallbackResponse = VerifyNikResponse.serviceUnavailable(
            java.util.UUID.randomUUID().toString()
        );

        return CompletableFuture.completedFuture(fallbackResponse);
    }

    /**
     * Validate NIK format before sending to external service.
     */
    private void validateNikFormat(String nik) {
        if (nik == null || nik.isEmpty()) {
            throw new AccountDomainException.InvalidNikException("NIK cannot be empty");
        }

        if (!nik.matches("^[0-9]{16}$")) {
            throw new AccountDomainException.InvalidNikException(nik);
        }

        // Additional Luhn-like checksum validation for Indonesian NIK
        if (!isValidNikChecksum(nik)) {
            throw new AccountDomainException.InvalidNikException(nik);
        }
    }

    /**
     * Basic checksum validation for NIK.
     * This is a simplified version - real NIK validation is more complex.
     */
    private boolean isValidNikChecksum(String nik) {
        // Basic format check - NIK should not be all same digits
        return !nik.matches("(\\d)\\1{15}");
    }
}
