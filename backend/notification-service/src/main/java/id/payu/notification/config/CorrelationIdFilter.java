package id.payu.notification.config;

import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;
import java.util.UUID;

/**
 * JAX-RS filter to propagate X-Correlation-Id header into JBoss MDC
 * for structured logging consistency across PayU Quarkus services.
 */
@Provider
@ApplicationScoped
public class CorrelationIdFilter implements ContainerRequestFilter, ContainerResponseFilter {

    public static final String CORRELATION_ID_HEADER = "X-Correlation-Id";
    private static final String START_TIME_PROPERTY = "request-start-time";

    @Override
    public void filter(ContainerRequestContext requestContext) {
        String correlationId = requestContext.getHeaderString(CORRELATION_ID_HEADER);
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString().replace("-", "");
        }

        org.jboss.logging.MDC.put("correlation_id", correlationId);
        requestContext.setProperty(CORRELATION_ID_HEADER, correlationId);
        requestContext.setProperty(START_TIME_PROPERTY, System.currentTimeMillis());

        Log.debugf("Incoming request: %s %s [correlationId=%s]",
                requestContext.getMethod(),
                requestContext.getUriInfo().getPath(),
                correlationId);
    }

    @Override
    public void filter(ContainerRequestContext requestContext,
                       ContainerResponseContext responseContext) {
        String correlationId = (String) requestContext.getProperty(CORRELATION_ID_HEADER);
        if (correlationId != null) {
            responseContext.getHeaders().putSingle(CORRELATION_ID_HEADER, correlationId);
        }

        Long startTime = (Long) requestContext.getProperty(START_TIME_PROPERTY);
        if (startTime != null) {
            long duration = System.currentTimeMillis() - startTime;
            Log.debugf("Request completed: %s %s -> %d (%dms)",
                    requestContext.getMethod(),
                    requestContext.getUriInfo().getPath(),
                    responseContext.getStatus(),
                    duration);
        }

        org.jboss.logging.MDC.remove("correlation_id");
    }
}
