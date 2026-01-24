package id.payu.gateway.filter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import id.payu.gateway.config.GatewayConfig;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Filter to validate incoming requests against JSON schemas.
 * Performs schema validation for POST/PUT/PATCH requests.
 */
@Provider
@ApplicationScoped
public class RequestValidationFilter implements ContainerRequestFilter {

    private static final Set<String> VALIDATABLE_METHODS = Set.of("POST", "PUT", "PATCH");

    @Inject
    GatewayConfig config;

    @Inject
    ObjectMapper objectMapper;

    private JsonSchemaFactory schemaFactory;

    @jakarta.annotation.PostConstruct
    void init() {
        this.schemaFactory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
    }

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        if (!config.validation().enabled()) {
            return;
        }

        // Skip health and metrics endpoints
        String path = requestContext.getUriInfo().getPath();
        if (path.startsWith("/q/") || path.equals("/health")) {
            return;
        }

        // Only validate write operations
        String method = requestContext.getMethod();
        if (!VALIDATABLE_METHODS.contains(method)) {
            return;
        }

        // Check request size
        long contentLength = requestContext.getLength();
        if (contentLength > config.validation().maxRequestSize()) {
            Log.warnf("Request size exceeded: %d bytes (max: %d)", contentLength, config.validation().maxRequestSize());
            requestContext.abortWith(
                Response.status(Response.Status.REQUEST_ENTITY_TOO_LARGE)
                    .entity(Map.of(
                        "error", "REQUEST_TOO_LARGE",
                        "message", "Request size exceeds maximum allowed size",
                        "maxSize", config.validation().maxRequestSize()
                    ))
                    .build()
            );
            return;
        }

        // Perform schema validation if enabled
        if (config.validation().schemaValidation()) {
            validateSchema(requestContext, path);
        }
    }

    private void validateSchema(ContainerRequestContext requestContext, String path) {
        try {
            // Read request body
            String requestBody = new String(requestContext.getEntityStream().readAllBytes());

            if (requestBody.isBlank()) {
                // Empty body is allowed for some endpoints
                return;
            }

            // Parse JSON
            JsonNode jsonNode = objectMapper.readTree(requestBody);

            // Get schema for this endpoint
            JsonSchema schema = getSchemaForPath(path);

            if (schema != null) {
                Set<ValidationMessage> validationResult = schema.validate(jsonNode);

                if (!validationResult.isEmpty()) {
                    String errors = validationResult.stream()
                        .map(ValidationMessage::getMessage)
                        .collect(Collectors.joining(", "));

                    Log.warnf("Schema validation failed for %s: %s", path, errors);
                    requestContext.abortWith(
                        Response.status(Response.Status.BAD_REQUEST)
                            .entity(Map.of(
                                "error", "SCHEMA_VALIDATION_FAILED",
                                "message", "Request does not match expected schema",
                                "details", errors
                            ))
                            .build()
                    );
                } else {
                    Log.debugf("Schema validation passed for %s", path);
                }
            }

            // Reset stream for downstream filters
            requestContext.setEntityStream(new java.io.ByteArrayInputStream(requestBody.getBytes()));

        } catch (Exception e) {
            if (config.validation().strictMode()) {
                Log.errorf(e, "Request validation failed for %s", path);
                requestContext.abortWith(
                    Response.status(Response.Status.BAD_REQUEST)
                        .entity(Map.of(
                            "error", "INVALID_JSON",
                            "message", "Request body contains invalid JSON"
                        ))
                        .build()
                );
            } else {
                Log.warnf(e, "Schema validation skipped due to error for %s", path);
            }
        }
    }

    private JsonSchema getSchemaForPath(String path) {
        // TODO: Implement schema retrieval based on path
        // This could load schemas from a database or filesystem
        // For now, return null to skip validation
        return null;
    }
}
