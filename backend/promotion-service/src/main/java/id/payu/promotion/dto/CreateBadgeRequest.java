package id.payu.promotion.dto;

import id.payu.promotion.domain.Badge.RequirementType;
import java.math.BigDecimal;
import java.util.UUID;

public record CreateBadgeRequest(
    String name,
    String description,
    String iconUrl,
    RequirementType requirementType,
    BigDecimal requirementValue,
    Integer pointsReward,
    String category
) {
}
