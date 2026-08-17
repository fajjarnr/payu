package id.payu.account.domain.port.out;

import id.payu.account.interfaces.dto.VerifyNikResponse;

/**
 * Port for KYC verification operations with external services (Dukcapil).
 * This is an output port (secondary port) for external integrations.
 */
public interface KycVerificationPort {

    /**
     * Verify NIK with Dukcapil (legacy method for backward compatibility).
     *
     * @param nik the 16-digit NIK
     * @param fullName the full name to match
     * @return verification response with minimal data
     * @deprecated Use {@link #verifyNik(id.payu.account.interfaces.dto.VerifyNikRequest)} instead
     */
    @Deprecated
    id.payu.account.interfaces.dto.DukcapilResponse verifyNik(String nik, String fullName);

    /**
     * Verify NIK with Dukcapil using full request data.
     * This method provides enhanced verification with birth data matching.
     *
     * @param request the verification request with NIK and personal data
     * @return detailed verification response
     */
    VerifyNikResponse verifyNik(id.payu.account.interfaces.dto.VerifyNikRequest request);
}
