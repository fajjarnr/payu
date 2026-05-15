package id.payu.promotion.adapter.messaging;

import id.payu.promotion.domain.model.CashbackNotification;
import id.payu.promotion.domain.port.out.NotificationPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Adapter for sending notifications via Kafka.
 * Publishes notification events to be consumed by notification-service.
 */
@Component
public class KafkaNotificationAdapter implements NotificationPort {

    private static final Logger LOG = LoggerFactory.getLogger(KafkaNotificationAdapter.class);

    private final KafkaTemplate<String, Map<String, Object>> kafkaTemplate;
    private final String notificationTopic;

    public KafkaNotificationAdapter(
            KafkaTemplate<String, Map<String, Object>> kafkaTemplate,
            @Value("${app.kafka.topics.notifications:notifications}") String notificationTopic) {
        this.kafkaTemplate = kafkaTemplate;
        this.notificationTopic = notificationTopic;
    }

    @Override
    public void sendCashbackNotification(CashbackNotification notification) {
        LOG.info("Sending cashback notification: accountId={}, amount={}",
                notification.getAccountId(), notification.getAmount());

        Map<String, Object> event = Map.of(
                "type", "CASHBACK",
                "accountId", notification.getAccountId(),
                "transactionId", notification.getTransactionId(),
                "amount", notification.getAmount().toString(),
                "message", notification.getMessage(),
                "timestamp", notification.getTimestamp().toString()
        );

        try {
            kafkaTemplate.send(notificationTopic, notification.getAccountId(), event);
            LOG.debug("CashbackEntity notification sent successfully");
        } catch (Exception e) {
            LOG.error("Failed to send cashback notification: {}", e.getMessage(), e);
            // Don't throw - notification failure shouldn't fail the cashback
        }
    }
}
