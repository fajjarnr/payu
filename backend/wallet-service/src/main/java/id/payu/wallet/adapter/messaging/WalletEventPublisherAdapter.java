package id.payu.wallet.adapter.messaging;

import id.payu.wallet.domain.port.out.WalletEventPublisherPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Kafka adapter for publishing wallet events.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WalletEventPublisherAdapter implements WalletEventPublisherPort {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    private static final String TOPIC_BALANCE_CHANGED = "wallet.balance.changed";
    private static final String TOPIC_BALANCE_RESERVED = "wallet.balance.reserved";
    private static final String TOPIC_RESERVATION_COMMITTED = "wallet.reservation.committed";
    private static final String TOPIC_RESERVATION_RELEASED = "wallet.reservation.released";

    @Override
    public void publishBalanceChanged(String accountId, BigDecimal newBalance, BigDecimal availableBalance) {
        Map<String, Object> event = Map.of(
                "accountId", accountId,
                "newBalance", newBalance,
                "availableBalance", availableBalance,
                "timestamp", LocalDateTime.now().toString()
        );
        sendEvent(TOPIC_BALANCE_CHANGED, accountId, event);
    }

    @Override
    public void publishBalanceReserved(String accountId, String reservationId, BigDecimal amount) {
        Map<String, Object> event = Map.of(
                "accountId", accountId,
                "reservationId", reservationId,
                "amount", amount,
                "timestamp", LocalDateTime.now().toString()
        );
        sendEvent(TOPIC_BALANCE_RESERVED, accountId, event);
    }

    @Override
    public void publishReservationCommitted(String accountId, String reservationId, BigDecimal amount) {
        Map<String, Object> event = Map.of(
                "accountId", accountId,
                "reservationId", reservationId,
                "amount", amount,
                "timestamp", LocalDateTime.now().toString()
        );
        sendEvent(TOPIC_RESERVATION_COMMITTED, accountId, event);
    }

    @Override
    public void publishReservationReleased(String accountId, String reservationId, BigDecimal amount) {
        Map<String, Object> event = Map.of(
                "accountId", accountId,
                "reservationId", reservationId,
                "amount", amount,
                "timestamp", LocalDateTime.now().toString()
        );
        sendEvent(TOPIC_RESERVATION_RELEASED, accountId, event);
    }

    private void sendEvent(String topic, String key, Object event) {
        try {
            kafkaTemplate.send(topic, key, event);
            log.debug("Published event to topic {}: {}", topic, event);
        } catch (Exception e) {
            log.error("Failed to publish event to topic {}: {}", topic, e.getMessage());
        }
    }
}
