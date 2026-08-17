package id.payu.promotion.interfaces.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import id.payu.promotion.domain.ReferralRewardType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

public record CreateReferralRequest(
    String referrerAccountId,
    @NotNull @DecimalMin(value = "0.0000") @Digits(integer = 15, fraction = 4) BigDecimal referrerReward,
    @NotNull @DecimalMin(value = "0.0000") @Digits(integer = 15, fraction = 4) BigDecimal refereeReward,
    @NotNull ReferralRewardType rewardType,
    LocalDateTime expiryDate
) {}
