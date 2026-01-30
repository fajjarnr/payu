package id.payu.partner.webhook;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import id.payu.api.common.webhook.WebhookHandler;
import id.payu.api.common.webhook.WebhookValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Webhook handler for payment notifications from BI-FAST and QRIS.
 *
 * <p>This handler processes payment webhooks with the following event types:
 * <ul>
 *   <li>{@code payment.completed} - Payment successfully completed</li>
 *   <li>{@code payment.failed} - Payment failed</li>
 *   <li>{@code payment.pending} - Payment is pending/in progress</li>
 *   <li>{@code payment.refunded} - Payment has been refunded</li>
 * </ul>
 *
 * <p><strong>Example Webhook Payload:</strong>
 * <pre>{@code
 * {
 *   "event": "payment.completed",
 *   "timestamp": "2026-01-30T10:30:00Z",
 *   "data": {
 *     "transactionId": "TXN-123456789",
 *     "referenceId": "REF-987654321",
 *     "amount": 150000.00,
 *     "currency": "IDR",
 *     "sourceAccount": "1234567890",
 *     "destinationAccount": "0987654321",
 *     "settlementTime": "2026-01-30T10:30:00Z",
 *     "metadata": {
 *       "channel": "BI-FAST",
 *       "fee": 2500.00
 *     }
 *   }
 * }
 * }</pre>
 *
 * @author PayU Platform Engineering
 * @see WebhookHandler
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentWebhookHandler implements WebhookHandler {

    private final ObjectMapper objectMapper;
    private final PaymentNotificationService notificationService;

    private static final String EVENT_COMPLETED = "payment.completed";
    private static final String EVENT_FAILED = "payment.failed";
    private static final String EVENT_PENDING = "payment.pending";
    private static final String EVENT_REFUNDED = "payment.refunded";

    @Override
    public String[] supportedEventTypes() {
        return new String[]{
                EVENT_COMPLETED,
                EVENT_FAILED,
                EVENT_PENDING,
                EVENT_REFUNDED
        };
    }

    @Override
    public boolean validatePayload(String webhookId, String payload) {
        try {
            JsonNode root = objectMapper.readTree(payload);

            // Validate required fields
            if (!root.has("event")) {
                log.warn("Missing 'event' field in webhook payload: id={}", webhookId);
                return false;
            }

            if (!root.has("data")) {
                log.warn("Missing 'data' field in webhook payload: id={}", webhookId);
                return false;
            }

            JsonNode data = root.get("data");

            // Validate data fields
            if (!data.has("transactionId")) {
                log.warn("Missing 'transactionId' in webhook data: id={}", webhookId);
                return false;
            }

            if (!data.has("amount")) {
                log.warn("Missing 'amount' in webhook data: id={}", webhookId);
                return false;
            }

            // Validate event type
            String eventType = root.get("event").asText();
            if (!isSupportedEventType(eventType)) {
                log.warn("Unsupported event type '{}': id={}", eventType, webhookId);
                return false;
            }

            return true;

        } catch (Exception e) {
            log.warn("Failed to parse webhook payload: id={}", webhookId, e);
            return false;
        }
    }

    @Override
    public void processWebhook(String webhookId, String payload) {
        try {
            JsonNode root = objectMapper.readTree(payload);
            String eventType = root.get("event").asText();
            JsonNode data = root.get("data");

            PaymentEvent event = PaymentEvent.builder()
                    .webhookId(webhookId)
                    .eventType(eventType)
                    .transactionId(data.get("transactionId").asText())
                    .referenceId(data.has("referenceId") ? data.get("referenceId").asText() : null)
                    .amount(new BigDecimal(data.get("amount").asText()))
                    .currency(data.has("currency") ? data.get("currency").asText() : "IDR")
                    .sourceAccount(data.has("sourceAccount") ? data.get("sourceAccount").asText() : null)
                    .destinationAccount(data.has("destinationAccount") ? data.get("destinationAccount").asText() : null)
                    .settlementTime(data.has("settlementTime")
                            ? Instant.parse(data.get("settlementTime").asText())
                            : Instant.now())
                    .rawPayload(payload)
                    .build();

            switch (eventType) {
                case EVENT_COMPLETED:
                    handlePaymentCompleted(event);
                    break;
                case EVENT_FAILED:
                    handlePaymentFailed(event, data);
                    break;
                case EVENT_PENDING:
                    handlePaymentPending(event);
                    break;
                case EVENT_REFUNDED:
                    handlePaymentRefunded(event, data);
                    break;
                default:
                    throw new WebhookValidationException(webhookId, "Unsupported event type: " + eventType);
            }

        } catch (WebhookValidationException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to process payment webhook: id={}", webhookId, e);
            throw new RuntimeException("Payment processing failed", e);
        }
    }

    @Override
    public void onSuccess(String webhookId, Object result) {
        log.info("Payment webhook processed successfully: id={}", webhookId);
        // Update metrics, send notifications, etc.
        notificationService.notifySuccess(webhookId);
    }

    @Override
    public void onError(String webhookId, Throwable error) {
        log.error("Payment webhook processing failed: id={}", webhookId, error);
        // Send alert to operations team
        notificationService.notifyFailure(webhookId, error.getMessage());
    }

    /**
     * Handles payment.completed events.
     *
     * @param event the parsed payment event
     */
    private void handlePaymentCompleted(PaymentEvent event) {
        log.info("Processing payment completion: transactionId={}, amount={} {}",
                event.getTransactionId(), event.getAmount(), event.getCurrency());

        // Update transaction status
        notificationService.updateTransactionStatus(
                event.getTransactionId(),
                TransactionStatus.COMPLETED,
                event.getSettlementTime()
        );

        // Update wallet balance
        notificationService.creditWallet(
                event.getDestinationAccount(),
                event.getAmount(),
                event.getTransactionId()
        );

        // Send notification to user
        notificationService.sendUserNotification(
                event.getDestinationAccount(),
                "Payment Received",
                String.format("You received %s %s", event.getAmount(), event.getCurrency())
        );
    }

    /**
     * Handles payment.failed events.
     *
     * @param event the parsed payment event
     * @param data the raw data node for additional fields
     */
    private void handlePaymentFailed(PaymentEvent event, JsonNode data) {
        String failureReason = data.has("failureReason")
                ? data.get("failureReason").asText()
                : "Unknown";

        log.warn("Processing payment failure: transactionId={}, reason={}",
                event.getTransactionId(), failureReason);

        // Update transaction status
        notificationService.updateTransactionStatus(
                event.getTransactionId(),
                TransactionStatus.FAILED,
                Instant.now()
        );

        // Release hold on source account if any
        if (event.getSourceAccount() != null) {
            notificationService.releaseHold(
                    event.getSourceAccount(),
                    event.getTransactionId()
            );
        }

        // Send failure notification
        notificationService.sendUserNotification(
                event.getSourceAccount(),
                "Payment Failed",
                String.format("Your payment of %s %s failed: %s",
                        event.getAmount(), event.getCurrency(), failureReason)
        );
    }

    /**
     * Handles payment.pending events.
     *
     * @param event the parsed payment event
     */
    private void handlePaymentPending(PaymentEvent event) {
        log.info("Processing payment pending: transactionId={}", event.getTransactionId());

        // Update transaction status
        notificationService.updateTransactionStatus(
                event.getTransactionId(),
                TransactionStatus.PENDING,
                Instant.now()
        );

        // Set timeout for pending transaction
        notificationService.schedulePendingTimeout(
                event.getTransactionId(),
                Instant.now().plusSeconds(300) // 5 minutes timeout
        );
    }

    /**
     * Handles payment.refunded events.
     *
     * @param event the parsed payment event
     * @param data the raw data node for additional fields
     */
    private void handlePaymentRefunded(PaymentEvent event, JsonNode data) {
        BigDecimal refundAmount = data.has("refundAmount")
                ? new BigDecimal(data.get("refundAmount").asText())
                : event.getAmount();

        String refundReason = data.has("refundReason")
                ? data.get("refundReason").asText()
                : "Customer request";

        log.info("Processing payment refund: transactionId={}, amount={}, reason={}",
                event.getTransactionId(), refundAmount, refundReason);

        // Create refund transaction
        String refundId = notificationService.createRefundTransaction(
                event.getTransactionId(),
                refundAmount,
                refundReason
        );

        // Debit wallet
        notificationService.debitWallet(
                event.getDestinationAccount(),
                refundAmount,
                refundId
        );

        // Credit source account
        notificationService.creditWallet(
                event.getSourceAccount(),
                refundAmount,
                refundId
        );

        // Send notification
        notificationService.sendUserNotification(
                event.getSourceAccount(),
                "Payment Refunded",
                String.format("Your payment of %s %s has been refunded",
                        refundAmount, event.getCurrency())
        );
    }

    /**
     * Checks if the event type is supported.
     */
    private boolean isSupportedEventType(String eventType) {
        for (String supported : supportedEventTypes()) {
            if (supported.equals(eventType)) {
                return true;
            }
        }
        return false;
    }
}
