package id.payu.wallet.dto;

import id.payu.wallet.domain.model.Card;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CardResponse(
        String id,
        String walletId,
        String cardNumber, // Masked in production
        String expiryDate,
        String status,
        String cardHolderName,
        BigDecimal dailyLimit,
        LocalDateTime createdAt) {
    public static CardResponse from(Card card) {
        return new CardResponse(
                card.getId().toString(),
                card.getWalletId().toString(),
                mask(card.getCardNumber()),
                card.getExpiryDate(),
                card.getStatus().name(),
                card.getCardHolderName(),
                card.getDailyLimit(),
                card.getCreatedAt());
    }

    private static String mask(String number) {
        if (number == null || number.length() < 4)
            return number;
        return "**** **** **** " + number.substring(number.length() - 4);
    }
}
