package id.payu.promotion.dto;

import java.util.List;

public record GamificationEventResponse(
    List<EarnedBadgeResponse> badgesEarned,
    UserLevelResponse levelUp,
    Integer xpEarned,
    Integer pointsEarned
) {
}
