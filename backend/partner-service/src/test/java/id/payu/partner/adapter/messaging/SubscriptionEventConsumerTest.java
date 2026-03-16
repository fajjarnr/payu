package id.payu.partner.adapter.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import id.payu.partner.application.service.WebhookDispatcherService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.apache.kafka.common.record.TimestampType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SubscriptionEventConsumer Unit Tests")
class SubscriptionEventConsumerTest {

    @Mock
    private WebhookDispatcherService webhookDispatcher;

    private ObjectMapper objectMapper;
    private SubscriptionEventConsumer consumer;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
        consumer = new SubscriptionEventConsumer(webhookDispatcher, objectMapper);
    }

    @Test
    @DisplayName("should consume subscription.created event and dispatch webhook")
    void shouldConsumeSubscriptionCreatedEvent() {
        // Given
        String payload = cloudEventJson("subscription.created", "sub-123", "partner-nobar");
        ConsumerRecord<String, String> record = createRecord(payload,
                "X-Event-Type", "subscription.created",
                "X-Partner-Id", "partner-nobar");

        // When
        consumer.consumeSubscriptionEvent(record);

        // Then
        ArgumentCaptor<String> typeCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Map> payloadCaptor = ArgumentCaptor.forClass(Map.class);
        verify(webhookDispatcher).dispatch(typeCaptor.capture(), payloadCaptor.capture());

        assertEquals("subscription.created", typeCaptor.getValue());
        Map<String, Object> result = payloadCaptor.getValue();
        assertEquals("partner-nobar", result.get("partnerId"));
    }

    @Test
    @DisplayName("should consume charge.succeeded event and dispatch webhook")
    void shouldConsumeChargeSucceededEvent() {
        // Given
        String payload = "{\"specversion\":\"1.0\",\"id\":\"" + UUID.randomUUID() + "\"," +
                "\"source\":\"/billing-service\",\"type\":\"charge.succeeded\"," +
                "\"time\":\"2026-03-16T00:00:00Z\"," +
                "\"data\":{\"amount\":99000,\"currency\":\"IDR\",\"status\":\"SUCCEEDED\",\"partnerId\":\"partner-nobar\"}}";
        ConsumerRecord<String, String> record = createRecord(payload,
                "X-Event-Type", "charge.succeeded",
                "X-Partner-Id", "partner-nobar");

        // When
        consumer.consumeSubscriptionEvent(record);

        // Then
        ArgumentCaptor<String> typeCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Map> payloadCaptor = ArgumentCaptor.forClass(Map.class);
        verify(webhookDispatcher).dispatch(typeCaptor.capture(), payloadCaptor.capture());

        assertEquals("charge.succeeded", typeCaptor.getValue());
        Map<String, Object> result = payloadCaptor.getValue();
        assertEquals("partner-nobar", result.get("partnerId"));
        assertEquals(99000, result.get("amount"));
    }

    @Test
    @DisplayName("should consume charge.failed event and dispatch webhook")
    void shouldConsumeChargeFailedEvent() {
        // Given
        String payload = "{\"specversion\":\"1.0\",\"id\":\"" + UUID.randomUUID() + "\"," +
                "\"source\":\"/billing-service\",\"type\":\"charge.failed\"," +
                "\"time\":\"2026-03-16T00:00:00Z\"," +
                "\"data\":{\"amount\":99000,\"status\":\"FAILED\",\"failureReason\":\"Insufficient balance\",\"partnerId\":\"partner-nobar\"}}";
        ConsumerRecord<String, String> record = createRecord(payload,
                "X-Event-Type", "charge.failed",
                "X-Partner-Id", "partner-nobar");

        // When
        consumer.consumeSubscriptionEvent(record);

        // Then
        ArgumentCaptor<String> typeCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Map> payloadCaptor = ArgumentCaptor.forClass(Map.class);
        verify(webhookDispatcher).dispatch(typeCaptor.capture(), payloadCaptor.capture());

        assertEquals("charge.failed", typeCaptor.getValue());
        Map<String, Object> result = payloadCaptor.getValue();
        assertEquals("Insufficient balance", result.get("failureReason"));
    }

    @Test
    @DisplayName("should extract event type from event when header is null")
    void shouldExtractEventTypeFromEvent() {
        // Given - no headers
        String payload = cloudEventJson("subscription.created", "sub-123", "partner-nobar");
        ConsumerRecord<String, String> record = createRecord(payload);

        // When
        consumer.consumeSubscriptionEvent(record);

        // Then
        verify(webhookDispatcher).dispatch(eq("subscription.created"), any());
    }

    @Test
    @DisplayName("should extract partnerId from event data when header is null")
    void shouldExtractPartnerIdFromEventData() {
        // Given
        String payload = cloudEventJson("subscription.created", "sub-123", "partner-nobar");
        ConsumerRecord<String, String> record = createRecord(payload);

        // When
        consumer.consumeSubscriptionEvent(record);

        // Then
        ArgumentCaptor<Map> payloadCaptor = ArgumentCaptor.forClass(Map.class);
        verify(webhookDispatcher).dispatch(any(), payloadCaptor.capture());
        assertEquals("partner-nobar", payloadCaptor.getValue().get("partnerId"));
    }

    @Test
    @DisplayName("should handle null/empty value gracefully")
    void shouldHandleNullEvent() {
        // Given
        ConsumerRecord<String, String> record = new ConsumerRecord<>(
                "subscription.events", 0, 0, null, null);

        // When
        consumer.consumeSubscriptionEvent(record);

        // Then - should not throw and not call dispatcher
        verify(webhookDispatcher, never()).dispatch(any(), any());
    }

    @Test
    @DisplayName("should handle empty value gracefully")
    void shouldHandleEmptyEvent() {
        // Given
        ConsumerRecord<String, String> record = new ConsumerRecord<>(
                "subscription.events", 0, 0, null, "");

        // When
        consumer.consumeSubscriptionEvent(record);

        // Then - should not throw and not call dispatcher
        verify(webhookDispatcher, never()).dispatch(any(), any());
    }

    @Test
    @DisplayName("should handle dispatcher exception gracefully")
    void shouldHandleDispatcherException() {
        // Given
        String payload = cloudEventJson("subscription.created", "sub-123", "partner-nobar");
        ConsumerRecord<String, String> record = createRecord(payload,
                "X-Event-Type", "subscription.created");
        doThrow(new RuntimeException("Dispatch failed"))
                .when(webhookDispatcher).dispatch(any(), any());

        // When - should not throw
        assertDoesNotThrow(() -> consumer.consumeSubscriptionEvent(record));

        // Then - dispatcher was called
        verify(webhookDispatcher).dispatch(any(), any());
    }

    @Test
    @DisplayName("should handle plain JSON payload (non-CloudEvent)")
    void shouldHandlePlainJsonPayload() {
        // Given
        String payload = "{\"subscriptionId\":\"sub-123\",\"partnerId\":\"partner-nobar\"," +
                "\"amount\":99000,\"type\":\"subscription.renewed\"}";
        ConsumerRecord<String, String> record = createRecord(payload);

        // When
        consumer.consumeSubscriptionEvent(record);

        // Then
        ArgumentCaptor<String> typeCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Map> payloadCaptor = ArgumentCaptor.forClass(Map.class);
        verify(webhookDispatcher).dispatch(typeCaptor.capture(), payloadCaptor.capture());

        assertEquals("subscription.renewed", typeCaptor.getValue());
        Map<String, Object> result = payloadCaptor.getValue();
        assertEquals("partner-nobar", result.get("partnerId"));
        assertEquals(99000, result.get("amount"));
    }

    // --- Helper methods ---

    private String cloudEventJson(String type, String subject, String partnerId) {
        return "{\"specversion\":\"1.0\",\"id\":\"" + UUID.randomUUID() + "\"," +
                "\"source\":\"/billing-service\",\"type\":\"" + type + "\"," +
                "\"subject\":\"" + subject + "\"," +
                "\"time\":\"2026-03-16T00:00:00Z\"," +
                "\"data\":{\"subscriptionId\":\"" + subject + "\"," +
                "\"partnerId\":\"" + partnerId + "\"," +
                "\"accountId\":\"acc-123\",\"planId\":\"plan-456\"}}";
    }

    private ConsumerRecord<String, String> createRecord(String value, String... headerPairs) {
        RecordHeaders headers = new RecordHeaders();
        for (int i = 0; i + 1 < headerPairs.length; i += 2) {
            headers.add(headerPairs[i], headerPairs[i + 1].getBytes(StandardCharsets.UTF_8));
        }
        return new ConsumerRecord<>(
                "subscription.events", 0, 0,
                ConsumerRecord.NO_TIMESTAMP, TimestampType.NO_TIMESTAMP_TYPE,
                0, 0, null, value, headers, Optional.empty());
    }
}
