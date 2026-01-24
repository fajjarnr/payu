package id.payu.promotion.dto;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for segment membership data.
 */
public record SegmentMembershipResponse(
    UUID id,
    String accountId,
    UUID segmentId,
    String segmentName,
    Boolean isActive,
    LocalDateTime lastEvaluatedAt,
    LocalDateTime createdAt
) {
}
