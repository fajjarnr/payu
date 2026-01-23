package id.payu.wallet.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record FxRateInfo(
        UUID id,
        String fromCurrency,
        String toCurrency,
        BigDecimal rate,
        BigDecimal inverseRate,
        LocalDateTime validFrom,
        LocalDateTime validUntil
) {}
