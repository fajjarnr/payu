package id.payu.account.domain.port.in;

import id.payu.account.interfaces.dto.VerifyNikResponse;

import java.util.concurrent.CompletableFuture;

/**
 * Use case for NIK verification via Dukcapil.
 * This port defines the contract for NIK verification operations.
 */
public interface VerifyNikUseCase {

    /**
     * Verify NIK with Dukcapil simulator.
     * Returns verification status with minimal data for security.
     */
    CompletableFuture<VerifyNikResponse> verifyNik(id.payu.account.interfaces.dto.VerifyNikRequest request);
}
