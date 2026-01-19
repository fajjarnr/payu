package id.payu.account.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record UserCreatedEvent(
        UUID userId,
        String externalId,
        String email,
        String fullName,
        LocalDateTime createdAt) {
}
