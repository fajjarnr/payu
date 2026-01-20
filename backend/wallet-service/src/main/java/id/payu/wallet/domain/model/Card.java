package id.payu.wallet.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

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

    public Card() {
    }

    public Card(UUID id, UUID walletId, String cardNumber, String cvv, String expiryDate, String cardHolderName, CardStatus status, BigDecimal dailyLimit, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.walletId = walletId;
        this.cardNumber = cardNumber;
        this.cvv = cvv;
        this.expiryDate = expiryDate;
        this.cardHolderName = cardHolderName;
        this.status = status;
        this.dailyLimit = dailyLimit;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

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

    public static CardBuilder builder() {
        return new CardBuilder();
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getWalletId() { return walletId; }
    public void setWalletId(UUID walletId) { this.walletId = walletId; }
    public String getCardNumber() { return cardNumber; }
    public void setCardNumber(String cardNumber) { this.cardNumber = cardNumber; }
    public String getCvv() { return cvv; }
    public void setCvv(String cvv) { this.cvv = cvv; }
    public String getExpiryDate() { return expiryDate; }
    public void setExpiryDate(String expiryDate) { this.expiryDate = expiryDate; }
    public String getCardHolderName() { return cardHolderName; }
    public void setCardHolderName(String cardHolderName) { this.cardHolderName = cardHolderName; }
    public CardStatus getStatus() { return status; }
    public void setStatus(CardStatus status) { this.status = status; }
    public BigDecimal getDailyLimit() { return dailyLimit; }
    public void setDailyLimit(BigDecimal dailyLimit) { this.dailyLimit = dailyLimit; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public static class CardBuilder {
        private UUID id;
        private UUID walletId;
        private String cardNumber;
        private String cvv;
        private String expiryDate;
        private String cardHolderName;
        private CardStatus status;
        private BigDecimal dailyLimit;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        CardBuilder() {}

        public CardBuilder id(UUID id) { this.id = id; return this; }
        public CardBuilder walletId(UUID walletId) { this.walletId = walletId; return this; }
        public CardBuilder cardNumber(String cardNumber) { this.cardNumber = cardNumber; return this; }
        public CardBuilder cvv(String cvv) { this.cvv = cvv; return this; }
        public CardBuilder expiryDate(String expiryDate) { this.expiryDate = expiryDate; return this; }
        public CardBuilder cardHolderName(String cardHolderName) { this.cardHolderName = cardHolderName; return this; }
        public CardBuilder status(CardStatus status) { this.status = status; return this; }
        public CardBuilder dailyLimit(BigDecimal dailyLimit) { this.dailyLimit = dailyLimit; return this; }
        public CardBuilder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public CardBuilder updatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; return this; }

        public Card build() {
            return new Card(id, walletId, cardNumber, cvv, expiryDate, cardHolderName, status, dailyLimit, createdAt, updatedAt);
        }
    }
}
