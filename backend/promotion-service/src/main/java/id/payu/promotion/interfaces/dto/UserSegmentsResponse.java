package id.payu.promotion.interfaces.dto;

import java.util.List;

/**
 * Response DTO for user's segment memberships.
 */
public record UserSegmentsResponse(
    String accountId,
    List<SegmentMembershipResponse> segments
) {
}
