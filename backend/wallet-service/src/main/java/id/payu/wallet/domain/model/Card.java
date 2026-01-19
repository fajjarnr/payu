package id.payu.wallet.domain.model;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class Card {
    private UUID id;
    private UUID walletId;
    private String cardNumber;
    private String cvv;
    private String expiryDate; // MM/YY
    private String cardHolderName;
    private CardStatus status;
    private BigDecimal dailyLimit;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public enum CardStatus {
        ACTIVE,
        BLOCKED,
        FROZEN
    }

    public boolean isActive() {
        return this.status == CardStatus.ACTIVE;
    }

    public void freeze() {
        this.status = CardStatus.FROZEN;
        this.updatedAt = LocalDateTime.now();
    }

    public void unfreeze() {
        if (this.status == CardStatus.FROZEN) {
            this.status = CardStatus.ACTIVE;
            this.updatedAt = LocalDateTime.now();
        }
    }
}
