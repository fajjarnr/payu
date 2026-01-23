package id.payu.promotion.dto;

import java.util.List;

public record GamificationSummaryResponse(
    UserLevelResponse level,
    List<EarnedBadgeResponse> badges,
    DailyCheckinResponse lastCheckin,
    Integer currentStreak,
    Long totalCheckins
) {
}
