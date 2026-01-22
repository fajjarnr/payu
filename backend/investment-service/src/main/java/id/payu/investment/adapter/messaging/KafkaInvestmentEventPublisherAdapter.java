package id.payu.investment.adapter.messaging;

import id.payu.investment.domain.port.out.InvestmentEventPublisherPort;
import id.payu.investment.dto.InvestmentEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class KafkaInvestmentEventPublisherAdapter implements InvestmentEventPublisherPort {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private static final String TOPIC = "investment-events";

    @Override
    public void publishInvestmentCreated(InvestmentEvent event) {
        try {
            kafkaTemplate.send(TOPIC, event);
            log.info("Published investment created event: {}", event.id());
        } catch (Exception e) {
            log.error("Failed to publish investment created event", e);
        }
    }

    @Override
    public void publishInvestmentCompleted(InvestmentEvent event) {
        try {
            kafkaTemplate.send(TOPIC, event);
            log.info("Published investment completed event: {}", event.id());
        } catch (Exception e) {
            log.error("Failed to publish investment completed event", e);
        }
    }

    @Override
    public void publishInvestmentFailed(InvestmentEvent event) {
        try {
            kafkaTemplate.send(TOPIC, event);
            log.info("Published investment failed event: {}", event.id());
        } catch (Exception e) {
            log.error("Failed to publish investment failed event", e);
        }
    }
}
