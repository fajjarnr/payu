package id.payu.partner.adapter.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import id.payu.events.cloudevents.CloudEventEnvelope;
import id.payu.partner.application.service.WebhookDispatcherService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.net.URI;
import java.time.OffsetDateTime;
import java.util.Map;
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
        CloudEventEnvelope<Map<String, Object>> event = createSampleEvent(
                "subscription.created", "sub-123", "partner-nobar");

        // When
        consumer.consumeSubscriptionEvent(event, "subscription.created", "partner-nobar");

        // Then
        ArgumentCaptor<String> typeCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Map> payloadCaptor = ArgumentCaptor.forClass(Map.class);
        verify(webhookDispatcher).dispatch(typeCaptor.capture(), payloadCaptor.capture());

        assertEquals("subscription.created", typeCaptor.getValue());
        Map<String, Object> payload = payloadCaptor.getValue();
        assertNotNull(payload.get("eventId"));
        assertNotNull(payload.get("eventTime"));
        assertEquals("partner-nobar", payload.get("partnerId"));
    }

    @Test
    @DisplayName("should consume charge.succeeded event and dispatch webhook")
    void shouldConsumeChargeSucceededEvent() {
        // Given
        CloudEventEnvelope<Map<String, Object>> event = createSampleEvent(
                "charge.succeeded", "charge-456", "partner-nobar");
        event.getData().put("amount", new BigDecimal("99000"));
        event.getData().put("currency", "IDR");
        event.getData().put("status", "SUCCEEDED");

        // When
        consumer.consumeSubscriptionEvent(event, "charge.succeeded", "partner-nobar");

        // Then
        ArgumentCaptor<String> typeCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Map> payloadCaptor = ArgumentCaptor.forClass(Map.class);
        verify(webhookDispatcher).dispatch(typeCaptor.capture(), payloadCaptor.capture());

        assertEquals("charge.succeeded", typeCaptor.getValue());
        Map<String, Object> payload = payloadCaptor.getValue();
        assertEquals("partner-nobar", payload.get("partnerId"));
        assertEquals(new BigDecimal("99000"), payload.get("amount"));
    }

    @Test
    @DisplayName("should consume charge.failed event and dispatch webhook")
    void shouldConsumeChargeFailedEvent() {
        // Given
        CloudEventEnvelope<Map<String, Object>> event = createSampleEvent(
                "charge.failed", "charge-789", "partner-nobar");
        event.getData().put("amount", new BigDecimal("99000"));
        event.getData().put("status", "FAILED");
        event.getData().put("failureReason", "Insufficient balance");

        // When
        consumer.consumeSubscriptionEvent(event, "charge.failed", "partner-nobar");

        // Then
        ArgumentCaptor<String> typeCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Map> payloadCaptor = ArgumentCaptor.forClass(Map.class);
        verify(webhookDispatcher).dispatch(typeCaptor.capture(), payloadCaptor.capture());

        assertEquals("charge.failed", typeCaptor.getValue());
        Map<String, Object> payload = payloadCaptor.getValue();
        assertEquals("partner-nobar", payload.get("partnerId"));
        assertEquals("Insufficient balance", payload.get("failureReason"));
    }

    @Test
    @DisplayName("should extract event type from event when header is null")
    void shouldExtractEventTypeFromEvent() {
        // Given
        CloudEventEnvelope<Map<String, Object>> event = createSampleEvent(
                "subscription.created", "sub-123", "partner-nobar");

        // When - pass null for eventType header
        consumer.consumeSubscriptionEvent(event, null, "partner-nobar");

        // Then
        verify(webhookDispatcher).dispatch(eq("subscription.created"), any());
    }

    @Test
    @DisplayName("should extract partnerId from event data when header is null")
    void shouldExtractPartnerIdFromEventData() {
        // Given
        CloudEventEnvelope<Map<String, Object>> event = createSampleEvent(
                "subscription.created", "sub-123", "partner-nobar");

        // When - pass null for partnerId header
        consumer.consumeSubscriptionEvent(event, "subscription.created", null);

        // Then
        ArgumentCaptor<Map> payloadCaptor = ArgumentCaptor.forClass(Map.class);
        verify(webhookDispatcher).dispatch(any(), payloadCaptor.capture());
        assertEquals("partner-nobar", payloadCaptor.getValue().get("partnerId"));
    }

    @Test
    @DisplayName("should handle null event gracefully")
    void shouldHandleNullEvent() {
        // When
        consumer.consumeSubscriptionEvent(null, "subscription.created", "partner-nobar");

        // Then - should not throw and not call dispatcher
        verify(webhookDispatcher, never()).dispatch(any(), any());
    }

    @Test
    @DisplayName("should handle event with null type gracefully")
    void shouldHandleEventWithNullType() {
        // Given
        CloudEventEnvelope<Map<String, Object>> event = createSampleEvent(
                null, "sub-123", "partner-nobar");

        // When
        consumer.consumeSubscriptionEvent(event, null, "partner-nobar");

        // Then - should not throw and not call dispatcher
        verify(webhookDispatcher, never()).dispatch(any(), any());
    }

    @Test
    @DisplayName("should handle dispatcher exception gracefully")
    void shouldHandleDispatcherException() {
        // Given
        CloudEventEnvelope<Map<String, Object>> event = createSampleEvent(
                "subscription.created", "sub-123", "partner-nobar");
        doThrow(new RuntimeException("Dispatch failed"))
                .when(webhookDispatcher).dispatch(any(), any());

        // When - should not throw
        assertDoesNotThrow(() ->
                consumer.consumeSubscriptionEvent(event, "subscription.created", "partner-nobar"));

        // Then - dispatcher was called
        verify(webhookDispatcher).dispatch(any(), any());
    }

    @Test
    @DisplayName("should convert POJO payload to Map correctly")
    void shouldConvertPojoPayloadToMap() {
        // Given
        TestPayload pojoPayload = new TestPayload();
        pojoPayload.setSubscriptionId("sub-123");
        pojoPayload.setPartnerId("partner-nobar");
        pojoPayload.setAmount(new BigDecimal("99000"));

        CloudEventEnvelope<TestPayload> event = CloudEventEnvelope.<TestPayload>builder()
                .id(UUID.randomUUID())
                .source(URI.create("/billing-service"))
                .type("subscription.created")
                .subject("sub-123")
                .time(OffsetDateTime.now())
                .data(pojoPayload)
                .build();

        // When
        consumer.consumeSubscriptionEvent(event, "subscription.created", "partner-nobar");

        // Then
        ArgumentCaptor<Map> payloadCaptor = ArgumentCaptor.forClass(Map.class);
        verify(webhookDispatcher).dispatch(any(), payloadCaptor.capture());

        Map<String, Object> payload = payloadCaptor.getValue();
        assertEquals("sub-123", payload.get("subscriptionId"));
        assertEquals("partner-nobar", payload.get("partnerId"));
        assertEquals(new BigDecimal("99000"), payload.get("amount"));
    }

    // Helper methods and classes

    private CloudEventEnvelope<Map<String, Object>> createSampleEvent(
            String type, String subject, String partnerId) {
        Map<String, Object> data = Map.of(
                "subscriptionId", subject,
                "partnerId", partnerId,
                "accountId", "acc-123",
                "planId", "plan-456"
        );

        return CloudEventEnvelope.<Map<String, Object>>builder()
                .id(UUID.randomUUID())
                .source(URI.create("/billing-service"))
                .type(type)
                .subject(subject)
                .time(OffsetDateTime.now())
                .data(data)
                .build();
    }

    public static class TestPayload {
        private String subscriptionId;
        private String partnerId;
        private BigDecimal amount;

        public String getSubscriptionId() { return subscriptionId; }
        public void setSubscriptionId(String subscriptionId) { this.subscriptionId = subscriptionId; }
        public String getPartnerId() { return partnerId; }
        public void setPartnerId(String partnerId) { this.partnerId = partnerId; }
        public BigDecimal getAmount() { return amount; }
        public void setAmount(BigDecimal amount) { this.amount = amount; }
    }
}
