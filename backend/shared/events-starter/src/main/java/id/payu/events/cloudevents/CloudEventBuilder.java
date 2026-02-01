package id.payu.events.cloudevents;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Builder class for constructing CloudEventEnvelope instances.
 * Provides a fluent API for creating CloudEvents with proper defaults.
 *
 * @param <T> The type of the event data payload
 */
public class CloudEventBuilder<T> {

    private String specVersion = "1.0";
    private UUID id = UUID.randomUUID();
    private URI source;
    private String type;
    private String dataContentType = "application/json";
    private OffsetDateTime time = OffsetDateTime.now();
    private String subject;
    private T data;
    private String payuTraceContext;
    private String payuCorrelationId;

    /**
     * Sets the CloudEvents specification version.
     *
     * @param specVersion The spec version (default: "1.0")
     * @return This builder
     */
    public CloudEventBuilder<T> specVersion(String specVersion) {
        this.specVersion = specVersion;
        return this;
    }

    /**
     * Sets the event ID.
     *
     * @param id The event UUID (default: random UUID)
     * @return This builder
     */
    public CloudEventBuilder<T> id(UUID id) {
        this.id = id;
        return this;
    }

    /**
     * Sets the event source.
     *
     * @param source The source URI (required)
     * @return This builder
     */
    public CloudEventBuilder<T> source(URI source) {
        this.source = source;
        return this;
    }

    /**
     * Sets the event source from a string.
     *
     * @param source The source URI string (required)
     * @return This builder
     */
    public CloudEventBuilder<T> source(String source) {
        this.source = URI.create(source);
        return this;
    }

    /**
     * Sets the event type.
     *
     * @param type The event type in reverse-DNS format (required)
     * @return This builder
     */
    public CloudEventBuilder<T> type(String type) {
        this.type = type;
        return this;
    }

    /**
     * Sets the data content type.
     *
     * @param dataContentType The content type (default: "application/json")
     * @return This builder
     */
    public CloudEventBuilder<T> dataContentType(String dataContentType) {
        this.dataContentType = dataContentType;
        return this;
    }

    /**
     * Sets the event timestamp.
     *
     * @param time The timestamp (default: current time)
     * @return This builder
     */
    public CloudEventBuilder<T> time(OffsetDateTime time) {
        this.time = time;
        return this;
    }

    /**
     * Sets the event subject.
     *
     * @param subject The subject identifying the resource
     * @return This builder
     */
    public CloudEventBuilder<T> subject(String subject) {
        this.subject = subject;
        return this;
    }

    /**
     * Sets the event data payload.
     *
     * @param data The event data
     * @return This builder
     */
    public CloudEventBuilder<T> data(T data) {
        this.data = data;
        return this;
    }

    /**
     * Sets the PayU trace context.
     *
     * @param payuTraceContext The trace context for distributed tracing
     * @return This builder
     */
    public CloudEventBuilder<T> payuTraceContext(String payuTraceContext) {
        this.payuTraceContext = payuTraceContext;
        return this;
    }

    /**
     * Sets the PayU correlation ID.
     *
     * @param payuCorrelationId The correlation ID for tracking related events
     * @return This builder
     */
    public CloudEventBuilder<T> payuCorrelationId(String payuCorrelationId) {
        this.payuCorrelationId = payuCorrelationId;
        return this;
    }

    /**
     * Builds the CloudEventEnvelope instance.
     *
     * @return A new CloudEventEnvelope
     * @throws IllegalStateException if required fields are not set
     */
    public CloudEventEnvelope<T> build() {
        if (source == null) {
            throw new IllegalStateException("Source is required");
        }
        if (type == null || type.isBlank()) {
            throw new IllegalStateException("Type is required");
        }

        CloudEventEnvelope<T> envelope = new CloudEventEnvelope<T>();
        envelope.setSpecVersion(this.specVersion);
        envelope.setId(this.id);
        envelope.setSource(this.source);
        envelope.setType(this.type);
        envelope.setDataContentType(this.dataContentType);
        envelope.setTime(this.time);
        envelope.setSubject(this.subject);
        envelope.setData(this.data);
        envelope.setPayuTraceContext(this.payuTraceContext);
        envelope.setPayuCorrelationId(this.payuCorrelationId);

        return envelope;
    }

    /**
     * Creates a builder pre-configured with service source.
     *
     * @param serviceName The service name (e.g., "account-service")
     * @param <T> The type of the event data
     * @return A pre-configured builder
     */
    public static <T> CloudEventBuilder<T> forService(String serviceName) {
        CloudEventBuilder<T> builder = new CloudEventBuilder<>();
        builder.source(URI.create("/services/" + serviceName));
        return builder;
    }

    /**
     * Creates a builder pre-configured with domain source.
     *
     * @param domain The domain name (e.g., "account", "transaction")
     * @param <T> The type of the event data
     * @return A pre-configured builder
     */
    public static <T> CloudEventBuilder<T> forDomain(String domain) {
        CloudEventBuilder<T> builder = new CloudEventBuilder<>();
        builder.source(URI.create("/domains/" + domain));
        return builder;
    }
}
