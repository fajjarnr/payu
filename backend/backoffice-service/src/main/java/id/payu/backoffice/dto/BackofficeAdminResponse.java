package id.payu.backoffice.dto;

import id.payu.backoffice.domain.BackofficeAdmin;
import java.time.LocalDateTime;
import java.util.UUID;

public record BackofficeAdminResponse(
        UUID id,
        String username,
        String email,
        String firstName,
        String lastName,
        String phoneNumber,
        BackofficeAdmin.AdminStatus status,
        String department,
        String permissions,
        LocalDateTime createdAt,
        LocalDateTime lastLoginAt
) {
    public static BackofficeAdminResponse from(BackofficeAdmin admin) {
        return new BackofficeAdminResponse(
                admin.getId(),
                admin.getUsername(),
                admin.getEmail(),
                admin.getFirstName(),
                admin.getLastName(),
                admin.getPhoneNumber(),
                admin.getStatus(),
                admin.getDepartment(),
                admin.getPermissions(),
                admin.getCreatedAt(),
                admin.getLastLoginAt()
        );
    }
}
