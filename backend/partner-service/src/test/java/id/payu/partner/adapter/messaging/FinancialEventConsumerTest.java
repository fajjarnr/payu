package id.payu.partner.adapter.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import id.payu.partner.application.service.WebhookDispatcherService;
import java.util.Map;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

/**
 * PARTNER-PROD-004: a consumer must never swallow a processing exception and
 * commit the offset — the record must be rethrown so the Kafka error handler
 * retries it and forwards it to the topic DLQ.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("FinancialEventConsumer durability")
class FinancialEventConsumerTest {

    @Mock
    private WebhookDispatcherService webhookDispatcher;

    private ObjectMapper objectMapper;
    private FinancialEventConsumer consumer;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
        consumer = new FinancialEventConsumer(webhookDispatcher, objectMapper);
    }

    private ConsumerRecord<String, String> record(String topic, String value) {
        return new ConsumerRecord<>(topic, 0, 0L, "key", value);
    }

    @Test
    @DisplayName("should dispatch webhook for a valid CloudEvent")
    void shouldDispatchForValidCloudEvent() {
        String payload = "{\"specversion\":\"1.0\",\"id\":\"evt-1\",\"source\":\"/wallet-service\"," +
                "\"type\":\"wallet.balance.changed\",\"time\":\"2026-03-16T00:00:00Z\"," +
                "\"data\":{\"amount\":1000,\"currency\":\"IDR\"}}";

        consumer.consumeFinancialEvent(record("wallet.balance.changed", payload));

        ArgumentCaptor<String> typeCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> idCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Map> payloadCaptor = ArgumentCaptor.forClass(Map.class);
        verify(webhookDispatcher).dispatch(typeCaptor.capture(), idCaptor.capture(), payloadCaptor.capture());

        assertEquals("wallet.balance.changed", typeCaptor.getValue());
        assertEquals("evt-1", idCaptor.getValue());
        assertEquals(1000, payloadCaptor.getValue().get("amount"));
    }

    @Test
    @DisplayName("should map versioned split-bills topics (TX-001)")
    void shouldMapVersionedSplitBillsTopics() {
        assertEquals("split-bill.created",
                consumer.deriveEventType("payu.transaction.split-bill-created.v1", record("payu.transaction.split-bill-created.v1", "{}")));
        assertEquals("split-bill.activated",
                consumer.deriveEventType("payu.transaction.split-bill-activated.v1", record("payu.transaction.split-bill-activated.v1", "{}")));
        assertEquals("split-bill.cancelled",
                consumer.deriveEventType("payu.transaction.split-bill-cancelled.v1", record("payu.transaction.split-bill-cancelled.v1", "{}")));
        assertEquals("split-bill.payment.made",
                consumer.deriveEventType("payu.transaction.payment-made.v1", record("payu.transaction.payment-made.v1", "{}")));
        assertEquals("split-bill.completed",
                consumer.deriveEventType("payu.transaction.split-bill-completed.v1", record("payu.transaction.split-bill-completed.v1", "{}")));
    }

    @Test
    @DisplayName("should rethrow on malformed payload so the record reaches the DLQ")
    void shouldRethrowOnMalformedPayload() {
        assertThrows(Exception.class,
                () -> consumer.consumeFinancialEvent(record("wallet.balance.changed", "{not-json")));
    }

    @Test
    @DisplayName("should rethrow when webhook dispatch fails so the record reaches the DLQ")
    void shouldRethrowWhenDispatchFails() {
        doThrow(new IllegalStateException("dispatch failed"))
                .when(webhookDispatcher).dispatch(any(String.class), any(String.class), any(Map.class));

        assertThrows(IllegalStateException.class,
                () -> consumer.consumeFinancialEvent(record("wallet.balance.changed",
                        "{\"specversion\":\"1.0\",\"id\":\"evt-2\",\"type\":\"wallet.balance.changed\","
                                + "\"time\":\"2026-03-16T00:00:00Z\",\"data\":{}}")));
    }
}
