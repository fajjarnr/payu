package id.payu.fx.adapter.messaging;

import id.payu.fx.application.service.FxRateService;
import id.payu.fx.domain.model.FxRate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Publishes FX rate updates to Kafka.
 * Used to break circular dependency between fx-service and wallet-service.
 *
 * @author PayU Logic Builder
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FxRateEventPublisher {

    private final FxRateService fxRateService;
    private final KafkaTemplate<String, FxRatesUpdatedEvent> kafkaTemplate;

    @Value("${fx.kafka.topic:fx-rates-updated}")
    private String topicName;

    @Value("${fx.publisher.enabled:true}")
    private boolean enabled;

    /**
     * Publish FX rates every minute to keep consumers updated.
     */
    @Scheduled(fixedRate = 60000)
    public void publishFxRates() {
        if (!enabled) {
            return;
        }

        try {
            List<FxRate> rates = fxRateService.getAllRates();

            if (rates.isEmpty()) {
                log.debug("No FX rates to publish");
                return;
            }

            List<FxRatesUpdatedEvent.FxRateDto> rateDtos = rates.stream()
                    .map(FxRatesUpdatedEvent::fromDomain)
                    .collect(Collectors.toList());

            FxRatesUpdatedEvent event = FxRatesUpdatedEvent.builder()
                    .eventId(UUID.randomUUID().toString())
                    .timestamp(Instant.now())
                    .rates(rateDtos)
                    .baseCurrency("IDR")
                    .build();

            kafkaTemplate.send(topicName, event)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.error("Failed to publish FX rates event: {}", ex.getMessage());
                        } else {
                            log.debug("Published FX rates event with {} rates to topic {}",
                                    rateDtos.size(), topicName);
                        }
                    });

        } catch (Exception e) {
            log.error("Error publishing FX rates: {}", e.getMessage(), e);
        }
    }

    /**
     * Publish FX rates immediately (for manual refresh).
     */
    public void publishFxRatesImmediately() {
        publishFxRates();
    }
}
