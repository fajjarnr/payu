package id.payu.fx.adapter.messaging;

import id.payu.fx.application.service.FxRateService;
import id.payu.fx.domain.model.FxRate;
import id.payu.outbox.service.OutboxService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Publishes FX rate updates via Outbox pattern.
 * <p>
 * MSG-012: Migrated from KafkaTemplate to OutboxService.
 * Although this is a @Scheduled broadcast, using outbox ensures
 * consistency and prevents duplicate publishes.
 *
 * @author PayU Digital Banking Platform
 * @since 1.8.8
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FxRateEventPublisher {

    private final FxRateService fxRateService;
    private final OutboxService outboxService;

    private static final String TOPIC = "payu.fx.rates-updated.v1";

    @Value("${fx.publisher.enabled:true}")
    private boolean enabled;

    /**
     * Publish FX rates every minute to keep consumers updated.
     */
    @Scheduled(fixedRate = 60000)
    @Transactional
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

            String eventId = UUID.randomUUID().toString();

            FxRatesUpdatedEvent event = FxRatesUpdatedEvent.builder()
                    .eventId(eventId)
                    .timestamp(Instant.now())
                    .rates(rateDtos)
                    .baseCurrency("IDR")
                    .build();

            outboxService.createEventFromObject(
                    "FxRate",
                    eventId,
                    "FxRatesUpdated",
                    event,
                    null,
                    TOPIC
            );

            log.debug("Created outbox event for FX rates with {} rates", rates.size());

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
