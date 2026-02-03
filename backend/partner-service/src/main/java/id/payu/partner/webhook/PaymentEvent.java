package id.payu.partner.webhook;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Domain object representing a payment webhook event.
 *
 * <p>This class encapsulates the payment data received from webhook notifications
 * sent by external payment providers (BI-FAST, QRIS, etc.).
 *
 * @author PayU Platform Engineering
 * @since 1.0.0
 */
public class PaymentEvent {

    /**
     * Unique identifier of the webhook (from X-Webhook-Id header).
     */
    private final String webhookId;

    /**
     * Type of event (e.g., payment.completed, payment.failed).
     */
    private final String eventType;

    /**
     * Unique transaction identifier from the payment provider.
     */
    private final String transactionId;

    /**
     * Reference ID assigned by the partner.
     */
    private final String referenceId;

    /**
     * Payment amount.
     */
    private final BigDecimal amount;

    /**
     * Currency code (e.g., IDR).
     */
    private final String currency;

    /**
     * Source account number (payer).
     */
    private final String sourceAccount;

    /**
     * Destination account number (payee).
     */
    private final String destinationAccount;

    /**
     * Time when the payment was settled.
     */
    private final Instant settlementTime;

    /**
     * Raw JSON payload for audit purposes.
     */
    private final String rawPayload;

    private PaymentEvent(String webhookId, String eventType, String transactionId, String referenceId,
                        BigDecimal amount, String currency, String sourceAccount, String destinationAccount,
                        Instant settlementTime, String rawPayload) {
        this.webhookId = webhookId;
        this.eventType = eventType;
        this.transactionId = transactionId;
        this.referenceId = referenceId;
        this.amount = amount;
        this.currency = currency;
        this.sourceAccount = sourceAccount;
        this.destinationAccount = destinationAccount;
        this.settlementTime = settlementTime;
        this.rawPayload = rawPayload;
    }

    public String getWebhookId() { return webhookId; }
    public String getEventType() { return eventType; }
    public String getTransactionId() { return transactionId; }
    public String getReferenceId() { return referenceId; }
    public BigDecimal getAmount() { return amount; }
    public String getCurrency() { return currency; }
    public String getSourceAccount() { return sourceAccount; }
    public String getDestinationAccount() { return destinationAccount; }
    public Instant getSettlementTime() { return settlementTime; }
    public String getRawPayload() { return rawPayload; }

    public static PaymentEventBuilder builder() {
        return new PaymentEventBuilder();
    }

    public static class PaymentEventBuilder {
        private String webhookId;
        private String eventType;
        private String transactionId;
        private String referenceId;
        private BigDecimal amount;
        private String currency;
        private String sourceAccount;
        private String destinationAccount;
        private Instant settlementTime;
        private String rawPayload;

        public PaymentEventBuilder webhookId(String webhookId) { this.webhookId = webhookId; return this; }
        public PaymentEventBuilder eventType(String eventType) { this.eventType = eventType; return this; }
        public PaymentEventBuilder transactionId(String transactionId) { this.transactionId = transactionId; return this; }
        public PaymentEventBuilder referenceId(String referenceId) { this.referenceId = referenceId; return this; }
        public PaymentEventBuilder amount(BigDecimal amount) { this.amount = amount; return this; }
        public PaymentEventBuilder currency(String currency) { this.currency = currency; return this; }
        public PaymentEventBuilder sourceAccount(String sourceAccount) { this.sourceAccount = sourceAccount; return this; }
        public PaymentEventBuilder destinationAccount(String destinationAccount) { this.destinationAccount = destinationAccount; return this; }
        public PaymentEventBuilder settlementTime(Instant settlementTime) { this.settlementTime = settlementTime; return this; }
        public PaymentEventBuilder rawPayload(String rawPayload) { this.rawPayload = rawPayload; return this; }

        public PaymentEvent build() {
            return new PaymentEvent(webhookId, eventType, transactionId, referenceId, amount, currency,
                    sourceAccount, destinationAccount, settlementTime, rawPayload);
        }
    }
}
