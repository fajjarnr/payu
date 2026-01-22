package id.payu.lending.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String externalId,
        String username,
        String email,
        String phoneNumber,
        String fullName,
        String nik,
        String status,
        String kycStatus,
        LocalDateTime createdAt
) {
}
