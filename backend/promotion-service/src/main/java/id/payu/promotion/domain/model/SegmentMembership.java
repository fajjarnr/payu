package id.payu.promotion.domain.model;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Segment membership domain model (BE-PROMO-001).
 */
public record SegmentMembership(
    UUID id,
    String accountId,
    UUID segmentId,
    Boolean isActive,
    LocalDateTime lastEvaluatedAt,
    LocalDateTime createdAt,
    Long version
) {
}
