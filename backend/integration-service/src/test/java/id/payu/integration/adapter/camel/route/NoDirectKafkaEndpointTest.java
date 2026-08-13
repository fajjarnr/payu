package id.payu.integration.adapter.camel.route;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ARCH-INTG-001: no Camel route may publish directly to Kafka via a
 * {@code kafka:} endpoint. Events must cross {@link id.payu.integration.application.port.out.MessagePublisherPort}
 * (outbox-backed) so every message gets a CloudEvents envelope and survives
 * crash via the transactional outbox.
 */
class NoDirectKafkaEndpointTest {

    @Test
    void swiftRouteBuilderShouldNotPublishDirectlyToKafka() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/id/payu/integration/adapter/camel/route/SwiftRouteBuilder.java"));

        assertThat(source).doesNotContain("kafka:payu.");
        assertThat(source).contains("MessagePublisherPort");
    }

    @Test
    void ojkRouteBuilderShouldNotPublishDirectlyToKafka() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/id/payu/integration/adapter/camel/route/OjkRouteBuilder.java"));

        assertThat(source).doesNotContain("kafka:payu.");
        assertThat(source).contains("MessagePublisherPort");
    }
}
