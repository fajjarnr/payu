package id.payu.promotion.dto;

import java.util.List;

/**
 * Response DTO for user's segment memberships.
 */
public record UserSegmentsResponse(
    String accountId,
    List<SegmentMembershipResponse> segments
) {
}
