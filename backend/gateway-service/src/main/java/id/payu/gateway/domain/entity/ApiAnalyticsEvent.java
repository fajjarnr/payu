package id.payu.gateway.domain.entity;

import id.payu.gateway.domain.vo.HttpMethod;
import java.time.Instant;
import java.util.Objects;

/**
 * Domain entity representing an API analytics event.
 * Tracks per-request metrics for analytics and monitoring.
 * <p>
 * This entity follows DDD patterns with internal behavior and invariants.
 * Data is persisted to TimescaleDB for time-series analytics.
 */
public class ApiAnalyticsEvent {

    private final String id;
    private final String partnerId;
    private final String endpoint;
    private final HttpMethod method;
    private final int statusCode;
    private final long durationMs;
    private final long requestSize;
    private final long responseSize;
    private final String userAgent;
    private final String clientIp;
    private final Instant timestamp;
    private final String correlationId;

    private ApiAnalyticsEvent(Builder builder) {
        this.id = Objects.requireNonNull(builder.id, "ID cannot be null");
        this.partnerId = builder.partnerId;
        this.endpoint = Objects.requireNonNull(builder.endpoint, "Endpoint cannot be null");
        this.method = Objects.requireNonNull(builder.method, "Method cannot be null");
        this.statusCode = builder.statusCode;
        this.durationMs = builder.durationMs;
        this.requestSize = builder.requestSize;
        this.responseSize = builder.responseSize;
        this.userAgent = builder.userAgent;
        this.clientIp = builder.clientIp;
        this.timestamp = Objects.requireNonNull(builder.timestamp, "Timestamp cannot be null");
        this.correlationId = builder.correlationId;

        validate();
    }

    private void validate() {
        if (statusCode < 100 || statusCode > 599) {
            throw new IllegalArgumentException("Invalid HTTP status code: " + statusCode);
        }
        if (durationMs < 0) {
            throw new IllegalArgumentException("Duration cannot be negative: " + durationMs);
        }
        if (requestSize < 0) {
            throw new IllegalArgumentException("Request size cannot be negative: " + requestSize);
        }
        if (responseSize < 0) {
            throw new IllegalArgumentException("Response size cannot be negative: " + responseSize);
        }
    }

    // Business methods

    public boolean isError() {
        return statusCode >= 400;
    }

    public boolean isServerError() {
        return statusCode >= 500;
    }

    public boolean isSuccess() {
        return statusCode >= 200 && statusCode < 300;
    }

    public String getEndpointKey() {
        return method.name() + ":" + endpoint;
    }

    public String getPartnerEndpointKey() {
        String pid = partnerId != null ? partnerId : "anonymous";
        return pid + ":" + method.name() + ":" + endpoint;
    }

    // Getters

    public String getId() {
        return id;
    }

    public String getPartnerId() {
        return partnerId;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public HttpMethod getMethod() {
        return method;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public long getDurationMs() {
        return durationMs;
    }

    public long getRequestSize() {
        return requestSize;
    }

    public long getResponseSize() {
        return responseSize;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public String getClientIp() {
        return clientIp;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    // Builder

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String id;
        private String partnerId;
        private String endpoint;
        private HttpMethod method;
        private int statusCode;
        private long durationMs;
        private long requestSize;
        private long responseSize;
        private String userAgent;
        private String clientIp;
        private Instant timestamp;
        private String correlationId;

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder partnerId(String partnerId) {
            this.partnerId = partnerId;
            return this;
        }

        public Builder endpoint(String endpoint) {
            this.endpoint = endpoint;
            return this;
        }

        public Builder method(HttpMethod method) {
            this.method = method;
            return this;
        }

        public Builder method(String method) {
            this.method = HttpMethod.fromString(method);
            return this;
        }

        public Builder statusCode(int statusCode) {
            this.statusCode = statusCode;
            return this;
        }

        public Builder durationMs(long durationMs) {
            this.durationMs = durationMs;
            return this;
        }

        public Builder requestSize(long requestSize) {
            this.requestSize = requestSize;
            return this;
        }

        public Builder responseSize(long responseSize) {
            this.responseSize = responseSize;
            return this;
        }

        public Builder userAgent(String userAgent) {
            this.userAgent = userAgent;
            return this;
        }

        public Builder clientIp(String clientIp) {
            this.clientIp = clientIp;
            return this;
        }

        public Builder timestamp(Instant timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public Builder correlationId(String correlationId) {
            this.correlationId = correlationId;
            return this;
        }

        public ApiAnalyticsEvent build() {
            if (timestamp == null) {
                timestamp = Instant.now();
            }
            if (id == null) {
                id = java.util.UUID.randomUUID().toString();
            }
            return new ApiAnalyticsEvent(this);
        }
    }
}
