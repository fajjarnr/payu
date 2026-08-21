package id.payu.support.interfaces.dto;

import java.time.Instant;
import java.util.UUID;

public record SupportTicketResponse(
    UUID id,
    String tenantId,
    String userId,
    String subject,
    String description,
    String category,
    String priority,
    String status,
    String assignedTo,
    Instant createdAt,
    Instant updatedAt,
    Instant resolvedAt
) {}
