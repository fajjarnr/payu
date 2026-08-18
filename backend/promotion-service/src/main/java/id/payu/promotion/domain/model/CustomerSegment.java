package id.payu.promotion.domain.model;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Customer segment domain model (BE-PROMO-001).
 */
public record CustomerSegment(
    UUID id,
    String name,
    String description,
    String rules,
    Boolean isActive,
    Integer priority,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    Long version
) {
}
