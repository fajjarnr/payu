package id.payu.account.adapter.client;

import id.payu.account.domain.port.out.KycVerificationPort;
import id.payu.account.interfaces.dto.DukcapilResponse;
import id.payu.account.interfaces.dto.VerifyNikRequest;
import id.payu.account.interfaces.dto.VerifyNikResponse;
import id.payu.account.exception.AccountDomainException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Adapter for KYC verification with Dukcapil simulator.
 * Implements circuit breaker, retry, and caching patterns.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class KycVerificationAdapter implements KycVerificationPort {

    private final GatewayClient gatewayClient;

    @Override
    @Deprecated
    public DukcapilResponse verifyNik(String nik, String fullName) {
        VerifyNikRequest request = new VerifyNikRequest(
            nik,
            fullName,
            "UNKNOWN",
            "2000-01-01"
        );

        VerifyNikResponse response = gatewayClient.verifyNik(request);

        // Convert VerifyNikResponse to DukcapilResponse for backward compatibility
        return new DukcapilResponse(
            response.requestId(),
            response.nik(),
            response.verified(),
            response.status(),
            response.responseCode(),
            response.responseMessage()
        );
    }

    @Override
    @CircuitBreaker(name = "dukcapilService", fallbackMethod = "verifyNikFallback")
    @Retry(name = "dukcapilService")
    @Cacheable(value = "nikVerification", key = "#request.nik() + ':' + #request.fullName().trim().toLowerCase()", unless = "#result == null")
    public VerifyNikResponse verifyNik(VerifyNikRequest request) {
        String requestId = UUID.randomUUID().toString();
        String maskedNik = maskNik(request.nik());

        log.info("NIK verification request [requestId={}, nik={}]", requestId, maskedNik);

        try {
            VerifyNikResponse response = gatewayClient.verifyNik(request);
            log.info("NIK verification completed [requestId={}, verified={}]", requestId, response.verified());
            return response;
        } catch (Exception e) {
            log.error("NIK verification failed [requestId={}, error={}]", requestId, e.getMessage());
            throw new AccountDomainException.DukcapilVerificationFailedException(
                "Failed to verify NIK: " + e.getMessage(),
                e
            );
        }
    }

    /**
     * Fallback method when Dukcapil service is unavailable.
     */
    private VerifyNikResponse verifyNikFallback(VerifyNikRequest request, Throwable throwable) {
        String requestId = UUID.randomUUID().toString();
        String maskedNik = maskNik(request.nik());

        log.warn("Dukcapil service unavailable, using fallback [requestId={}, nik={}, error={}]",
            requestId, maskedNik, throwable.getMessage());

        throw new AccountDomainException.DukcapilServiceUnavailableException(throwable);
    }

    /**
     * Mask NIK for logging (show first 4 and last 4 digits).
     */
    private String maskNik(String nik) {
        if (nik == null || nik.length() < 8) {
            return "****";
        }
        return nik.substring(0, 4) + "******" + nik.substring(nik.length() - 4);
    }
}
