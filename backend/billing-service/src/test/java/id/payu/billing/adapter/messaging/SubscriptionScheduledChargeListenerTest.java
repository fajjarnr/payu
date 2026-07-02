package id.payu.billing.adapter.messaging;

import id.payu.billing.application.service.SubscriptionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SubscriptionScheduledChargeListener Unit Tests")
class SubscriptionScheduledChargeListenerTest {

    @Mock
    private SubscriptionService subscriptionService;

    @InjectMocks
    private SubscriptionScheduledChargeListener listener;

    @Test
    @DisplayName("should parse UUID and delegate to processScheduledCharge")
    void shouldDelegateToProcessScheduledCharge() {
        UUID subId = UUID.randomUUID();
        listener.onScheduledBilling(subId.toString());
        verify(subscriptionService).processScheduledCharge(subId);
    }

    @Test
    @DisplayName("should throw RuntimeException on invalid UUID format")
    void shouldThrowOnInvalidUUID() {
        String invalid = "not-a-uuid";
        assertThatThrownBy(() -> listener.onScheduledBilling(invalid))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Scheduled billing execution failed");
    }

    @Test
    @DisplayName("should throw RuntimeException when service fails")
    void shouldThrowWhenServiceFails() {
        UUID subId = UUID.randomUUID();
        doThrow(new IllegalStateException("DB unavailable"))
                .when(subscriptionService).processScheduledCharge(subId);

        assertThatThrownBy(() -> listener.onScheduledBilling(subId.toString()))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Scheduled billing execution failed");
    }

    @Test
    @DisplayName("should not throw on successful processing")
    void shouldNotThrowOnSuccess() {
        UUID subId = UUID.randomUUID();
        listener.onScheduledBilling(subId.toString());
        verify(subscriptionService).processScheduledCharge(subId);
    }
}
