package id.payu.wallet.adapter.persistence.entity;

import id.payu.security.converter.EncryptedStringConverter;
import id.payu.wallet.domain.model.Card;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import id.payu.wallet.domain.model.CardStatus;

/**
 * JPA Entity for Card persistence.
 *
 * <p><b>Security:</b> Card number is encrypted at rest using field-level encryption
 * via {@link EncryptedStringConverter}. This ensures compliance with PCI-DSS requirements
 * for storage of sensitive cardholder data.</p>
 *
 * <p><b>Note:</b> CVV is NEVER stored in the database as per PCI-DSS requirements.
 * CVV is only used during authorization and immediately discarded.</p>
 */
@Entity
@Table(name = "cards")
// BUG-ARCH-005 FIX: Replaced @Data with @Getter @Setter to avoid Lombok-generated equals/hashCode on JPA entities
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CardEntity {

    @Id
    private UUID id;

    @Column(name = "wallet_id", nullable = false)
    private UUID walletId;

    /**
     * Card number encrypted at rest using AES-GCM (256-bit key).
     * The EncryptedStringConverter handles automatic encryption/decryption.
     */
    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "card_number", nullable = false, length = 512)
    private String cardNumber;

    @Column(name = "expiry_date", nullable = false, length = 5)
    private String expiryDate;

    @Column(name = "card_holder_name", nullable = false)
    private String cardHolderName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CardStatus status;

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
                .expiryDate(card.getExpiryDate())
                .cardHolderName(card.getCardHolderName())
                .status(card.getStatus())
                .dailyLimit(card.getDailyLimit())
                .createdAt(card.getCreatedAt())
                .updatedAt(card.getUpdatedAt())
                .build();
    }
}
