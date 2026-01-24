package id.payu.gateway.filter;

import id.payu.gateway.config.GatewayConfig;
import id.payu.gateway.service.ApiKeyRotationService;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import java.util.List;
import java.util.Map;

/**
 * Filter to validate API keys for protected endpoints.
 */
@Provider
@ApplicationScoped
public class ApiKeyValidationFilter implements ContainerRequestFilter {

    @Inject
    GatewayConfig config;

    @Inject
    ApiKeyRotationService apiKeyService;

    @Override
    public void filter(ContainerRequestContext requestContext) {
        if (!config.apiKeys().enabled()) {
            return;
        }

        String path = requestContext.getUriInfo().getPath();

        // Check if path should bypass API key validation
        for (String bypassPath : config.apiKeys().bypassPaths()) {
            if (path.startsWith(bypassPath) || path.equals(bypassPath)) {
                Log.debugf("Bypassing API key validation for path: %s", path);
                return;
            }
        }

        // Get API key from header
        String apiKey = requestContext.getHeaderString(config.apiKeys().headerName());

        if (apiKey == null || apiKey.isBlank()) {
            Log.warnf("Missing API key for path: %s", path);
            requestContext.abortWith(
                Response.status(Response.Status.UNAUTHORIZED)
                    .entity(Map.of(
                        "error", "MISSING_API_KEY",
                        "message", "API key is required for this endpoint"
                    ))
                    .build()
            );
            return;
        }

        // Validate API key
        apiKeyService.validateApiKey(apiKey)
            .subscribe()
            .with(userId -> {
                if (userId == null) {
                    Log.warnf("Invalid API key for path: %s", path);
                    requestContext.abortWith(
                        Response.status(Response.Status.UNAUTHORIZED)
                            .entity(Map.of(
                                "error", "INVALID_API_KEY",
                                "message", "API key is invalid or expired"
                            ))
                            .build()
                    );
                } else {
                    // Store user ID in request context for downstream use
                    requestContext.setProperty("user-id", userId);
                    Log.debugf("API key validated for user: %s", userId);
                }
            }, failure -> {
                // If validation fails, fail open (allow request) or fail closed based on config
                Log.errorf(failure, "API key validation failed for path: %s", path);
                requestContext.abortWith(
                    Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                        .entity(Map.of(
                            "error", "API_KEY_VALIDATION_ERROR",
                            "message", "Failed to validate API key"
                        ))
                        .build()
                );
            });
    }
}
