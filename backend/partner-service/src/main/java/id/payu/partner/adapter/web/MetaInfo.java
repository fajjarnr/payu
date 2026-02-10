package id.payu.partner.adapter.web;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

/**
 * Metadata included in every API response.
 * Provides request tracing and timestamp information.
 */
@Schema(description = "Response metadata")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MetaInfo {

    @Schema(
        description = "Unique request identifier for tracing",
        example = "req-abc-123-def-456"
    )
    private String requestId;

    @Schema(
        description = "ISO 8601 timestamp of when the response was generated",
        example = "2026-01-28T10:30:00Z",
        type = "string",
        format = "date-time"
    )
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    private Instant timestamp;

    public MetaInfo() {}

    public MetaInfo(String requestId, Instant timestamp) {
        this.requestId = requestId;
        this.timestamp = timestamp;
    }

    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }
    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }

    /**
     * Creates MetaInfo with current timestamp and generated request ID.
     */
    public static MetaInfo now() {
        return new MetaInfo(
            "req-" + UUID.randomUUID().toString().substring(0, 8),
            Instant.now()
        );
    }

    /**
     * Creates MetaInfo with provided request ID and current timestamp.
     */
    public static MetaInfo withRequestId(String requestId) {
        return new MetaInfo(
            requestId,
            Instant.now()
        );
    }
}
