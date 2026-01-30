package id.payu.events.cloudevents;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Base CloudEvent envelope class for PayU platform.
 * Implements CloudEvents specification version 1.0.
 *
 * @param <T> The type of the event data payload
 * @see <a href="https://cloudevents.io/">CloudEvents Specification</a>
 */
@Data
@Builder
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CloudEventEnvelope<T> {

    /**
     * CloudEvents specification version.
     * Default: "1.0"
     */
    @NotBlank
    @Builder.Default
    @JsonProperty("specversion")
    private String specVersion = "1.0";

    /**
     * Unique identifier for the event.
     * Automatically generated UUID if not provided.
     */
    @NotNull
    @Builder.Default
    private UUID id = UUID.randomUUID();

    /**
     * Identifies the context in which an event happened.
     * Often represented as a URI.
     */
    @NotNull
    private URI source;

    /**
     * Contains a value describing the type of event related to the originating occurrence.
     * Format: reverse-DNS name (e.g., id.payu.account.created)
     */
    @NotBlank
    private String type;

    /**
     * Content type of the data value.
     * Default: "application/json"
     */
    @Builder.Default
    @JsonProperty("datacontenttype")
    private String dataContentType = "application/json";

    /**
     * Timestamp of when the occurrence happened.
     * Default: current time
     */
    @Builder.Default
    private OffsetDateTime time = OffsetDateTime.now();

    /**
     * The subject of the event in the context of the event producer.
     * Identifies the resource being acted upon.
     */
    private String subject;

    /**
     * The event payload.
     * The type is generic to support various event data structures.
     */
    private T data;

    /**
     * PayU-specific trace context for distributed tracing.
     * Follows W3C Trace Context format.
     */
    @JsonProperty("payutracecontext")
    private String payuTraceContext;

    /**
     * PayU-specific correlation ID for tracking related events across services.
     */
    @JsonProperty("payucorrelationid")
    private String payuCorrelationId;

    /**
     * Factory method to create a builder with default values.
     *
     * @param <T> The type of the event data
     * @return A new CloudEventBuilder instance
     */
    public static <T> CloudEventBuilder<T> builder() {
        return new CloudEventBuilder<>();
    }

    /**
     * Validates the envelope has all required fields.
     *
     * @throws IllegalStateException if required fields are missing
     */
    public void validate() {
        if (specVersion == null || specVersion.isBlank()) {
            throw new IllegalStateException("specVersion is required");
        }
        if (id == null) {
            throw new IllegalStateException("id is required");
        }
        if (source == null) {
            throw new IllegalStateException("source is required");
        }
        if (type == null || type.isBlank()) {
            throw new IllegalStateException("type is required");
        }
    }
}
