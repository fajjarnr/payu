package id.payu.notification.consumer;

import id.payu.notification.domain.NotificationChannel;
import id.payu.notification.dto.SendNotificationRequest;
import id.payu.notification.service.NotificationService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.jboss.logging.Logger;

/**
 * Kafka consumer for wallet, transaction, payment, and split bill events.
 * Sends notifications based on events.
 */
@ApplicationScoped
public class EventConsumer {

    private static final Logger LOG = Logger.getLogger(EventConsumer.class);

    @Inject
    NotificationService notificationService;

    @Incoming("wallet-events")
    public void onWalletEvent(String payload) {
        LOG.infof("Received wallet event: %s", payload);
        try {
            // Parse and send notification
            // In production, parse JSON and extract userId, email, etc.
            LOG.info("Processing wallet balance change notification");
        } catch (Exception e) {
            LOG.errorf("Failed to process wallet event: %s", e.getMessage());
        }
    }

    @Incoming("transaction-events")
    public void onTransactionEvent(String payload) {
        LOG.infof("Received transaction event: %s", payload);
        try {
            // Parse and send notification
            LOG.info("Processing transaction notification");
        } catch (Exception e) {
            LOG.errorf("Failed to process transaction event: %s", e.getMessage());
        }
    }

    @Incoming("payment-events")
    public void onPaymentEvent(String payload) {
        LOG.infof("Received payment event: %s", payload);
        try {
            // Parse and send notification
            LOG.info("Processing bill payment notification");
        } catch (Exception e) {
            LOG.errorf("Failed to process payment event: %s", e.getMessage());
        }
    }

    @Incoming("split-bill-events")
    public void onSplitBillEvent(String payload) {
        LOG.infof("Received split bill event: %s", payload);
        try {
            processSplitBillEvent(payload);
        } catch (Exception e) {
            LOG.errorf("Failed to process split bill event: %s", e.getMessage());
        }
    }

    private void processSplitBillEvent(String payload) {
        String eventType = extractEventType(payload);
        
        switch (eventType) {
            case "split-bill-activated":
                sendSplitBillInvitationNotification(payload);
                break;
            case "participant-added":
                sendParticipantAddedNotification(payload);
                break;
            case "payment-made":
                sendPaymentMadeNotification(payload);
                break;
            case "split-bill-completed":
                sendSplitBillCompletedNotification(payload);
                break;
            case "payment-reminder":
                sendPaymentReminderNotification(payload);
                break;
            default:
                LOG.infof("Unhandled split bill event type: %s", eventType);
        }
    }

    private void sendSplitBillInvitationNotification(String payload) {
        String title = "Undangan Pembagian Tagihan";
        String splitBillId = extractValue(payload, "splitBillId");
        String splitBillTitle = extractValue(payload, "title");
        String totalAmount = extractValue(payload, "totalAmount");
        String currency = extractValue(payload, "currency");
        
        String body = String.format(
                "Anda telah diundang untuk pembagian tagihan: %s\n" +
                "Total Tagihan: %s %s\n" +
                "Silakan login ke aplikasi PayU untuk menerima atau menolak undangan.",
                splitBillTitle, currency, totalAmount
        );
        
        LOG.infof("Sending split bill invitation notification: splitBillId=%s", splitBillId);
    }

    private void sendParticipantAddedNotification(String payload) {
        String creatorAccountId = extractValue(payload, "creatorAccountId");
        String title = "Peserta Baru Ditambahkan";
        String accountName = extractValue(payload, "accountName");
        
        String body = String.format(
                "Anda telah menambahkan peserta baru: %s ke pembagian tagihan.",
                accountName
        );
        
        LOG.infof("Sending participant added notification: accountId=%s", creatorAccountId);
    }

    private void sendPaymentMadeNotification(String payload) {
        String accountId = extractValue(payload, "accountId");
        String accountName = extractValue(payload, "accountName");
        String paymentAmount = extractValue(payload, "paymentAmount");
        String currency = extractValue(payload, "currency");
        
        String title = "Pembayaran Tagihan Diterima";
        String body = String.format(
                "%s telah membayar %s %s untuk pembagian tagihan.",
                accountName, currency, paymentAmount
        );
        
        LOG.infof("Sending payment made notification: accountId=%s", accountId);
    }

    private void sendSplitBillCompletedNotification(String payload) {
        String creatorAccountId = extractValue(payload, "creatorAccountId");
        String title = "Pembagian Tagihan Selesai";
        String splitBillTitle = extractValue(payload, "referenceNumber");
        String totalAmount = extractValue(payload, "totalAmount");
        String currency = extractValue(payload, "currency");
        
        String body = String.format(
                "Pembagian tagihan %s (%s %s) telah selesai. Semua peserta telah membayar tagihan mereka.",
                splitBillTitle, currency, totalAmount
        );
        
        LOG.infof("Sending split bill completed notification: accountId=%s", creatorAccountId);
    }

    private void sendPaymentReminderNotification(String payload) {
        String accountId = extractValue(payload, "accountId");
        String accountName = extractValue(payload, "accountName");
        String amountOwed = extractValue(payload, "amountOwed");
        String currency = extractValue(payload, "currency");
        String splitBillTitle = extractValue(payload, "referenceNumber");
        
        String title = "Pengingat Pembayaran Tagihan";
        String body = String.format(
                "Halo %s, Anda memiliki tagihan yang belum dibayar untuk %s.\n" +
                "Jumlah Tagihan: %s %s\n" +
                "Silakan segera lakukan pembayaran.",
                accountName, splitBillTitle, currency, amountOwed
        );
        
        LOG.infof("Sending payment reminder notification: accountId=%s", accountId);
    }

    private String extractEventType(String payload) {
        return extractValue(payload, "eventType");
    }

    private String extractValue(String payload, String key) {
        try {
            int keyIndex = payload.indexOf("\"" + key + "\"");
            if (keyIndex == -1) return "";
            int valueStart = payload.indexOf(":", keyIndex);
            if (valueStart == -1) return "";
            valueStart = payload.indexOf("\"", valueStart);
            if (valueStart == -1) return "";
            int valueEnd = payload.indexOf("\"", valueStart + 1);
            if (valueEnd == -1) return "";
            return payload.substring(valueStart + 1, valueEnd);
        } catch (Exception e) {
            LOG.errorf("Failed to extract value for key %s: %s", key, e.getMessage());
            return "";
        }
    }
}
