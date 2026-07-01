package id.payu.integration.adapter.camel.route;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;

import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * AUDIT-053 fix: SwiftRouteBuilder must source Kafka bootstrap servers from a
 * Spring {@code @Value}-injected property instead of {@code System.getenv()}.
 *
 * <p>Tests use reflection on {@link SwiftRouteBuilder} class fields only — no
 * instance instantiation is needed because {@code @RequiredArgsConstructor}
 * would otherwise require {@code SwiftValidator}, {@code SwiftTransformer},
 * {@code IntegrationService}, and {@code MessageProcessingService} mocks. The
 * structural assertions are sufficient regression guards.</p>
 */
class SwiftRouteBuilderKafkaBootstrapTest {

    @Test
    void shouldHaveValueAnnotationOnKafkaBootstrapServersField() throws Exception {
        Field field = SwiftRouteBuilder.class.getDeclaredField("kafkaBootstrapServers");

        Value annotation = field.getAnnotation(Value.class);
        assertThat(annotation)
            .as("@Value annotation must drive kafkaBootstrapServers (not System.getenv)")
            .isNotNull();
        assertThat(annotation.value())
            .as("property placeholder must follow kafka.bootstrap-servers convention")
            .contains("kafka.bootstrap-servers");
    }

    @Test
    void valueDefaultExpressionShouldPreserveLocalhostFallback() throws Exception {
        Field field = SwiftRouteBuilder.class.getDeclaredField("kafkaBootstrapServers");
        Value annotation = field.getAnnotation(Value.class);

        assertThat(annotation.value())
            .as("default expression must preserve existing localhost:9092 fallback")
            .contains("localhost:9092");
    }
}
