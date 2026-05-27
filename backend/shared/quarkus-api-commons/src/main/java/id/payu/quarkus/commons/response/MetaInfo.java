package id.payu.quarkus.commons.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.Instant;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class MetaInfo {

    private String requestId;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    private Instant timestamp;

    public MetaInfo() {
    }

    public MetaInfo(String requestId, Instant timestamp) {
        this.requestId = requestId;
        this.timestamp = timestamp;
    }

    public static MetaInfo now() {
        return new MetaInfo(
                "req-" + UUID.randomUUID().toString().substring(0, 8),
                Instant.now()
        );
    }

    public static MetaInfo withRequestId(String requestId) {
        return new MetaInfo(requestId, Instant.now());
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }
}
