package id.payu.account.adapter.messaging;

import id.payu.account.domain.port.out.UserEventPublisherPort;
import id.payu.account.dto.UserCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaUserEventPublisherAdapter implements UserEventPublisherPort {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private static final String TOPIC = "user.created";

    @Override
    public void publishUserCreated(UserCreatedEvent event) {
        log.info("Publishing UserCreatedEvent: {}", event);
        try {
            kafkaTemplate.send(TOPIC, event.userId().toString(), event);
        } catch (Exception e) {
            log.error("Failed to publish UserCreatedEvent", e);
            // In a real scenario, we might want to throw an exception or use an outbox
            // pattern
        }
    }
}
