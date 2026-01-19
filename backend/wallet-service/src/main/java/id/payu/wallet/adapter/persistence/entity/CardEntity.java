package id.payu.wallet.adapter.persistence.entity;

import id.payu.wallet.domain.model.Card;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "cards")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CardEntity {

    @Id
    private UUID id;

    @Column(name = "wallet_id", nullable = false)
    private UUID walletId;

    @Column(name = "card_number", nullable = false, length = 16)
    private String cardNumber;

    @Column(nullable = false, length = 3)
    private String cvv;

    @Column(name = "expiry_date", nullable = false, length = 5)
    private String expiryDate;

    @Column(name = "card_holder_name", nullable = false)
    private String cardHolderName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Card.CardStatus status;

    @Column(name = "daily_limit")
    private BigDecimal dailyLimit;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public Card toDomain() {
        return Card.builder()
                .id(this.id)
                .walletId(this.walletId)
                .cardNumber(this.cardNumber)
                .cvv(this.cvv)
                .expiryDate(this.expiryDate)
                .cardHolderName(this.cardHolderName)
                .status(this.status)
                .dailyLimit(this.dailyLimit)
                .createdAt(this.createdAt)
                .updatedAt(this.updatedAt)
                .build();
    }

    public static CardEntity fromDomain(Card card) {
        return CardEntity.builder()
                .id(card.getId())
                .walletId(card.getWalletId())
                .cardNumber(card.getCardNumber())
                .cvv(card.getCvv())
                .expiryDate(card.getExpiryDate())
                .cardHolderName(card.getCardHolderName())
                .status(card.getStatus())
                .dailyLimit(card.getDailyLimit())
                .createdAt(card.getCreatedAt())
                .updatedAt(card.getUpdatedAt())
                .build();
    }
}
