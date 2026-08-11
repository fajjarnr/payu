package id.payu.account.dto;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Minimal registration response (ACCOUNT-004): never exposes email, phone,
 * fullName, NIK or any other PII back to the public registration endpoint.
 */
public record RegisterUserResponse(
        UUID userId,
        String externalId,
        String status,
        String kycStatus,
        LocalDateTime createdAt) {
}
