package id.payu.wallet.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public class CardResponse {
    private UUID id;
    private UUID walletId;
    private String cardNumber;
    private String expiryDate;
    private String cardHolderName;
    private String status;
    private BigDecimal dailyLimit;
    private LocalDateTime createdAt;

    public CardResponse() {
    }

    public CardResponse(UUID id, UUID walletId, String cardNumber, String expiryDate, String cardHolderName, String status, BigDecimal dailyLimit, LocalDateTime createdAt) {
        this.id = id;
        this.walletId = walletId;
        this.cardNumber = cardNumber;
        this.expiryDate = expiryDate;
        this.cardHolderName = cardHolderName;
        this.status = status;
        this.dailyLimit = dailyLimit;
        this.createdAt = createdAt;
    }

    public static CardResponseBuilder builder() {
        return new CardResponseBuilder();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getWalletId() { return walletId; }
    public void setWalletId(UUID walletId) { this.walletId = walletId; }
    public String getCardNumber() { return cardNumber; }
    public void setCardNumber(String cardNumber) { this.cardNumber = cardNumber; }
    public String getExpiryDate() { return expiryDate; }
    public void setExpiryDate(String expiryDate) { this.expiryDate = expiryDate; }
    public String getCardHolderName() { return cardHolderName; }
    public void setCardHolderName(String cardHolderName) { this.cardHolderName = cardHolderName; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public BigDecimal getDailyLimit() { return dailyLimit; }
    public void setDailyLimit(BigDecimal dailyLimit) { this.dailyLimit = dailyLimit; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public static class CardResponseBuilder {
        private UUID id;
        private UUID walletId;
        private String cardNumber;
        private String expiryDate;
        private String cardHolderName;
        private String status;
        private BigDecimal dailyLimit;
        private LocalDateTime createdAt;

        CardResponseBuilder() {}

        public CardResponseBuilder id(UUID id) { this.id = id; return this; }
        public CardResponseBuilder walletId(UUID walletId) { this.walletId = walletId; return this; }
        public CardResponseBuilder cardNumber(String cardNumber) { this.cardNumber = cardNumber; return this; }
        public CardResponseBuilder expiryDate(String expiryDate) { this.expiryDate = expiryDate; return this; }
        public CardResponseBuilder cardHolderName(String cardHolderName) { this.cardHolderName = cardHolderName; return this; }
        public CardResponseBuilder status(String status) { this.status = status; return this; }
        public CardResponseBuilder dailyLimit(BigDecimal dailyLimit) { this.dailyLimit = dailyLimit; return this; }
        public CardResponseBuilder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }

        public CardResponse build() {
            return new CardResponse(id, walletId, cardNumber, expiryDate, cardHolderName, status, dailyLimit, createdAt);
        }
    }
}
