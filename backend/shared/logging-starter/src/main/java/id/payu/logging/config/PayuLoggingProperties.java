package id.payu.logging.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for PayU logging starter.
 */
@ConfigurationProperties(prefix = "payu.logging")
public class PayuLoggingProperties {

    private boolean enabled = true;
    private String serviceName;
    private String serviceVersion = "1.0.0";
    private String environment = "dev";
    private CorrelationProperties correlation = new CorrelationProperties();
    private TracingProperties tracing = new TracingProperties();
    private RequestLoggingProperties requestLogging = new RequestLoggingProperties();

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public String getServiceName() { return serviceName; }
    public void setServiceName(String serviceName) { this.serviceName = serviceName; }

    public String getServiceVersion() { return serviceVersion; }
    public void setServiceVersion(String serviceVersion) { this.serviceVersion = serviceVersion; }

    public String getEnvironment() { return environment; }
    public void setEnvironment(String environment) { this.environment = environment; }

    public CorrelationProperties getCorrelation() { return correlation; }
    public void setCorrelation(CorrelationProperties correlation) { this.correlation = correlation; }

    public TracingProperties getTracing() { return tracing; }
    public void setTracing(TracingProperties tracing) { this.tracing = tracing; }

    public RequestLoggingProperties getRequestLogging() { return requestLogging; }
    public void setRequestLogging(RequestLoggingProperties requestLogging) { this.requestLogging = requestLogging; }

    public static class CorrelationProperties {
        private boolean enabled = true;
        private String headerName = "X-Correlation-Id";
        private String mdcKey = "correlation_id";

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public String getHeaderName() { return headerName; }
        public void setHeaderName(String headerName) { this.headerName = headerName; }
        public String getMdcKey() { return mdcKey; }
        public void setMdcKey(String mdcKey) { this.mdcKey = mdcKey; }
    }

    public static class TracingProperties {
        private boolean enabled = true;
        private String traceIdMdcKey = "trace_id";
        private String spanIdMdcKey = "span_id";

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public String getTraceIdMdcKey() { return traceIdMdcKey; }
        public void setTraceIdMdcKey(String traceIdMdcKey) { this.traceIdMdcKey = traceIdMdcKey; }
        public String getSpanIdMdcKey() { return spanIdMdcKey; }
        public void setSpanIdMdcKey(String spanIdMdcKey) { this.spanIdMdcKey = spanIdMdcKey; }
    }

    public static class RequestLoggingProperties {
        private boolean enabled = false;
        private boolean includePayload = false;
        private int maxPayloadLength = 1000;

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public boolean isIncludePayload() { return includePayload; }
        public void setIncludePayload(boolean includePayload) { this.includePayload = includePayload; }
        public int getMaxPayloadLength() { return maxPayloadLength; }
        public void setMaxPayloadLength(int maxPayloadLength) { this.maxPayloadLength = maxPayloadLength; }
    }
}
