package id.payu.account.dto;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * PII-minimized user event (ACCOUNT-004): only stable identifiers travel in the
 * outbox payload. Consumers resolve PII through the account service when
 * authorized.
 */
public record UserCreatedEvent(
        UUID userId,
        String externalId,
        LocalDateTime createdAt) {
}
