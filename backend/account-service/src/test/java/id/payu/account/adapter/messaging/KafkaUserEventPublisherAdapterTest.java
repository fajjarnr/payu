package id.payu.account.adapter.messaging;

import id.payu.account.dto.UserCreatedEvent;
import id.payu.outbox.service.OutboxService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * ACCOUNT-004: the user-created outbox payload must not carry PII (email,
 * fullName) to Kafka.
 */
@DisplayName("KafkaUserEventPublisherAdapter payload")
class KafkaUserEventPublisherAdapterTest {

    private OutboxService outboxService;
    private KafkaUserEventPublisherAdapter adapter;

    @BeforeEach
    void setUp() {
        outboxService = mock(OutboxService.class);
        adapter = new KafkaUserEventPublisherAdapter(outboxService);
    }

    @Test
    @DisplayName("publishes only stable identifiers, no PII")
    void payloadCarriesNoPii() {
        UUID userId = UUID.randomUUID();
        adapter.publishUserCreated(new UserCreatedEvent(
                userId, "iam-external-id", LocalDateTime.of(2026, 8, 11, 10, 0)));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> payloadCaptor = ArgumentCaptor.forClass(Map.class);
        verify(outboxService).createEvent(
                org.mockito.ArgumentMatchers.eq("User"),
                org.mockito.ArgumentMatchers.eq(userId.toString()),
                org.mockito.ArgumentMatchers.eq("UserCreated"),
                payloadCaptor.capture(),
                org.mockito.ArgumentMatchers.isNull(),
                org.mockito.ArgumentMatchers.eq("payu.account.user-created.v1"));

        Map<String, Object> payload = payloadCaptor.getValue();
        assertThat(payload).containsKeys("userId", "externalId", "createdAt");
        assertThat(payload).doesNotContainKeys("email", "fullName", "phoneNumber", "username", "nik");
        assertThat(payload.get("externalId")).isEqualTo("iam-external-id");
    }

    @Test
    @DisplayName("publishUserUpdated targets user-updated topic with UserUpdated type")
    void publishUserUpdatedUsesCorrectTopicAndType() {
        UUID userId = UUID.randomUUID();
        adapter.publishUserUpdated(new UserCreatedEvent(
                userId, "iam-external-id", LocalDateTime.of(2026, 8, 11, 10, 0)));

        verify(outboxService).createEvent(
                org.mockito.ArgumentMatchers.eq("User"),
                org.mockito.ArgumentMatchers.eq(userId.toString()),
                org.mockito.ArgumentMatchers.eq("UserUpdated"),
                org.mockito.ArgumentMatchers.anyMap(),
                org.mockito.ArgumentMatchers.isNull(),
                org.mockito.ArgumentMatchers.eq("payu.account.user-updated.v1"));
    }

    @Test
    @DisplayName("publishKycCompleted targets kyc-completed topic with KycCompleted type")
    void publishKycCompletedUsesCorrectTopicAndType() {
        UUID userId = UUID.randomUUID();
        adapter.publishKycCompleted(new UserCreatedEvent(
                userId, "iam-external-id", LocalDateTime.of(2026, 8, 11, 10, 0)));

        verify(outboxService).createEvent(
                org.mockito.ArgumentMatchers.eq("User"),
                org.mockito.ArgumentMatchers.eq(userId.toString()),
                org.mockito.ArgumentMatchers.eq("KycCompleted"),
                org.mockito.ArgumentMatchers.anyMap(),
                org.mockito.ArgumentMatchers.isNull(),
                org.mockito.ArgumentMatchers.eq("payu.account.kyc-completed.v1"));
    }
}
