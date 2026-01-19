package id.payu.notification.consumer;

import id.payu.notification.domain.NotificationChannel;
import id.payu.notification.dto.SendNotificationRequest;
import id.payu.notification.service.NotificationService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.jboss.logging.Logger;

/**
 * Kafka consumer for wallet, transaction, and payment events.
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
}
