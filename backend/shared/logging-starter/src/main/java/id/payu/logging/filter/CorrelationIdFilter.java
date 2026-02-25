package id.payu.logging.filter;

import id.payu.logging.config.PayuLoggingProperties;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;

import java.io.IOException;
import java.util.UUID;

/**
 * Servlet filter that manages correlation ID for incoming requests.
 * Reads X-Correlation-Id header or generates a new one, then sets it in MDC
 * for propagation to all downstream logs.
 */
public class CorrelationIdFilter implements Filter {

    private final PayuLoggingProperties properties;

    public CorrelationIdFilter(PayuLoggingProperties properties) {
        this.properties = properties;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String correlationId = extractOrGenerateCorrelationId(httpRequest);
        String serviceName = properties.getServiceName() != null
            ? properties.getServiceName()
            : "unknown-service";

        try {
            // Set MDC values for this request
            MDC.put(properties.getCorrelation().getMdcKey(), correlationId);
            MDC.put("service", serviceName);
            MDC.put("service_version", properties.getServiceVersion());
            MDC.put("environment", properties.getEnvironment());

            // Add correlation ID to response header for client tracking
            httpResponse.setHeader(properties.getCorrelation().getHeaderName(), correlationId);

            chain.doFilter(request, response);
        } finally {
            // BUG-BE-101: Remove only the keys this filter set, not all MDC entries
            // MDC.clear() would wipe entries set by Spring Security, OpenTelemetry, etc.
            MDC.remove(properties.getCorrelation().getMdcKey());
            MDC.remove("service");
            MDC.remove("service_version");
            MDC.remove("environment");
        }
    }

    private String extractOrGenerateCorrelationId(HttpServletRequest request) {
        String headerName = properties.getCorrelation().getHeaderName();
        String correlationId = request.getHeader(headerName);

        if (correlationId == null || correlationId.isBlank()) {
            correlationId = generateCorrelationId();
        }

        return correlationId;
    }

    private String generateCorrelationId() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
