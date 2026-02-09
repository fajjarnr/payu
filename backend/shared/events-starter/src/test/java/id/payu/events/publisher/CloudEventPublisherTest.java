package id.payu.events.publisher;

import id.payu.events.cloudevents.CloudEventEnvelope;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("CloudEventPublisher")
class CloudEventPublisherTest {

    @Test
    @DisplayName("CloudEventPublishException should carry message")
    void publishExceptionMessage() {
        var ex = new CloudEventPublisher.CloudEventPublishException("Kafka unavailable");
        assertThat(ex.getMessage()).isEqualTo("Kafka unavailable");
    }

    @Test
    @DisplayName("CloudEventPublishException should carry cause")
    void publishExceptionCause() {
        var cause = new RuntimeException("connection refused");
        var ex = new CloudEventPublisher.CloudEventPublishException("Kafka error", cause);

        assertThat(ex.getMessage()).isEqualTo("Kafka error");
        assertThat(ex.getCause()).isEqualTo(cause);
    }

    @Test
    @DisplayName("interface should define all required methods")
    void interfaceMethods() throws NoSuchMethodException {
        // Verify the interface contract has all expected methods
        assertThat(CloudEventPublisher.class.getMethod("publish", CloudEventEnvelope.class)).isNotNull();
        assertThat(CloudEventPublisher.class.getMethod("publishAsync", CloudEventEnvelope.class)).isNotNull();
        assertThat(CloudEventPublisher.class.getMethod("publishToTopic", CloudEventEnvelope.class, String.class)).isNotNull();
        assertThat(CloudEventPublisher.class.getMethod("publishToTopicAsync", CloudEventEnvelope.class, String.class)).isNotNull();
    }
}
