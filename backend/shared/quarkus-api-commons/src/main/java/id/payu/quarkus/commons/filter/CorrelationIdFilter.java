package id.payu.quarkus.commons.filter;

import id.payu.quarkus.commons.constant.ApiConstants;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.util.UUID;

@Provider
@ApplicationScoped
public class CorrelationIdFilter implements ContainerRequestFilter, ContainerResponseFilter {

    private static final Logger log = LoggerFactory.getLogger(CorrelationIdFilter.class);

    private static final String CORRELATION_ID = "correlationId";
    private static final String REQUEST_ID = "requestId";
    private static final String CORRELATION_ID_SNAKE = "correlation_id";
    private static final String REQUEST_ID_SNAKE = "request_id";
    private static final String START_TIME_PROPERTY = "request-start-time";

    @Override
    public void filter(ContainerRequestContext requestContext) {
        String correlationId = requestContext.getHeaderString(ApiConstants.CORRELATION_ID_HEADER);
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = requestContext.getHeaderString("X-Correlation-Id");
        }
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString().replace("-", "");
        }

        String requestId = requestContext.getHeaderString(ApiConstants.REQUEST_ID_HEADER);
        if (requestId == null || requestId.isBlank()) {
            requestId = requestContext.getHeaderString("X-Request-Id");
        }
        if (requestId == null || requestId.isBlank()) {
            requestId = UUID.randomUUID().toString().substring(0, 8);
        }

        MDC.put(CORRELATION_ID, correlationId);
        MDC.put(REQUEST_ID, requestId);
        MDC.put(CORRELATION_ID_SNAKE, correlationId);
        MDC.put(REQUEST_ID_SNAKE, requestId);

        requestContext.setProperty(CORRELATION_ID, correlationId);
        requestContext.setProperty(REQUEST_ID, requestId);
        requestContext.setProperty(START_TIME_PROPERTY, System.currentTimeMillis());

        if (log.isDebugEnabled()) {
            log.debug("Incoming request: {} {} [correlationId={}, requestId={}]",
                       requestContext.getMethod(),
                       requestContext.getUriInfo().getPath(),
                       correlationId,
                       requestId);
        }
    }

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) {
        String correlationId = (String) requestContext.getProperty(CORRELATION_ID);
        String requestId = (String) requestContext.getProperty(REQUEST_ID);

        if (correlationId != null) {
            responseContext.getHeaders().putSingle(ApiConstants.CORRELATION_ID_HEADER, correlationId);
            responseContext.getHeaders().putSingle("X-Correlation-Id", correlationId);
        }
        if (requestId != null) {
            responseContext.getHeaders().putSingle(ApiConstants.REQUEST_ID_HEADER, requestId);
            responseContext.getHeaders().putSingle("X-Request-Id", requestId);
        }

        Long startTime = (Long) requestContext.getProperty(START_TIME_PROPERTY);
        if (startTime != null) {
            long duration = System.currentTimeMillis() - startTime;
            if (log.isDebugEnabled()) {
                log.debug("Request completed: {} {} -> {} ({}ms)",
                           requestContext.getMethod(),
                           requestContext.getUriInfo().getPath(),
                           responseContext.getStatus(),
                           duration);
            }
        }

        MDC.remove(CORRELATION_ID);
        MDC.remove(REQUEST_ID);
        MDC.remove(CORRELATION_ID_SNAKE);
        MDC.remove(REQUEST_ID_SNAKE);
    }
}
