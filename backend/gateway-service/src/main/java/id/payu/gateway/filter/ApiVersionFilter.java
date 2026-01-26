package id.payu.gateway.filter;

import id.payu.gateway.config.GatewayConfig;
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
 * Filter to handle API versioning.
 * Supports URL path versioning (v1, v2) and header-based versioning.
 */
@Provider
@ApplicationScoped
public class ApiVersionFilter implements ContainerRequestFilter {

    private static final String VERSION_HEADER = "X-API-Version";
    private static final Map<String, String> DEPRECATION_WARNINGS = Map.of(
        "v1", "API v1 is deprecated. Please migrate to v2 by 2026-06-01."
    );

    @Inject
    GatewayConfig config;

    @Override
    public void filter(ContainerRequestContext requestContext) {
        if (!config.versioning().enabled()) {
            return;
        }

        String path = requestContext.getUriInfo().getPath();

        // Skip health and metrics endpoints
        if (path.startsWith("/q/") || path.equals("/health")) {
            return;
        }

        // Extract version from path or header
        String version = extractVersion(path, requestContext);

        // Validate version
        if (!isValidVersion(version)) {
            Log.warnf("Invalid API version requested: %s", version);
            requestContext.abortWith(
                Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of(
                        "error", "INVALID_API_VERSION",
                        "message", "Invalid API version. Supported versions: " + config.versioning().supportedVersions(),
                        "supportedVersions", config.versioning().supportedVersions()
                    ))
                    .build()
            );
            return;
        }

        // Check for deprecation warnings
        if (config.versioning().deprecatedVersions().orElse(List.of()).contains(version)) {
            String warning = DEPRECATION_WARNINGS.get(version);
            if (warning != null) {
                requestContext.getHeaders().add("X-API-Deprecation", warning);
                Log.debugf("API version %s is deprecated: %s", version, warning);
            }
        }

        // Store version in context for downstream use
        requestContext.setProperty("api-version", version);
        requestContext.getHeaders().add(VERSION_HEADER, version);

        Log.debugf("API version: %s for path: %s", version, path);
    }

    private String extractVersion(String path, ContainerRequestContext requestContext) {
        // Try to extract from path first (e.g., /api/v1/accounts)
        String[] pathParts = path.split("/");
        for (String part : pathParts) {
            if (part.startsWith("v") && part.length() > 1) {
                return part;
            }
        }

        // Fall back to header
        String headerVersion = requestContext.getHeaderString(VERSION_HEADER);
        if (headerVersion != null && !headerVersion.isBlank()) {
            return headerVersion.startsWith("v") ? headerVersion : "v" + headerVersion;
        }

        // Use default version
        return config.versioning().defaultVersion();
    }

    private boolean isValidVersion(String version) {
        List<String> supportedVersions = config.versioning().supportedVersions();
        return supportedVersions.contains(version);
    }
}
