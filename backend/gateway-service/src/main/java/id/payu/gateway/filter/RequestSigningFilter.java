package id.payu.gateway.filter;

import id.payu.gateway.config.GatewayConfig;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;

/**
 * Filter to validate HMAC-SHA256 request signatures for partner APIs.
 * Ensures request integrity and authenticity.
 */
@Provider
@ApplicationScoped
public class RequestSigningFilter implements ContainerRequestFilter {

    @Inject
    GatewayConfig config;

    @Override
    public void filter(ContainerRequestContext requestContext) {
        if (!config.requestSigning().enabled()) {
            return;
        }

        String path = requestContext.getUriInfo().getPath();

        // Check if this path requires signature validation
        if (!requiresSignature(path)) {
            return;
        }

        // Get signature and timestamp from headers
        String providedSignature = requestContext.getHeaderString(config.requestSigning().headerName());
        String timestampStr = requestContext.getHeaderString(config.requestSigning().timestampHeader());

        if (providedSignature == null || providedSignature.isBlank()) {
            Log.warnf("Missing signature header for %s", path);
            requestContext.abortWith(
                Response.status(Response.Status.UNAUTHORIZED)
                    .entity(Map.of(
                        "error", "MISSING_SIGNATURE",
                        "message", "Request signature is required for this endpoint"
                    ))
                    .build()
            );
            return;
        }

        if (timestampStr == null || timestampStr.isBlank()) {
            Log.warnf("Missing timestamp header for %s", path);
            requestContext.abortWith(
                Response.status(Response.Status.UNAUTHORIZED)
                    .entity(Map.of(
                        "error", "MISSING_TIMESTAMP",
                        "message", "Request timestamp is required"
                    ))
                    .build()
            );
            return;
        }

        // Validate timestamp
        try {
            long timestamp = Long.parseLong(timestampStr);
            long now = Instant.now().getEpochSecond();
            long diff = Math.abs(now - timestamp);

            if (diff > config.requestSigning().toleranceSeconds()) {
                Log.warnf("Request timestamp too old or too new: %d seconds difference", diff);
                requestContext.abortWith(
                    Response.status(Response.Status.UNAUTHORIZED)
                        .entity(Map.of(
                            "error", "INVALID_TIMESTAMP",
                            "message", "Request timestamp is outside tolerance window",
                            "toleranceSeconds", config.requestSigning().toleranceSeconds()
                        ))
                        .build()
                );
                return;
            }
        } catch (NumberFormatException e) {
            Log.warnf("Invalid timestamp format: %s", timestampStr);
            requestContext.abortWith(
                Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of(
                        "error", "INVALID_TIMESTAMP_FORMAT",
                        "message", "Timestamp must be a valid Unix timestamp"
                    ))
                    .build()
            );
            return;
        }

        // Get partner ID from request (could be from header or JWT)
        String partnerId = requestContext.getHeaderString("X-Partner-Id");
        if (partnerId == null || partnerId.isBlank()) {
            Log.warnf("Missing partner ID header for signed request");
            requestContext.abortWith(
                Response.status(Response.Status.UNAUTHORIZED)
                    .entity(Map.of(
                        "error", "MISSING_PARTNER_ID",
                        "message", "Partner ID is required for signed requests"
                    ))
                    .build()
            );
            return;
        }

        // Get secret key for this partner
        String secretKey = config.requestSigning().partnerKeys().get(partnerId);
        if (secretKey == null) {
            Log.warnf("Unknown partner ID: %s", partnerId);
            requestContext.abortWith(
                Response.status(Response.Status.UNAUTHORIZED)
                    .entity(Map.of(
                        "error", "UNKNOWN_PARTNER",
                        "message", "Partner ID not recognized"
                    ))
                    .build()
            );
            return;
        }

        // Calculate expected signature
        String expectedSignature;
        try {
            expectedSignature = calculateSignature(requestContext, timestampStr, secretKey);
        } catch (Exception e) {
            Log.errorf(e, "Failed to calculate signature");
            requestContext.abortWith(
                Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of(
                        "error", "SIGNATURE_ERROR",
                        "message", "Failed to validate signature"
                    ))
                    .build()
            );
            return;
        }

        // Compare signatures
        if (!expectedSignature.equals(providedSignature)) {
            Log.warnf("Signature mismatch for partner %s: expected=%s, provided=%s",
                partnerId, expectedSignature, providedSignature);
            requestContext.abortWith(
                Response.status(Response.Status.UNAUTHORIZED)
                    .entity(Map.of(
                        "error", "INVALID_SIGNATURE",
                        "message", "Request signature validation failed"
                    ))
                    .build()
            );
            return;
        }

        Log.debugf("Request signature validated successfully for partner %s", partnerId);
    }

    private boolean requiresSignature(String path) {
        List<String> requiredPaths = config.requestSigning().requiredPaths();
        for (String pattern : requiredPaths) {
            // Simple wildcard matching
            String regex = pattern.replace("*", ".*");
            if (path.matches(regex)) {
                return true;
            }
        }
        return false;
    }

    private String calculateSignature(ContainerRequestContext requestContext,
                                       String timestamp, String secretKey) throws Exception {
        // Build string to sign: method + path + timestamp + body
        StringBuilder payload = new StringBuilder();
        payload.append(requestContext.getMethod());
        payload.append("\n");
        payload.append(requestContext.getUriInfo().getRequestUri().getPath());
        payload.append("\n");
        payload.append(timestamp);
        payload.append("\n");

        // Add body hash if present
        // Note: In JAX-RS ContainerRequestContext, we can't read entity stream directly
        // This would need to be implemented with a ReaderInterceptor
        // For now, skip body hashing

        // Decode secret key
        byte[] keyBytes = Base64.getDecoder().decode(secretKey);

        // Calculate HMAC
        try {
            javax.crypto.Mac mac = javax.crypto.Mac.getInstance(config.requestSigning().algorithm());
            javax.crypto.spec.SecretKeySpec keySpec = new javax.crypto.spec.SecretKeySpec(keyBytes, config.requestSigning().algorithm());
            mac.init(keySpec);
            byte[] signature = mac.doFinal(payload.toString().getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(signature);
        } catch (Exception e) {
            throw new RuntimeException("Failed to calculate signature", e);
        }
    }
}
