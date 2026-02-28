package id.payu.gateway.adapter.filter;

import id.payu.gateway.application.service.RequestTransformationService;
import id.payu.gateway.config.GatewayConfig;
import io.quarkus.logging.Log;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.ext.Provider;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Filter to apply request transformations based on configured rules.
 *
 * <p>
 * This filter implements IMP-018: Request/Response Transformation.
 * Features:
 * - Header injection (add/remove/rewrite)
 * - Configurable transformation rules per route
 * - Priority-based rule execution
 *
 * <p>
 * Priority: Early in the filter chain to ensure transformations are applied
 * before other processing.
 */
@Provider
@ApplicationScoped
@Priority(Priorities.HEADER_DECORATOR)
public class RequestTransformationFilter implements ContainerRequestFilter {

    @Inject
    GatewayConfig config;

    @Inject
    RequestTransformationService transformationService;

    private boolean enabled;

    @PostConstruct
    void init() {
        this.enabled = true;
        Log.infof("RequestTransformationFilter initialized (enabled: %s)", enabled);
    }

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        if (!enabled) {
            return;
        }

        String path = requestContext.getUriInfo().getPath();
        String method = requestContext.getMethod();

        // Skip health and metrics endpoints
        if (path.startsWith("/q/") || path.equals("/health") || path.equals("/status")) {
            return;
        }

        // Convert headers to mutable map
        Map<String, List<String>> headers = new java.util.HashMap<>();
        for (String headerName : requestContext.getHeaders().keySet()) {
            List<String> values = new ArrayList<>(requestContext.getHeaders().get(headerName));
            headers.put(headerName, values);
        }

        // Apply transformations
        Map<String, List<String>> transformedHeaders =
            transformationService.transformRequestHeaders(path, method, headers);

        // Apply transformed headers back to request
        for (Map.Entry<String, List<String>> entry : transformedHeaders.entrySet()) {
            String headerName = entry.getKey();
            List<String> values = entry.getValue();

            if (!values.isEmpty()) {
                // Remove existing header and add new values
                requestContext.getHeaders().remove(headerName);
                for (String value : values) {
                    requestContext.getHeaders().add(headerName, value);
                }
            }
        }

        Log.debugf("Applied request transformations for %s %s", method, path);
    }
}
