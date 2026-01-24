package id.payu.promotion.dto;

import java.util.List;

/**
 * Response DTO for members of a segment.
 */
public record SegmentMembersResponse(
    String segmentId,
    String segmentName,
    Long totalMembers,
    List<String> accountIds
) {
}
