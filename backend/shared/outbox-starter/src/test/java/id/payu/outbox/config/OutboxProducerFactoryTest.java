package id.payu.outbox.config;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.ProducerFactory;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ARCH-PROD-001: the outbox producer must default to durable delivery
 * (acks=all, idempotence, bounded retries) so financial events never rely on
 * the Kafka client default (acks=1, no idempotence).
 */
@DisplayName("OutboxProducerFactory")
class OutboxProducerFactoryTest {

    @Test
    @DisplayName("should default to acks=all, idempotence and bounded retries")
    void shouldDefaultToDurableProducerConfig() {
        ProducerFactory<Object, Object> factory = new OutboxAutoConfiguration()
                .outboxProducerFactory("localhost:9092");

        assertThat(factory).isInstanceOf(DefaultKafkaProducerFactory.class);
        @SuppressWarnings("unchecked")
        Map<String, Object> config = ((DefaultKafkaProducerFactory<Object, Object>) factory).getConfigurationProperties();

        assertThat(config.get(ProducerConfig.ACKS_CONFIG))
                .as("acks must be all, never the client default of 1")
                .isEqualTo("all");
        assertThat(config.get(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG))
                .as("idempotence must be enabled so retries cannot duplicate records")
                .isEqualTo(true);
        assertThat(config.get(ProducerConfig.RETRIES_CONFIG))
                .as("retries must be bounded")
                .isEqualTo(5);
    }
}
