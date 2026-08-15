package id.payu.wallet.adapter.messaging.fx;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ARCH-TOPIC-003: the fx consumer must listen on the outbox-published topic
 * {@code payu.fx.rates-updated.v1} and unwrap the CloudEvents envelope
 * ({@code data} holds the FxRatesUpdatedEvent), like RefundRequestedConsumer.
 */
class FxRateEventConsumerTest {

    private final FxRateCache fxRateCache = new FxRateCache();
    private final ObjectMapper mapper =
            new ObjectMapper().registerModule(new JavaTimeModule());
    private final FxRateEventConsumer consumer =
            new FxRateEventConsumer(fxRateCache, mapper);

    private static final String TOPIC = "payu.fx.rates-updated.v1";

    @Test
    void consumesCloudEventEnvelopeFromStandardTopicAndUpdatesCache() throws Exception {
        Map<String, Object> data = Map.of(
                "eventId", UUID.randomUUID().toString(),
                "timestamp", Instant.parse("2026-08-15T00:00:00Z").toString(),
                "baseCurrency", "IDR",
                "rates", List.of(Map.of(
                        "fromCurrency", "USD",
                        "toCurrency", "IDR",
                        "rate", "16000.0000",
                        "validFrom", Instant.parse("2026-08-15T00:00:00Z").toString(),
                        "validUntil", Instant.now().plusSeconds(900).toString())));

        Map<String, Object> envelope = Map.of(
                "specversion", "1.0.2",
                "id", UUID.randomUUID().toString(),
                "source", "/services/fx-service",
                "type", "FxRatesUpdated",
                "subject", UUID.randomUUID().toString(),
                "data", data);

        String json = new ObjectMapper().writeValueAsString(envelope);
        ConsumerRecord<String, String> record =
                new ConsumerRecord<>(TOPIC, 0, 0L, "fx-rates", json);

        consumer.onFxRatesUpdated(record);

        assertThat(fxRateCache.getRate("USD", "IDR"))
                .isEqualByComparingTo(new BigDecimal("16000.0000"));
    }
}
