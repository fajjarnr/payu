package id.payu.wallet.dto;

import java.math.BigDecimal;

public record CreateCardRequest(
        String accountId,
        String cardHolderName,
        BigDecimal dailyLimit) {
}
