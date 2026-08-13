package id.payu.billing.adapter.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import id.payu.billing.application.service.SubscriptionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SubscriptionDueEventListener Unit Tests")
class SubscriptionDueEventListenerTest {

    @Mock
    private SubscriptionService subscriptionService;

    private SubscriptionDueEventListener listener;

    @BeforeEach
    void setUp() {
        listener = new SubscriptionDueEventListener(subscriptionService, new ObjectMapper());
    }

    @Test
    @DisplayName("should parse subscriptionId from CloudEvent payload and delegate to processScheduledCharge")
    void shouldDelegateToProcessScheduledCharge() {
        UUID subId = UUID.randomUUID();
        String payload = "{\"specversion\":\"1.0.2\",\"type\":\"subscription.due\",\"data\":{\"subscriptionId\":\"" + subId + "\"}}";

        listener.onSubscriptionDue(payload);

        verify(subscriptionService).processScheduledCharge(subId);
    }

    @Test
    @DisplayName("should fall back to root-level subscriptionId")
    void shouldFallBackToRootSubscriptionId() {
        UUID subId = UUID.randomUUID();
        String payload = "{\"specversion\":\"1.0.2\",\"type\":\"subscription.due\",\"subscriptionId\":\"" + subId + "\"}";

        listener.onSubscriptionDue(payload);

        verify(subscriptionService).processScheduledCharge(subId);
    }

    @Test
    @DisplayName("should propagate parsing failure so the record goes to DLQ")
    void shouldThrowOnMissingSubscriptionId() {
        assertThatThrownBy(() -> listener.onSubscriptionDue("{\"specversion\":\"1.0.2\"}"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Scheduled billing execution failed")
                .hasCauseInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("should propagate service failure for retry + DLQ")
    void shouldThrowWhenServiceFails() {
        UUID subId = UUID.randomUUID();
        doThrow(new IllegalStateException("DB unavailable"))
                .when(subscriptionService).processScheduledCharge(subId);
        String payload = "{\"data\":{\"subscriptionId\":\"" + subId + "\"}}";

        assertThatThrownBy(() -> listener.onSubscriptionDue(payload))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Scheduled billing execution failed")
                .hasCauseInstanceOf(IllegalStateException.class);
        verifyNoMoreInteractions(subscriptionService);
    }

    @Test
    @DisplayName("should not throw on successful processing")
    void shouldNotThrowOnSuccess() {
        UUID subId = UUID.randomUUID();
        String payload = "{\"data\":{\"subscriptionId\":\"" + subId + "\"}}";

        listener.onSubscriptionDue(payload);

        verify(subscriptionService).processScheduledCharge(subId);
    }
}
