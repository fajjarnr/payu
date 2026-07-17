package id.payu.backoffice.dto;

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
) {}
