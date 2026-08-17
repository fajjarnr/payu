package id.payu.lending.interfaces.dto;

import id.payu.security.annotation.Sensitive;
import java.time.LocalDateTime;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String externalId,
        @Sensitive String username,
        @Sensitive String email,
        @Sensitive String phoneNumber,
        @Sensitive String fullName,
        @Sensitive String nik,
        String status,
        String kycStatus,
        LocalDateTime createdAt
) {
}
