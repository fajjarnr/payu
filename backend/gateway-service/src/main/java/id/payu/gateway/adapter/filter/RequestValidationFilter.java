package id.payu.gateway.adapter.filter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import id.payu.gateway.config.GatewayConfig;
import io.quarkus.logging.Log;
import io.smallrye.common.annotation.RunOnVirtualThread;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Filter to validate incoming requests against JSON schemas (IMP-008).
 * <p>
 * Loads JSON Schema files from classpath (schemas/ directory) and validates
 * POST/PUT/PATCH request bodies against them.
 * <p>
 * Schema naming convention:
 *   schemas/{service-prefix}-{operation}.json
 * Example:
 *   schemas/accounts-create.json       → POST /api/v1/accounts
 *   schemas/transactions-create.json   → POST /api/v1/transactions
 *   schemas/auth-login.json            → POST /api/v1/auth/login
 */
@Provider
@ApplicationScoped
@RunOnVirtualThread
public class RequestValidationFilter implements ContainerRequestFilter {

    private static final Set<String> VALIDATABLE_METHODS = Set.of("POST", "PUT", "PATCH");
    private static final String SCHEMA_BASE_PATH = "schemas/";

    /**
     * Maps path patterns to schema file names.
     * First match wins (longest prefix match).
     */
    private static final Map<String, String> PATH_SCHEMA_MAP;

    static {
        Map<String, String> map = new java.util.LinkedHashMap<>();
        // Auth endpoints
        map.put("/api/v1/auth/login", "auth-login.json");
        map.put("/api/v1/auth/register", "auth-register.json");
        map.put("/api/v1/auth/refresh", "auth-refresh.json");

        // Account endpoints
        map.put("/api/v1/accounts/register", "accounts-register.json");
        map.put("/api/v1/accounts", "accounts-create.json");

        // Transaction endpoints
        map.put("/api/v1/transactions/transfer", "transactions-transfer.json");
        map.put("/api/v1/transactions", "transactions-create.json");

        // Payment endpoints
        map.put("/api/v1/payments", "payments-create.json");
        // Virtual Account endpoints (must be more specific than /api/v1/payments
        // to win longest-prefix match in the filter's path resolution)
        map.put("/api/v1/payments/va", "payments-va-create.json");
        map.put("/api/v1/payments/va/{vaId}", "payments-va-create.json");

        // Partner endpoints
        map.put("/api/v1/partners", "partners-create.json");

        PATH_SCHEMA_MAP = Map.copyOf(map);
    }

    @Inject
    GatewayConfig config;

    @Inject
    ObjectMapper objectMapper;

    private JsonSchemaFactory schemaFactory;

    /**
     * Cache loaded schemas to avoid re-parsing on every request.
     */
    private final ConcurrentHashMap<String, JsonSchema> schemaCache = new ConcurrentHashMap<>();

    @jakarta.annotation.PostConstruct
    void init() {
        this.schemaFactory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
        preloadSchemas();
    }

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        if (!config.validation().enabled()) {
            return;
        }

        String path = requestContext.getUriInfo().getPath();
        // Skip auth OIDC endpoints — code exchange payload not JSON-schema validated
        if (path.startsWith("/api/v1/auth") || path.startsWith("/api/auth") || path.startsWith("/v1/auth")) {
            return;
        }
        if (path.startsWith("/q/") || path.equals("/health") || path.equals("/status")
                || path.equals("/version")) {
            return;
        }
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
            byte[] bodyBytes = requestContext.getEntityStream().readAllBytes();
            String requestBody = new String(bodyBytes);

            // Always reset stream for downstream processing
            requestContext.setEntityStream(new java.io.ByteArrayInputStream(bodyBytes));

            if (requestBody.isBlank()) {
                return;
            }

            // Parse JSON first
            JsonNode jsonNode;
            try {
                jsonNode = objectMapper.readTree(requestBody);
            } catch (Exception e) {
                Log.warnf(e, "Failed to parse request body as JSON for %s", path);
                if (config.validation().strictMode()) {
                    requestContext.abortWith(
                        Response.status(Response.Status.BAD_REQUEST)
                            .entity(Map.of(
                                "error", "INVALID_JSON",
                                "message", "Request body contains invalid JSON",
                                "status", 400
                            ))
                            .build()
                    );
                }
                return;
            }

            // Look up and validate against schema
            JsonSchema schema = getSchemaForPath(path);
            if (schema != null) {
                Set<ValidationMessage> validationResult = schema.validate(jsonNode);

                if (!validationResult.isEmpty()) {
                    var errors = validationResult.stream()
                        .map(vm -> Map.of(
                                "field", vm.getInstanceLocation() != null
                                        ? vm.getInstanceLocation().toString() : "",
                                "message", vm.getMessage()
                        ))
                        .collect(Collectors.toList());

                    String errorSummary = validationResult.stream()
                        .map(ValidationMessage::getMessage)
                        .collect(Collectors.joining("; "));

                    Log.warnf("Schema validation failed for %s: %s", path, errorSummary);
                    requestContext.abortWith(
                        Response.status(Response.Status.BAD_REQUEST)
                            .entity(Map.of(
                                "error", "SCHEMA_VALIDATION_FAILED",
                                "message", "Request does not match expected schema",
                                "status", 400,
                                "validationErrors", errors
                            ))
                            .build()
                    );
                } else {
                    Log.debugf("Schema validation passed for %s", path);
                }
            }
        } catch (Exception e) {
            if (config.validation().strictMode()) {
                Log.errorf(e, "Request validation failed for %s", path);
                requestContext.abortWith(
                    Response.status(Response.Status.BAD_REQUEST)
                        .entity(Map.of(
                            "error", "VALIDATION_ERROR",
                            "message", "Request validation failed: " + e.getMessage(),
                            "status", 400
                        ))
                        .build()
                );
            } else {
                Log.warnf(e, "Schema validation skipped due to error for %s", path);
            }
        }
    }

    /**
     * Look up JSON schema for a given request path.
     * Uses longest prefix match from PATH_SCHEMA_MAP, then loads from classpath cache.
     */
    JsonSchema getSchemaForPath(String path) {
        // Find best matching schema path (longest prefix match)
        String bestMatch = null;
        String bestSchemaFile = null;

        for (Map.Entry<String, String> entry : PATH_SCHEMA_MAP.entrySet()) {
            String pattern = entry.getKey();
            if (path.equals(pattern) || path.startsWith(pattern + "/")) {
                if (bestMatch == null || pattern.length() > bestMatch.length()) {
                    bestMatch = pattern;
                    bestSchemaFile = entry.getValue();
                }
            }
        }

        if (bestSchemaFile == null) {
            return null;
        }

        return schemaCache.get(bestSchemaFile);
    }

    /**
     * Pre-load all schema files from classpath at startup.
     */
    private void preloadSchemas() {
        int loaded = 0;
        for (String schemaFile : PATH_SCHEMA_MAP.values()) {
            if (schemaCache.containsKey(schemaFile)) {
                continue; // already loaded (deduplication for shared schemas)
            }
            try {
                String resourcePath = SCHEMA_BASE_PATH + schemaFile;
                InputStream is = Thread.currentThread().getContextClassLoader()
                        .getResourceAsStream(resourcePath);
                if (is == null) {
                    // Fallback to class classloader (works in Surefire tests)
                    is = RequestValidationFilter.class.getClassLoader()
                            .getResourceAsStream(resourcePath);
                }
                if (is != null) {
                    JsonSchema schema = schemaFactory.getSchema(is);
                    schemaCache.put(schemaFile, schema);
                    loaded++;
                    Log.debugf("Loaded validation schema: %s", resourcePath);
                } else {
                    Log.debugf("Schema file not found (validation will be skipped): %s", resourcePath);
                }
            } catch (Exception e) {
                Log.warnf(e, "Failed to load schema: %s", schemaFile);
            }
        }
        Log.infof("RequestValidationFilter initialized: %d schemas loaded, %d mappings configured",
                loaded, PATH_SCHEMA_MAP.size());
    }
}
