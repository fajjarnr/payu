package id.payu.billing.idempotency;

import io.quarkus.redis.client.RedisClient;
import io.vertx.redis.client.Response;
import jakarta.inject.Inject;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response.ResponseBuilder;
import org.jboss.logging.Logger;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.Base64;

/**
 * Interceptor for handling idempotency in Quarkus JAX-RS resources.
 * Uses Redis for storing idempotency keys and responses.
 */
@Interceptor
@Idempotent
public class IdempotencyInterceptor {

    private static final Logger LOG = Logger.getLogger(IdempotencyInterceptor.class);
    private static final String IDEMPOTENCY_PREFIX = "idempotency:";

    @Inject
    RedisClient redisClient;

    @Context
    ContainerRequestContext requestContext;

    @AroundInvoke
    public Object handleIdempotency(InvocationContext context) throws Exception {
        Idempotent idempotent = context.getMethod().getAnnotation(Idempotent.class);
        if (idempotent == null) {
            idempotent = context.getTarget().getClass().getAnnotation(Idempotent.class);
        }

        if (idempotent == null) {
            return context.proceed();
        }

        String headerName = idempotent.headerName();
        String idempotencyKey = requestContext.getHeaderString(headerName);

        // Check if idempotency key is required but missing
        if (idempotent.required() && (idempotencyKey == null || idempotencyKey.isBlank())) {
            LOG.warn("Idempotency-Key header is required but missing");
            return jakarta.ws.rs.core.Response.status(400)
                    .entity(new ErrorResponse("IDEMPOTENCY_KEY_REQUIRED", "Idempotency-Key header is required"))
                    .build();
        }

        // If not required and no key provided, proceed normally
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return context.proceed();
        }

        String redisKey = IDEMPOTENCY_PREFIX + idempotencyKey;
        String requestFingerprint = computeRequestFingerprint();

        // Check for existing response
        Response cachedResponse = redisClient.get(redisKey);
        if (cachedResponse != null) {
            String cachedData = cachedResponse.toString();

            // Check if it's an in-progress request
            if (cachedData.startsWith("IN_PROGRESS:")) {
                String cachedFingerprint = cachedData.substring("IN_PROGRESS:".length());
                if (!cachedFingerprint.equals(requestFingerprint)) {
                    return jakarta.ws.rs.core.Response.status(409)
                            .entity(new ErrorResponse("IDEMPOTENCY_KEY_REUSE", "Idempotency-Key was already used with a different request body"))
                            .build();
                }
                return jakarta.ws.rs.core.Response.status(409)
                        .entity(new ErrorResponse("IDEMPOTENCY_IN_PROGRESS", "A request with this Idempotency-Key is currently being processed"))
                        .build();
            }

            // Return cached response
            String[] parts = cachedData.split(":", 3);
            if (parts.length >= 3) {
                String cachedFingerprint = parts[0];
                int statusCode = Integer.parseInt(parts[1]);
                String responseBody = parts[2];

                if (!cachedFingerprint.equals(requestFingerprint)) {
                    return jakarta.ws.rs.core.Response.status(409)
                            .entity(new ErrorResponse("IDEMPOTENCY_KEY_REUSE", "Idempotency-Key was already used with a different request body"))
                            .build();
                }

                LOG.debug("Returning cached idempotency response for key: " + idempotencyKey);
                return jakarta.ws.rs.core.Response.status(statusCode)
                        .entity(responseBody)
                        .build();
            }
        }

        // Mark as in-progress
        long ttlSeconds = Duration.ofHours(idempotent.ttlHours()).getSeconds();
        redisClient.setex(redisKey, String.valueOf(ttlSeconds), "IN_PROGRESS:" + requestFingerprint);

        try {
            // Execute the method
            Object result = context.proceed();

            // Store the response
            if (result instanceof jakarta.ws.rs.core.Response response) {
                String responseBody = response.getEntity() != null ? response.getEntity().toString() : "";
                String storedData = requestFingerprint + ":" + response.getStatus() + ":" + responseBody;
                redisClient.setex(redisKey, String.valueOf(ttlSeconds), storedData);
            }

            return result;
        } catch (Exception e) {
            // Remove in-progress marker on error
            redisClient.del(redisKey);
            throw e;
        }
    }

    private String computeRequestFingerprint() {
        try {
            // Create a fingerprint based on request URI and method
            String data = requestContext.getMethod() + ":" + requestContext.getUriInfo().getPath();
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            return "fallback-fingerprint";
        }
    }

    public record ErrorResponse(String code, String message) {}
}
