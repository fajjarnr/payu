package id.payu.events.publisher;

import id.payu.events.cloudevents.CloudEventEnvelope;

import java.util.concurrent.CompletableFuture;

/**
 * Interface for publishing CloudEvents in the PayU platform.
 * Implementations handle the actual transport mechanism (Kafka, HTTP, etc.).
 *
 * @param <T> The type of the event data payload
 */
public interface CloudEventPublisher<T> {

    /**
     * Publishes a CloudEvent synchronously.
     *
     * @param event The CloudEvent envelope to publish
     * @throws CloudEventPublishException if publishing fails
     */
    void publish(CloudEventEnvelope<T> event);

    /**
     * Publishes a CloudEvent asynchronously.
     *
     * @param event The CloudEvent envelope to publish
     * @return A CompletableFuture that completes when the event is published
     */
    CompletableFuture<Void> publishAsync(CloudEventEnvelope<T> event);

    /**
     * Publishes a CloudEvent to a specific topic/channel.
     *
     * @param event The CloudEvent envelope to publish
     * @param topic The target topic or channel name
     * @throws CloudEventPublishException if publishing fails
     */
    void publishToTopic(CloudEventEnvelope<T> event, String topic);

    /**
     * Publishes a CloudEvent asynchronously to a specific topic/channel.
     *
     * @param event The CloudEvent envelope to publish
     * @param topic The target topic or channel name
     * @return A CompletableFuture that completes when the event is published
     */
    CompletableFuture<Void> publishToTopicAsync(CloudEventEnvelope<T> event, String topic);

    /**
     * Exception thrown when CloudEvent publishing fails.
     */
    class CloudEventPublishException extends RuntimeException {
        public CloudEventPublishException(String message) {
            super(message);
        }

        public CloudEventPublishException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
