package id.payu.gateway.adapter.filter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import id.payu.gateway.config.GatewayConfig;
import io.quarkus.logging.Log;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;

import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Response masking filter for partner/external API endpoints (IMP-009).
 * <p>
 * Strips internal fields (trace IDs, internal error codes, debug info, etc.)
 * from responses on partner-facing paths. This prevents leaking internal
 * implementation details to external consumers.
 * <p>
 * Configuration:
 * - gateway.response-masking.enabled: enable/disable masking
 * - gateway.response-masking.blacklisted-fields: fields to remove
 * - gateway.response-masking.masked-paths: path prefixes that trigger masking
 */
@Provider
@ApplicationScoped
public class ResponseMaskingFilter implements ContainerResponseFilter {

    private static final String MASKING_ACTIVE_PROP = "response-masking-active";

    @Inject
    GatewayConfig config;

    @Inject
    ObjectMapper objectMapper;

    private Set<String> blacklistedFields;
    private List<String> maskedPaths;

    @PostConstruct
    void init() {
        GatewayConfig.ResponseMaskingConfig maskingConfig = config.responseMasking();
        this.blacklistedFields = new HashSet<>(maskingConfig.blacklistedFields());
        this.maskedPaths = maskingConfig.maskedPaths();
        Log.infof("ResponseMaskingFilter initialized: %d blacklisted fields, %d masked paths",
                blacklistedFields.size(), maskedPaths.size());
    }

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext)
            throws IOException {

        if (!config.responseMasking().enabled()) {
            return;
        }

        String path = requestContext.getUriInfo().getPath();

        // Only mask responses for partner/external API paths
        if (!shouldMask(path)) {
            return;
        }

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
            String responseBody;
            if (entity instanceof String s) {
                responseBody = s;
            } else {
                responseBody = objectMapper.writeValueAsString(entity);
            }

            if (responseBody == null || responseBody.isBlank()) {
                return;
            }

            JsonNode root = objectMapper.readTree(responseBody);
            int removedCount = maskFields(root);

            if (removedCount > 0) {
                String maskedBody = objectMapper.writeValueAsString(root);
                responseContext.setEntity(maskedBody);
                Log.debugf("Masked %d fields from response for path: %s", removedCount, path);
            }
        } catch (Exception e) {
            // Don't fail the response if masking fails
            Log.warnf(e, "Response masking failed for path: %s", path);
        }
    }

    /**
     * Check if the path should trigger response masking.
     */
    boolean shouldMask(String path) {
        if (path == null) return false;
        for (String maskedPath : maskedPaths) {
            if (path.startsWith(maskedPath) || path.equals(maskedPath)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Recursively remove blacklisted fields from a JSON tree.
     *
     * @return number of fields removed
     */
    int maskFields(JsonNode node) {
        if (node == null) return 0;

        int removed = 0;

        if (node.isObject()) {
            ObjectNode objectNode = (ObjectNode) node;
            Iterator<String> fieldNames = objectNode.fieldNames();
            Set<String> toRemove = new HashSet<>();

            while (fieldNames.hasNext()) {
                String fieldName = fieldNames.next();
                if (blacklistedFields.contains(fieldName)) {
                    toRemove.add(fieldName);
                } else {
                    // Recursively process nested objects/arrays
                    removed += maskFields(objectNode.get(fieldName));
                }
            }

            for (String field : toRemove) {
                objectNode.remove(field);
                removed++;
            }
        } else if (node.isArray()) {
            for (JsonNode element : node) {
                removed += maskFields(element);
            }
        }

        return removed;
    }
}
