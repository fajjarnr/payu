package id.payu.wallet.adapter.messaging.fx;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Consumes FX rate updates from Kafka.
 * Used to break circular dependency between wallet-service and fx-service.
 *
 * @author PayU Logic Builder
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FxRateEventConsumer {

    private final FxRateCache fxRateCache;

    /**
     * Consume FX rates updated event.
     *
     * @param event the FX rates updated event
     */
    @KafkaListener(topics = "${fx.kafka.topic:fx-rates-updated}", groupId = "${spring.kafka.consumer.group-id:wallet-service-group}")
    public void onFxRatesUpdated(FxRatesUpdatedEvent event) {
        log.debug("Received FX rates updated event: {} with {} rates",
                event.getEventId(), event.getRates() != null ? event.getRates().size() : 0);

        fxRateCache.updateRates(event);

        log.info("Processed FX rates updated event, cache size: {}", fxRateCache.size());
    }
}
