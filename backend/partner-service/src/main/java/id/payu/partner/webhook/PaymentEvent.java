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
@Getter
@Builder
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
}
