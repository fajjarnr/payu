package id.payu.quarkus.commons.filter;

import id.payu.quarkus.commons.constant.ApiConstants;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;
import org.slf4j.MDC;

import java.util.UUID;

@Provider
@ApplicationScoped
public class CorrelationIdFilter implements ContainerRequestFilter, ContainerResponseFilter {

    private static final String CORRELATION_ID = "correlationId";
    private static final String REQUEST_ID = "requestId";

    @Override
    public void filter(ContainerRequestContext requestContext) {
        String correlationId = requestContext.getHeaderString(ApiConstants.CORRELATION_ID_HEADER);
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }

        String requestId = requestContext.getHeaderString(ApiConstants.REQUEST_ID_HEADER);
        if (requestId == null || requestId.isBlank()) {
            requestId = "req-" + UUID.randomUUID().toString().substring(0, 8);
        }

        MDC.put(CORRELATION_ID, correlationId);
        MDC.put(REQUEST_ID, requestId);

        requestContext.setProperty(CORRELATION_ID, correlationId);
        requestContext.setProperty(REQUEST_ID, requestId);
    }

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) {
        String correlationId = (String) requestContext.getProperty(CORRELATION_ID);
        String requestId = (String) requestContext.getProperty(REQUEST_ID);

        if (correlationId != null) {
            responseContext.getHeaders().putSingle(ApiConstants.CORRELATION_ID_HEADER, correlationId);
        }
        if (requestId != null) {
            responseContext.getHeaders().putSingle(ApiConstants.REQUEST_ID_HEADER, requestId);
        }

        MDC.remove(CORRELATION_ID);
        MDC.remove(REQUEST_ID);
    }
}
