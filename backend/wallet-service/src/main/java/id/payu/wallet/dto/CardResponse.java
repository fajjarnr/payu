package id.payu.wallet.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Card response DTO.
 *
 * <p><b>Security:</b> Card numbers are masked in API responses to show only
 * the last 4 digits (e.g., **** **** **** 1234). This prevents exposure of
 * full card numbers via API responses as per PCI-DSS requirements.</p>
 *
 * <p><b>PCI-DSS Compliance:</b> Full card numbers must NEVER be exposed in API responses.
 * Use {@link #getCardNumber()} for API responses (returns masked format) and
 * {@link #getFullCardNumber()} for internal processing only.</p>
 */
public class CardResponse {
    private UUID id;
    private UUID walletId;
    private String cardNumber; // Internal field for mapping
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

    /**
     * Returns the masked card number showing only last 4 digits.
     * Format: **** **** **** 1234
     *
     * <p>This is the ONLY card number representation exposed in API responses
     * to comply with PCI-DSS requirements. Full card numbers must NEVER be
     * exposed in API responses.</p>
     *
     * @return masked card number (e.g., "**** **** **** 3456")
     */
    @JsonProperty("cardNumber")
    public String getCardNumber() {
        if (cardNumber == null || cardNumber.length() < 4) {
            return "**** **** **** ****";
        }
        // PCI-DSS compliant masking: only last 4 digits visible
        return "**** **** **** " + cardNumber.substring(cardNumber.length() - 4);
    }

    /**
     * Internal-only setter for mapping. Use getCardNumber() for API responses
     * which returns PCI-DSS compliant masked format.
     */
    public void setCardNumber(String cardNumber) { this.cardNumber = cardNumber; }

    /**
     * Internal-only accessor for full card number. NEVER exposed in API responses.
     * Use only for internal processing (e.g., payment authorization).
     *
     * @return full card number (for internal use only)
     */
    @JsonIgnore
    public String getFullCardNumber() { return cardNumber; }

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
