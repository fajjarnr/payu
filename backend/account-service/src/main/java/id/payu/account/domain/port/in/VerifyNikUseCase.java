package id.payu.account.domain.port.in;

import id.payu.account.dto.VerifyNikResponse;

import java.util.concurrent.CompletableFuture;

/**
 * Use case for NIK verification via Dukcapil.
 * This port defines the contract for NIK verification operations.
 */
public interface VerifyNikUseCase {

    /**
     * Verify NIK with Dukcapil simulator.
     * Returns verification status with minimal data for security.
     *
     * @param request the verification request containing NIK and personal data
     * @return CompletableFuture with verification result
     */
    CompletableFuture<VerifyNikResponse> verifyNik(id.payu.account.dto.VerifyNikRequest request);
}
