package id.payu.promotion.adapter.messaging;

import id.payu.outbox.service.OutboxService;
import id.payu.promotion.domain.model.CashbackNotification;
import id.payu.promotion.domain.port.out.NotificationPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Outbox-based adapter for sending notifications via Kafka.
 * <p>
 * MSG-008: Migrated from direct KafkaTemplate.send() to OutboxService.createEvent()
 * for transactional atomicity between business data and event publishing.
 *
 * @author PayU Digital Banking Platform
 * @since 1.8.8
 */
@Component
public class KafkaNotificationAdapter implements NotificationPort {

    private static final Logger LOG = LoggerFactory.getLogger(KafkaNotificationAdapter.class);

    private final OutboxService outboxService;
    private static final String NOTIFICATION_TOPIC = "payu.promotion.notification.v1";
    private static final String AGGREGATE_TYPE = "Promotion";

    public KafkaNotificationAdapter(OutboxService outboxService) {
        this.outboxService = outboxService;
    }

    @Override
    public void sendCashbackNotification(CashbackNotification notification) {
        LOG.info("Creating outbox event for cashback notification: accountId={}, amount={}",
                notification.getAccountId(), notification.getAmount());

        outboxService.createEvent(
                AGGREGATE_TYPE,
                notification.getAccountId(),
                "CashbackNotification",
                Map.of(
                        "type", "CASHBACK",
                        "accountId", notification.getAccountId(),
                        "transactionId", notification.getTransactionId(),
                        "amount", notification.getAmount().toString(),
                        "message", notification.getMessage(),
                        "timestamp", notification.getTimestamp().toString()
                ),
                null,
                NOTIFICATION_TOPIC
        );
    }
}
