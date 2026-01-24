package id.payu.promotion.dto;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for customer segment data.
 */
public record CustomerSegmentResponse(
    UUID id,
    String name,
    String description,
    String rules,
    Boolean isActive,
    Integer priority,
    Long memberCount,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
}
