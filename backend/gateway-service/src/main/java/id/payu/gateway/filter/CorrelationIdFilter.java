package id.payu.gateway.filter;

import io.quarkus.logging.Log;
import io.vertx.core.http.HttpServerRequest;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.ext.Provider;
import java.util.UUID;

/**
 * Filter to add correlation ID to all requests for distributed tracing.
 */
@Provider
@ApplicationScoped
public class CorrelationIdFilter implements ContainerRequestFilter, ContainerResponseFilter {

    public static final String CORRELATION_ID_HEADER = "X-Correlation-Id";
    public static final String REQUEST_ID_HEADER = "X-Request-Id";
    private static final String START_TIME_PROPERTY = "request-start-time";

    @Context
    HttpServerRequest request;

    @Override
    public void filter(ContainerRequestContext requestContext) {
        // Get or generate correlation ID
        String correlationId = requestContext.getHeaderString(CORRELATION_ID_HEADER);
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }

        // Generate request ID
        String requestId = UUID.randomUUID().toString().substring(0, 8);

        // Store in MDC for logging
        org.jboss.logging.MDC.put("correlationId", correlationId);
        org.jboss.logging.MDC.put("requestId", requestId);

        // Store for response
        requestContext.setProperty(CORRELATION_ID_HEADER, correlationId);
        requestContext.setProperty(REQUEST_ID_HEADER, requestId);
        requestContext.setProperty(START_TIME_PROPERTY, System.currentTimeMillis());

        Log.debugf("Incoming request: %s %s [correlationId=%s, requestId=%s]",
                   requestContext.getMethod(),
                   requestContext.getUriInfo().getPath(),
                   correlationId,
                   requestId);
    }

    @Override
    public void filter(ContainerRequestContext requestContext, 
                       ContainerResponseContext responseContext) {
        // Add correlation ID to response
        String correlationId = (String) requestContext.getProperty(CORRELATION_ID_HEADER);
        String requestId = (String) requestContext.getProperty(REQUEST_ID_HEADER);

        if (correlationId != null) {
            responseContext.getHeaders().putSingle(CORRELATION_ID_HEADER, correlationId);
        }
        if (requestId != null) {
            responseContext.getHeaders().putSingle(REQUEST_ID_HEADER, requestId);
        }

        // Log request duration
        Long startTime = (Long) requestContext.getProperty(START_TIME_PROPERTY);
        if (startTime != null) {
            long duration = System.currentTimeMillis() - startTime;
            Log.debugf("Request completed: %s %s -> %d (%dms)",
                       requestContext.getMethod(),
                       requestContext.getUriInfo().getPath(),
                       responseContext.getStatus(),
                       duration);
        }

        // Clean up MDC
        org.jboss.logging.MDC.remove("correlationId");
        org.jboss.logging.MDC.remove("requestId");
    }
}
