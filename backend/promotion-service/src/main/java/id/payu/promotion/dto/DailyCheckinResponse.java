package id.payu.promotion.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record DailyCheckinResponse(
    UUID id,
    String accountId,
    LocalDate checkinDate,
    Integer streakCount,
    Integer pointsEarned,
    LocalDateTime createdAt
) {
}
