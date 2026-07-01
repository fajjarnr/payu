package id.payu.saga.config;

import id.payu.outbox.service.OutboxService;
import id.payu.saga.config.SagaAutoConfiguration.SagaEventPublisher;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * AUDIT-048 fix: {@link SagaEventPublisher} must route saga lifecycle events
 * through {@link OutboxService} (Rule #4: outbox > direct KafkaTemplate.send)
 * so events survive Kafka outages and have replay semantics.
 *
 * <p>Test mocks {@link OutboxService} and {@link KafkaTemplate} to verify the
 * publisher only writes to outbox, never to Kafka directly.</p>
 */
class SagaEventPublisherOutboxTest {

    @Test
    void shouldPublishViaOutboxWhenEventsEnabled() {
        OutboxService outboxService = mock(OutboxService.class);
        @SuppressWarnings("unchecked")
        KafkaTemplate<String, Object> kafkaTemplate = mock(KafkaTemplate.class);
        SagaProperties properties = new SagaProperties();
        properties.setEventsEnabled(true);
        properties.setEventTopic("payu.saga.events.v1");

        SagaEventPublisher publisher = new SagaEventPublisher(outboxService, properties);
        publisher.publishSagaEvent("saga-123", "SagaStarted", Map.of("step", "validate"));

        verify(outboxService).createEvent(
            eq("Saga"),
            eq("saga-123"),
            eq("SagaStarted"),
            any(),
            isNull(),
            eq("payu.saga.events.v1")
        );
        verifyNoInteractions(kafkaTemplate);
    }

    @Test
    void shouldSkipPublishingWhenEventsDisabled() {
        OutboxService outboxService = mock(OutboxService.class);
        SagaProperties properties = new SagaProperties();
        properties.setEventsEnabled(false);
        properties.setEventTopic("payu.saga.events.v1");

        SagaEventPublisher publisher = new SagaEventPublisher(outboxService, properties);
        publisher.publishSagaEvent("saga-123", "SagaStarted", Map.of("step", "validate"));

        verifyNoInteractions(outboxService);
    }

    @Test
    void shouldRouteToOutboxEvenWithComplexPayload() {
        OutboxService outboxService = mock(OutboxService.class);
        SagaProperties properties = new SagaProperties();
        properties.setEventsEnabled(true);
        properties.setEventTopic("payu.saga.events.v1");

        SagaEventPublisher publisher = new SagaEventPublisher(outboxService, properties);

        Map<String, Object> complexPayload = Map.of(
            "step", "compensation",
            "retryCount", 3,
            "errorCode", "TIMEOUT"
        );
        publisher.publishSagaEvent("saga-456", "SagaFailed", complexPayload);

        verify(outboxService).createEvent(
            eq("Saga"),
            eq("saga-456"),
            eq("SagaFailed"),
            any(),
            isNull(),
            eq("payu.saga.events.v1")
        );
    }
}
