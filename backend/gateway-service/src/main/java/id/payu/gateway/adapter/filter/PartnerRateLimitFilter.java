package id.payu.gateway.adapter.filter;

import id.payu.gateway.application.service.PartnerRateLimitService;
import id.payu.gateway.config.GatewayConfig;
import io.quarkus.logging.Log;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

import java.io.IOException;
import java.util.Map;

/**
 * Filter to enforce per-partner rate limits based on RatePlan configuration.
 *
 * <p>
 * This filter implements IMP-017: Rate Plan per Partner.
 * Features:
 * - Config-driven per partner rate limits
 * - Per-endpoint overrides within plans
 * - Proper rate limit headers (RFC 6585)
 *
 * <p>
 * Priority: After API key validation but before other processing.
 */
@Provider
@ApplicationScoped
@Priority(Priorities.AUTHENTICATION + 10)
public class PartnerRateLimitFilter implements ContainerRequestFilter {

    @Inject
    GatewayConfig config;

    @Inject
    PartnerRateLimitService partnerRateLimitService;

    private boolean enabled;

    @PostConstruct
    void init() {
        // Enabled by default, can be disabled via config
        this.enabled = true;
        Log.infof("PartnerRateLimitFilter initialized (enabled: %s)", enabled);
    }

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        if (!enabled) {
            return;
        }

        String path = requestContext.getUriInfo().getPath();

        // Skip health and metrics endpoints
        if (path.startsWith("/q/") || path.equals("/health") || path.equals("/status")) {
            return;
        }

        String partnerId = extractPartnerId(requestContext);
        if (partnerId == null) {
            // No partner ID, skip partner rate limiting
            // Fall back to global rate limiting
            return;
        }

        partnerRateLimitService.checkRateLimit(partnerId, path)
            .subscribe()
            .with(
                result -> {
                    // Add rate limit headers
                    requestContext.getHeaders().add("X-RateLimit-Limit",
                        String.valueOf(result.limit()));
                    requestContext.getHeaders().add("X-RateLimit-Remaining",
                        String.valueOf(result.remaining()));

                    if (!result.allowed()) {
                        Log.warnf("Partner rate limit exceeded: partner=%s, path=%s, window=%s",
                            partnerId, path, result.limitingWindow());

                        requestContext.abortWith(
                            Response.status(429)
                                .header("Retry-After", String.valueOf(result.retryAfter()))
                                .entity(Map.of(
                                    "error", "PARTNER_RATE_LIMIT_EXCEEDED",
                                    "message", "Rate limit exceeded for partner: " + partnerId,
                                    "retryAfter", result.retryAfter(),
                                    "limitingWindow", result.limitingWindow()
                                ))
                                .build()
                        );
                    } else {
                        // Record the request for analytics
                        partnerRateLimitService.recordRequest(partnerId, path);
                    }
                },
                failure -> {
                    Log.warnf(failure, "Partner rate limit check failed for %s, allowing request", partnerId);
                }
            );
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
            // Map API key to partner ID
            // This is a simplified implementation
            return derivePartnerFromApiKey(apiKey);
        }

        return null;
    }

    static String derivePartnerFromApiKey(String apiKey) {
        // Simplified mapping - in production, use a proper API key to partner mapping
        if (apiKey.startsWith("tokobapak_")) {
            return "tokobapak";
        } else if (apiKey.startsWith("nobar_")) {
            return "nobar";
        }
        return null;
    }
}
