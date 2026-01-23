package id.payu.promotion.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record UserLevelResponse(
    UUID id,
    String accountId,
    Integer level,
    String levelName,
    Integer xp,
    Integer xpToNextLevel,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
}
