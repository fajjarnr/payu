package id.payu.promotion.dto;

import id.payu.promotion.domain.Badge.RequirementType;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record EarnedBadgeResponse(
    UUID id,
    UUID badgeId,
    String badgeName,
    String description,
    String iconUrl,
    RequirementType requirementType,
    BigDecimal requirementValue,
    Integer pointsReward,
    String category,
    LocalDateTime earnedAt
) {
}
