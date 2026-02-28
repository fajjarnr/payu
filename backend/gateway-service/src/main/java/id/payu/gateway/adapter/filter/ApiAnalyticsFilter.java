package id.payu.gateway.adapter.filter;

import id.payu.gateway.application.service.PersistentAnalyticsService;
import io.quarkus.logging.Log;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;

/**
 * Filter to track API analytics with persistent storage.
 * Records request counts, response times, and error rates per partner/endpoint/method.
 *
 * <p>
 * This filter implements IMP-016: Persistent API Analytics.
 * Features:
 * - Per-partner, per-endpoint, per-method tracking
 * - Persistent storage to Redis/TimescaleDB
 * - 90 days detailed retention, 1 year aggregated retention
 */
@Provider
@ApplicationScoped
@Priority(Priorities.USER)
public class ApiAnalyticsFilter implements ContainerResponseFilter {

    private static final String START_TIME_PROPERTY = "analytics-start-time";

    @Inject
    PersistentAnalyticsService analyticsService;

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) {
        // Record start time for later use
        Long startTime = (Long) requestContext.getProperty(START_TIME_PROPERTY);

        if (startTime == null) {
            startTime = System.currentTimeMillis();
            requestContext.setProperty(START_TIME_PROPERTY, startTime);
        }

        long duration = System.currentTimeMillis() - startTime;

        String path = requestContext.getUriInfo().getPath();
        String method = requestContext.getMethod();
        int statusCode = responseContext.getStatus();

        // Extract partner ID if available
        String partnerId = extractPartnerId(requestContext);

        // Extract client info
        String clientIp = getClientIp(requestContext);
        String userAgent = requestContext.getHeaderString("User-Agent");
        String correlationId = requestContext.getHeaderString("X-Correlation-Id");

        // Record metrics asynchronously
        try {
            analyticsService.recordEvent(partnerId, path, method, statusCode,
                duration, clientIp, userAgent, correlationId);
            Log.debugf("Recorded analytics: [%s] %s %s -> %d (%dms)",
                partnerId != null ? partnerId : "anonymous", method, path, statusCode, duration);
        } catch (Exception e) {
            // Don't fail requests if analytics fails
            Log.warnf(e, "Failed to record analytics for %s %s", method, path);
        }
    }

    private String extractPartnerId(ContainerRequestContext requestContext) {
        // Try to get partner ID from header
        String partnerId = requestContext.getHeaderString("X-Partner-Id");
        if (partnerId != null && !partnerId.isBlank()) {
            return partnerId;
        }

        // Try to get from API key
        String apiKey = requestContext.getHeaderString("X-API-Key");
        if (apiKey != null && !apiKey.isBlank()) {
            return derivePartnerFromApiKey(apiKey);
        }

        return null;
    }

    private String derivePartnerFromApiKey(String apiKey) {
        // Simplified mapping - in production, use a proper API key to partner mapping
        if (apiKey.startsWith("tokobapak_")) {
            return "tokobapak";
        } else if (apiKey.startsWith("nobar_")) {
            return "nobar";
        } else if (apiKey.startsWith("demo_")) {
            return "demo-partner";
        }
        return null;
    }

    private String getClientIp(ContainerRequestContext requestContext) {
        String forwarded = requestContext.getHeaderString("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        String realIp = requestContext.getHeaderString("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp;
        }
        return "unknown";
    }
}
