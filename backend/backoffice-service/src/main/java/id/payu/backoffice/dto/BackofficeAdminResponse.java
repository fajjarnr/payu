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
                admin.id,
                admin.username,
                admin.email,
                admin.firstName,
                admin.lastName,
                admin.phoneNumber,
                admin.status,
                admin.department,
                admin.permissions,
                admin.createdAt,
                admin.lastLoginAt
        );
    }
}
