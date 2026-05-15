package id.payu.backoffice.dto;

import id.payu.backoffice.adapter.persistence.entity.BackofficeAdminEntity;
import java.time.LocalDateTime;
import java.util.UUID;
import id.payu.backoffice.domain.AdminStatus;

public record BackofficeAdminResponse(
        UUID id,
        String username,
        String email,
        String firstName,
        String lastName,
        String phoneNumber,
        AdminStatus status,
        String department,
        String permissions,
        LocalDateTime createdAt,
        LocalDateTime lastLoginAt
) {
    public static BackofficeAdminResponse from(BackofficeAdminEntity admin) {
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
