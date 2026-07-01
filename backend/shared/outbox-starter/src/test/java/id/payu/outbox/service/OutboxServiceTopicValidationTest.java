package id.payu.outbox.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import id.payu.outbox.entity.OutboxEvent;
import id.payu.outbox.repository.OutboxRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * GAP-31 — Reproduces missing topic-name pattern validation in {@link OutboxService#createEvent}.
 *
 * <p>Bug: {@code OutboxService.createEvent(destinationTopic, ...)} accepts any string as the
 * destination topic. This breaks the platform contract from AGENTS.md rule #4:
 * <em>"topic payu.&lt;domain&gt;.&lt;event-type&gt;.v&lt;n&gt;, DLQ suffix .dlq"</em>.
 * Developers can publish to non-conforming topics (e.g. {@code totally-invalid},
 * {@code payu.Wallet.credited.v1}, {@code payu.wallet.credited}) with no enforcement at
 * the boundary.</p>
 *
 * <p>This test asserts:
 * <ul>
 *   <li>valid {@code payu.<domain>.<event>.v<n>} topics pass</li>
 *   <li>valid DLQ topics {@code payu.<domain>.<event>.v<n>.dlq} pass</li>
 *   <li>{@code null} destinationTopic is allowed (default topic used)</li>
 *   <li>any other format throws {@link IllegalArgumentException} with a clear message</li>
 * </ul>
 * </p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("OutboxService — destinationTopic pattern validation (GAP-31)")
class OutboxServiceTopicValidationTest {

    private static final String AGGREGATE_TYPE = "Wallet";
    private static final String AGGREGATE_ID = "wallet-001";
    private static final String EVENT_TYPE = "WalletCredited";
    private static final Map<String, Object> PAYLOAD = Map.of("amount", 50000, "currency", "IDR");

    @Mock
    private OutboxRepository outboxRepository;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private OutboxService outboxService;

    private void stubSave() {
        when(outboxRepository.save(any(OutboxEvent.class))).thenAnswer(invocation -> {
            OutboxEvent event = invocation.getArgument(0);
            if (event.getId() == null) {
                event.setId(UUID.randomUUID());
            }
            return event;
        });
    }

    @Nested
    @DisplayName("valid topics must be accepted")
    class ValidTopics {

        @ParameterizedTest(name = "valid: {0}")
        @ValueSource(strings = {
                "payu.wallet.credited.v1",
                "payu.account.opened.v10",
                "payu.transaction.completed.v42",
                "payu.dispute.escalated.v1",
                "payu.wallet.credited.v1.dlq",
                "payu.payment.failed.v3.dlq"
        })
        void shouldAcceptCanonicalPayuTopic(String topic) {
            stubSave();
            assertThatCode(() -> outboxService.createEvent(
                    AGGREGATE_TYPE, AGGREGATE_ID, EVENT_TYPE, PAYLOAD, null, topic))
                    .as("topic '%s' should pass pattern validation", topic)
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("null destinationTopic is allowed (default topic is used)")
        void shouldAllowNullDestinationTopic() {
            stubSave();
            assertThatCode(() -> outboxService.createEvent(
                    AGGREGATE_TYPE, AGGREGATE_ID, EVENT_TYPE, PAYLOAD, null, null))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("invalid topics must be rejected with IllegalArgumentException")
    class InvalidTopics {

        @ParameterizedTest(name = "invalid: {0}")
        @ValueSource(strings = {
                "totally-invalid-topic",       // no payu prefix
                "payu.Wallet.credited.v1",     // uppercase domain
                "payu.wallet.Credited.v1",     // uppercase event type
                "payu.wallet.credited",        // missing version segment
                "payu.wallet.credited.v",      // version without number
                "payu.wallet.credited.v1.extra", // extra segment after version
                "payu..credited.v1",           // empty domain
                "payu.wallet..v1",             // empty event type
                "payu.wallet.credited.v1.dlq.extra", // extra after dlq
                "",                            // empty string
                "payu_wallet_credited_v1",     // underscores instead of dots
                "KAFKA.TOPIC"                  // arbitrary garbage
        })
        void shouldRejectNonCanonicalTopic(String topic) {
            assertThatThrownBy(() -> outboxService.createEvent(
                    AGGREGATE_TYPE, AGGREGATE_ID, EVENT_TYPE, PAYLOAD, null, topic))
                    .as("topic '%s' must be rejected", topic)
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("payu.");
        }
    }
}
