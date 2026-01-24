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
 * Filter to enforce IP whitelisting for sensitive endpoints.
 * Validates that requests come from allowed IP addresses.
 */
@Provider
@ApplicationScoped
public class IpWhitelistFilter implements ContainerRequestFilter {

    @Inject
    GatewayConfig config;

    @Override
    public void filter(ContainerRequestContext requestContext) {
        if (!config.ipWhitelist().enabled()) {
            return;
        }

        String path = requestContext.getUriInfo().getPath();

        // Skip health and metrics endpoints
        if (path.startsWith("/q/") || path.equals("/health")) {
            return;
        }

        // Check for bypass header
        for (String bypassHeader : config.ipWhitelist().bypassHeaders()) {
            String headerValue = requestContext.getHeaderString(bypassHeader);
            if (headerValue != null && headerValue.equalsIgnoreCase("true")) {
                Log.debugf("IP whitelist bypassed via header: %s", bypassHeader);
                return;
            }
        }

        // Check if path requires IP whitelist
        WhitelistRule rule = getWhitelistRule(path);
        if (rule == null) {
            return; // No rule for this path
        }

        // Get client IP
        String clientIp = getClientIp(requestContext);

        // Validate IP
        if (!rule.isAllowed(clientIp)) {
            Log.warnf("IP blocked by whitelist: %s for path %s", clientIp, path);
            requestContext.abortWith(
                Response.status(Response.Status.FORBIDDEN)
                    .entity(Map.of(
                        "error", "IP_NOT_ALLOWED",
                        "message", "Your IP address is not authorized to access this endpoint"
                    ))
                    .build()
            );
        } else {
            Log.debugf("IP allowed by whitelist: %s for path %s", clientIp, path);
        }
    }

    private WhitelistRule getWhitelistRule(String path) {
        if (config.ipWhitelist().paths() == null) {
            return null;
        }

        for (GatewayConfig.IpWhitelistConfig.IpWhitelistPathConfig pathConfig : config.ipWhitelist().paths()) {
            String pattern = pathConfig.pattern();
            // Simple wildcard matching
            String regex = pattern.replace("*", ".*");
            if (path.matches(regex)) {
                return new WhitelistRule(pathConfig.ips(), config.ipWhitelist().mode());
            }
        }

        return null;
    }

    private String getClientIp(ContainerRequestContext requestContext) {
        // Check X-Forwarded-For header
        String forwarded = requestContext.getHeaderString("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }

        // Check X-Real-IP header
        String realIp = requestContext.getHeaderString("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp;
        }

        // Return unknown
        return "unknown";
    }

    /**
     * Helper class to check IP against whitelist rules.
     */
    private static class WhitelistRule {
        private final List<String> allowedIps;
        private final String mode;

        public WhitelistRule(List<String> allowedIps, String mode) {
            this.allowedIps = allowedIps;
            this.mode = mode;
        }

        public boolean isAllowed(String ip) {
            // In allow mode, IP must be in the list
            if ("allow".equalsIgnoreCase(mode)) {
                return isIpInList(ip);
            }
            // In deny mode, IP must NOT be in the list
            else {
                return !isIpInList(ip);
            }
        }

        private boolean isIpInList(String ip) {
            for (String allowedIp : allowedIps) {
                if (ipMatches(ip, allowedIp)) {
                    return true;
                }
            }
            return false;
        }

        private boolean ipMatches(String ip, String pattern) {
            // Exact match
            if (ip.equals(pattern)) {
                return true;
            }

            // CIDR notation matching (simplified)
            if (pattern.contains("/")) {
                return matchesCidr(ip, pattern);
            }

            return false;
        }

        private boolean matchesCidr(String ip, String cidr) {
            String[] parts = cidr.split("/");
            String networkIp = parts[0];
            int prefixLength = Integer.parseInt(parts[1]);

            // Simple implementation for /8, /16, /24
            String[] ipOctets = ip.split("\\.");
            String[] networkOctets = networkIp.split("\\.");

            if (prefixLength == 8) {
                return ipOctets[0].equals(networkOctets[0]);
            } else if (prefixLength == 16) {
                return ipOctets[0].equals(networkOctets[0]) &&
                       ipOctets[1].equals(networkOctets[1]);
            } else if (prefixLength == 24) {
                return ipOctets[0].equals(networkOctets[0]) &&
                       ipOctets[1].equals(networkOctets[1]) &&
                       ipOctets[2].equals(networkOctets[2]);
            }

            // For other prefix lengths, return false (should implement proper CIDR)
            return false;
        }
    }
}
