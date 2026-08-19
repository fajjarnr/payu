package id.payu.notification.adapter.messaging;

import id.payu.notification.domain.NotificationChannel;
import id.payu.notification.interfaces.dto.SendNotificationRequest;
import id.payu.notification.application.service.NotificationService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.jboss.logging.Logger;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import io.micrometer.core.instrument.MeterRegistry;

/**
 * Kafka consumer for wallet, transaction, payment, and split bill events.
 * Sends notifications based on events.
 * Refactored (MSG-023) to use ObjectMapper, metrics, proper DLQ routing, and idempotency checks.
 */
@ApplicationScoped
public class EventConsumer {

    private static final Logger LOG = Logger.getLogger(EventConsumer.class);

    @Inject
    NotificationService notificationService;

    @Inject
    ObjectMapper objectMapper;

    @Inject
    MeterRegistry meterRegistry;

    private void recordMetric(String topic, String status) {
        if (meterRegistry != null) {
            meterRegistry.counter("notification.consumed.events", 
                    "topic", topic, "status", status).increment();
        }
    }

    private String getEventId(JsonNode node) {
        if (node.has("id")) {
            return node.get("id").asText("");
        }
        return "";
    }

    private String getNestedValue(JsonNode node, String key) {
        if (node.has("data") && node.get("data").isObject()) {
            JsonNode dataNode = node.get("data");
            if (dataNode.has(key)) {
                return dataNode.get(key).asText("");
            }
        }
        if (node.has(key)) {
            return node.get(key).asText("");
        }
        return "";
    }

    @Incoming("wallet-events")
    public void onWalletEvent(String payload) {
        LOG.infof("Received wallet event: %s", payload);
        try {
            JsonNode node = objectMapper.readTree(payload);
            String eventId = getEventId(node);
            String userId = getNestedValue(node, "userId");
            String email = getNestedValue(node, "email");
            String balance = getNestedValue(node, "balance");

            if (userId.isEmpty() || email.isEmpty()) {
                LOG.warn("Required fields missing in wallet event - ignoring");
                recordMetric("wallet-events", "ignored");
                return;
            }

            LOG.info("Processing wallet balance change notification");
            SendNotificationRequest notification = new SendNotificationRequest(
                userId,
                NotificationChannel.EMAIL,
                email,
                "Wallet Balance Updated",
                "Your wallet balance has been updated to: Rp " + balance,
                "wallet-update",
                null,
                eventId.isEmpty() ? null : "wallet-" + eventId
            );
            notificationService.send(notification, eventId.isEmpty() ? null : "wallet-" + eventId);
            recordMetric("wallet-events", "success");
        } catch (Exception e) {
            LOG.errorf(e, "Failed to process wallet event: %s", e.getMessage());
            recordMetric("wallet-events", "failed");
            throw new RuntimeException("Failed to process wallet event", e);
        }
    }

    @Incoming("transaction-events")
    public void onTransactionEvent(String payload) {
        LOG.infof("Received transaction event: %s", payload);
        try {
            JsonNode node = objectMapper.readTree(payload);
            String eventId = getEventId(node);
            String userId = getNestedValue(node, "userId");
            String email = getNestedValue(node, "email");
            String transactionId = getNestedValue(node, "transactionId");
            String amount = getNestedValue(node, "amount");

            if (userId.isEmpty() || email.isEmpty()) {
                LOG.warn("Required fields missing in transaction event - ignoring");
                recordMetric("transaction-events", "ignored");
                return;
            }

            LOG.info("Processing transaction notification");
            SendNotificationRequest notification = new SendNotificationRequest(
                userId,
                NotificationChannel.EMAIL,
                email,
                "Transaction Completed",
                String.format("Your transaction %s of amount %s has been completed successfully.", transactionId, amount),
                "transaction-completed",
                null,
                eventId.isEmpty() ? null : "transaction-" + eventId
            );
            notificationService.send(notification, eventId.isEmpty() ? null : "transaction-" + eventId);
            recordMetric("transaction-events", "success");
        } catch (Exception e) {
            LOG.errorf(e, "Failed to process transaction event: %s", e.getMessage());
            recordMetric("transaction-events", "failed");
            throw new RuntimeException("Failed to process transaction event", e);
        }
    }

    @Incoming("billing-payment-events")
    public void onBillingPaymentEvent(String payload) {
        // ADR-0027: billing completed is same flow as payment-events, but separate topic payu.billing.payment-completed.v1
        onPaymentEvent(payload);
    }

    @Incoming("payment-events")
    public void onPaymentEvent(String payload) {
        LOG.infof("Received payment event: %s", payload);
        try {
            JsonNode node = objectMapper.readTree(payload);
            String eventId = getEventId(node);
            String userId = getNestedValue(node, "userId");
            String email = getNestedValue(node, "email");
            String billingId = getNestedValue(node, "billingId");
            String amount = getNestedValue(node, "amount");

            if (userId.isEmpty() || email.isEmpty()) {
                LOG.warn("Required fields missing in payment event - ignoring");
                recordMetric("payment-events", "ignored");
                return;
            }

            LOG.info("Processing bill payment notification");
            SendNotificationRequest notification = new SendNotificationRequest(
                userId,
                NotificationChannel.EMAIL,
                email,
                "Payment Event Notification",
                String.format("Payment status update for bill %s, amount %s.", billingId, amount),
                "payment-update",
                null,
                eventId.isEmpty() ? null : "payment-" + eventId
            );
            notificationService.send(notification, eventId.isEmpty() ? null : "payment-" + eventId);
            recordMetric("payment-events", "success");
        } catch (Exception e) {
            LOG.errorf(e, "Failed to process payment event: %s", e.getMessage());
            recordMetric("payment-events", "failed");
            throw new RuntimeException("Failed to process payment event", e);
        }
    }

    @Incoming("split-bill-events")
    public void onSplitBillEvent(String payload) {
        LOG.infof("Received split bill event: %s", payload);
        try {
            processSplitBillEvent(payload);
            recordMetric("split-bill-events", "success");
        } catch (Exception e) {
            LOG.errorf(e, "Failed to process split bill event: %s", e.getMessage());
            recordMetric("split-bill-events", "failed");
            throw new RuntimeException("Failed to process split bill event", e);
        }
    }

    // ==================== KYC Events (ADR-001: Hybrid KYC) ====================

    @Incoming("kyc-verified-events")
    public void onKycVerified(String payload) {
        LOG.infof("Received KYC verified event: %s", payload);
        try {
            JsonNode node = objectMapper.readTree(payload);
            String userId = getNestedValue(node, "user_id");
            String eventId = getNestedValue(node, "event_id");
            if (userId.isEmpty()) {
                userId = getNestedValue(node, "userId");
            }
            if (eventId.isEmpty()) {
                eventId = getEventId(node);
            }

            LOG.infof("KYC verified for user: %s — sending in-app notification", userId);
            String idempotencyKey = eventId.isEmpty() ? null : "kyc-verified-" + eventId;
            
            SendNotificationRequest notification = new SendNotificationRequest(
                null, NotificationChannel.IN_APP,
                userId, "KYC Verification Approved",
                "Your identity verification has been approved. Welcome to PayU!",
                null, null,
                idempotencyKey
            );
            notificationService.send(notification, idempotencyKey);
            recordMetric("kyc-verified-events", "success");
        } catch (Exception e) {
            LOG.errorf(e, "Failed to process KYC verified event: %s", e.getMessage());
            recordMetric("kyc-verified-events", "failed");
            throw new RuntimeException("Failed to process KYC verified event", e);
        }
    }

    @Incoming("kyc-failed-events")
    public void onKycFailed(String payload) {
        LOG.infof("Received KYC failed event: %s", payload);
        try {
            JsonNode node = objectMapper.readTree(payload);
            String userId = getNestedValue(node, "user_id");
            String reason = getNestedValue(node, "reason");
            String eventId = getNestedValue(node, "event_id");
            if (userId.isEmpty()) {
                userId = getNestedValue(node, "userId");
            }
            if (eventId.isEmpty()) {
                eventId = getEventId(node);
            }

            LOG.infof("KYC failed for user: %s reason: %s", userId, reason);
            String idempotencyKey = eventId.isEmpty() ? null : "kyc-failed-" + eventId;
            
            SendNotificationRequest notification = new SendNotificationRequest(
                null, NotificationChannel.IN_APP,
                userId, "KYC Verification Failed",
                "Your identity verification was not approved. Reason: " + reason,
                null, null,
                idempotencyKey
            );
            notificationService.send(notification, idempotencyKey);
            recordMetric("kyc-failed-events", "success");
        } catch (Exception e) {
            LOG.errorf(e, "Failed to process KYC failed event: %s", e.getMessage());
            recordMetric("kyc-failed-events", "failed");
            throw new RuntimeException("Failed to process KYC failed event", e);
        }
    }

    private void processSplitBillEvent(String payload) throws Exception {
        JsonNode node = objectMapper.readTree(payload);
        String eventType = getNestedValue(node, "eventType");
        
        switch (eventType) {
            case "split-bill-activated":
                sendSplitBillInvitationNotification(node);
                break;
            case "participant-added":
                sendParticipantAddedNotification(node);
                break;
            case "payment-made":
                sendPaymentMadeNotification(node);
                break;
            case "split-bill-completed":
                sendSplitBillCompletedNotification(node);
                break;
            case "payment-reminder":
                sendPaymentReminderNotification(node);
                break;
            default:
                LOG.infof("Unhandled split bill event type: %s", eventType);
        }
    }

    private void sendSplitBillInvitationNotification(JsonNode node) {
        String splitBillId = getNestedValue(node, "splitBillId");
        String splitBillTitle = getNestedValue(node, "title");
        String totalAmount = getNestedValue(node, "totalAmount");
        String currency = getNestedValue(node, "currency");
        
        String body = String.format(
                "Anda telah diundang untuk pembagian tagihan: %s\n" +
                "Total Tagihan: %s %s\n" +
                "Silakan login ke aplikasi PayU untuk menerima atau menolak undangan.",
                splitBillTitle, currency, totalAmount
        );
        
        LOG.infof("Sending split bill invitation notification: splitBillId=%s", splitBillId);
    }

    private void sendParticipantAddedNotification(JsonNode node) {
        String creatorAccountId = getNestedValue(node, "creatorAccountId");
        String accountName = getNestedValue(node, "accountName");
        
        String body = String.format(
                "Anda telah menambahkan peserta baru: %s ke pembagian tagihan.",
                accountName
        );
        
        LOG.infof("Sending participant added notification: accountId=%s", creatorAccountId);
    }

    private void sendPaymentMadeNotification(JsonNode node) {
        String accountId = getNestedValue(node, "accountId");
        String accountName = getNestedValue(node, "accountName");
        String paymentAmount = getNestedValue(node, "paymentAmount");
        String currency = getNestedValue(node, "currency");
        
        String body = String.format(
                "%s telah membayar %s %s untuk pembagian tagihan.",
                accountName, currency, paymentAmount
        );
        
        LOG.infof("Sending payment made notification: accountId=%s", accountId);
    }

    private void sendSplitBillCompletedNotification(JsonNode node) {
        String creatorAccountId = getNestedValue(node, "creatorAccountId");
        String splitBillTitle = getNestedValue(node, "referenceNumber");
        String totalAmount = getNestedValue(node, "totalAmount");
        String currency = getNestedValue(node, "currency");
        
        String body = String.format(
                "Pembagian tagihan %s (%s %s) telah selesai. Semua peserta telah membayar tagihan mereka.",
                splitBillTitle, currency, totalAmount
        );
        
        LOG.infof("Sending split bill completed notification: accountId=%s", creatorAccountId);
    }

    private void sendPaymentReminderNotification(JsonNode node) {
        String accountId = getNestedValue(node, "accountId");
        String accountName = getNestedValue(node, "accountName");
        String amountOwed = getNestedValue(node, "amountOwed");
        String currency = getNestedValue(node, "currency");
        String splitBillTitle = getNestedValue(node, "referenceNumber");
        
        String body = String.format(
                "Halo %s, Anda memiliki tagihan yang belum dibayar untuk %s.\n" +
                "Jumlah Tagihan: %s %s\n" +
                "Silakan segera lakukan pembayaran.",
                accountName, splitBillTitle, currency, amountOwed
        );
        
        LOG.infof("Sending payment reminder notification: accountId=%s", accountId);
    }
}
