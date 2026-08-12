package id.payu.account.dto;

import id.payu.account.domain.model.KycStatus;
import id.payu.account.domain.model.User;
import id.payu.account.domain.model.UserStatus;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Inter-service user profile (GRPC-008). Consumed by lending-service credit scoring.
 */
public record UserProfileResponse(
        UUID id,
        String externalId,
        String username,
        String email,
        String phoneNumber,
        String fullName,
        String nik,
        UserStatus status,
        KycStatus kycStatus,
        LocalDateTime createdAt
) {
    public static UserProfileResponse from(User user) {
        return new UserProfileResponse(
                user.getId(),
                user.getExternalId(),
                user.getUsername(),
                user.getEmail(),
                user.getPhoneNumber(),
                user.getFullName(),
                user.getNik(),
                user.getStatus(),
                user.getKycStatus(),
                user.getCreatedAt());
    }
}
