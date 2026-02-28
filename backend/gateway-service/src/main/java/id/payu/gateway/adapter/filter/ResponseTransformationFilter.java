package id.payu.gateway.adapter.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import id.payu.gateway.application.service.RequestTransformationService;
import id.payu.gateway.config.GatewayConfig;
import io.quarkus.logging.Log;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Filter to apply response transformations based on configured rules.
 *
 * <p>
 * This filter implements IMP-018: Request/Response Transformation.
 * Features:
 * - Header injection (add/remove/rewrite)
 * - Body field masking for sensitive data
 * - Configurable transformation rules per route
 *
 * <p>
 * Priority: Late in the filter chain to ensure transformations are applied
 * after response generation but before sending to client.
 */
@Provider
@ApplicationScoped
@Priority(Priorities.HEADER_DECORATOR + 10)
public class ResponseTransformationFilter implements ContainerResponseFilter {

    @Inject
    GatewayConfig config;

    @Inject
    RequestTransformationService transformationService;

    @Inject
    ObjectMapper objectMapper;

    private boolean enabled;

    @PostConstruct
    void init() {
        this.enabled = true;
        Log.infof("ResponseTransformationFilter initialized (enabled: %s)", enabled);
    }

    @Override
    public void filter(ContainerRequestContext requestContext,
                       ContainerResponseContext responseContext) throws IOException {
        if (!enabled) {
            return;
        }

        String path = requestContext.getUriInfo().getPath();
        String method = requestContext.getMethod();

        // Skip health and metrics endpoints
        if (path.startsWith("/q/") || path.equals("/health") || path.equals("/status")) {
            return;
        }

        // Transform headers
        transformHeaders(requestContext, responseContext, path, method);

        // Transform body (masking)
        transformBody(requestContext, responseContext, path, method);

        Log.debugf("Applied response transformations for %s %s", method, path);
    }

    private void transformHeaders(ContainerRequestContext requestContext,
                                   ContainerResponseContext responseContext,
                                   String path, String method) {
        // Convert headers to mutable map
        Map<String, List<String>> headers = new java.util.HashMap<>();
        for (String headerName : responseContext.getHeaders().keySet()) {
            Object values = responseContext.getHeaders().get(headerName);
            if (values instanceof List) {
                headers.put(headerName, new ArrayList<>((List<String>) values));
            } else if (values != null) {
                List<String> list = new ArrayList<>();
                list.add(values.toString());
                headers.put(headerName, list);
            }
        }

        // Apply transformations
        Map<String, List<String>> transformedHeaders =
            transformationService.transformResponseHeaders(path, method, headers);

        // Apply transformed headers back to response
        for (Map.Entry<String, List<String>> entry : transformedHeaders.entrySet()) {
            String headerName = entry.getKey();
            List<String> values = entry.getValue();

            if (!values.isEmpty()) {
                responseContext.getHeaders().remove(headerName);
                for (String value : values) {
                    responseContext.getHeaders().add(headerName, value);
                }
            }
        }
    }

    private void transformBody(ContainerRequestContext requestContext,
                               ContainerResponseContext responseContext,
                               String path, String method) {
        // Only process JSON responses
        if (responseContext.getMediaType() == null ||
            !responseContext.getMediaType().toString().contains("json")) {
            return;
        }

        Object entity = responseContext.getEntity();
        if (entity == null) {
            return;
        }

        try {
            String body;
            if (entity instanceof String) {
                body = (String) entity;
            } else {
                body = objectMapper.writeValueAsString(entity);
            }

            String mimeType = responseContext.getMediaType() != null
                ? responseContext.getMediaType().toString()
                : "application/json";

            String maskedBody = transformationService.maskResponseBody(path, method, body, mimeType);

            if (!body.equals(maskedBody)) {
                responseContext.setEntity(maskedBody);
                Log.debugf("Masked sensitive fields in response for %s %s", method, path);
            }
        } catch (Exception e) {
            Log.warnf(e, "Failed to transform response body for %s %s", method, path);
        }
    }
}
