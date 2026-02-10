package id.payu.gateway.adapter.filter;

import io.quarkus.logging.Log;
import io.quarkus.redis.datasource.ReactiveRedisDataSource;
import io.quarkus.redis.datasource.value.ReactiveValueCommands;
import io.smallrye.mutiny.Uni;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.time.Duration;
import java.util.Map;

/**
 * Gateway-level authorization filter that validates JWT tokens
 * and forwards user context to downstream services.
 * Implements centralized authentication to reduce burden on individual services.
 */
@Provider
@ApplicationScoped
public class AuthorizationFilter implements ContainerRequestFilter {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String USER_ID_HEADER = "X-User-Id";
    private static final String ACCOUNT_ID_HEADER = "X-Account-Id";
    private static final String ROLES_HEADER = "X-User-Roles";

    // Public endpoints that don't require authentication
    private static final String[] PUBLIC_ENDPOINTS = {
        "/api/v1/auth/login",
        "/api/v1/accounts/register",  // Registration endpoint in account-service
        "/api/v1/auth/refresh",
        "/api/v1/otp/send",
        "/api/v1/otp/verify",
        "/api/v1/health",
        "/health",
        "/q/",
        "/api/v1/partners/webhook",
        "/api/v1/bi-fast/callback",
        "/api/v1/qris/callback",
        "/api/v1/public/"             // Public content endpoints (CMS, etc.)
    };

    // Exact match public endpoints (must match exactly)
    private static final String[] EXACT_PUBLIC_ENDPOINTS = {
        "/api/v1/accounts/register",
        "/api/v1/auth/login"
    };

    @Inject
    @ConfigProperty(name = "gateway.authorization.enabled", defaultValue = "true")
    boolean authorizationEnabled;

    @Inject
    @ConfigProperty(name = "gateway.authorization.jwt-secret", defaultValue = "")
    String jwtSecret;

    @Inject
    ReactiveRedisDataSource redisDataSource;

    private ReactiveValueCommands<String, String> valueCommands;

    @PostConstruct
    void init() {
        this.valueCommands = redisDataSource.value(String.class);
    }

    @Override
    public void filter(ContainerRequestContext requestContext) {
        if (!authorizationEnabled) {
            Log.debug("Authorization filter is disabled");
            return;
        }

        String path = requestContext.getUriInfo().getPath();
        
        // Skip public endpoints
        if (isPublicEndpoint(path)) {
            Log.debugf("Skipping authorization for public endpoint: %s", path);
            return;
        }

        // Get Authorization header
        String authHeader = requestContext.getHeaderString(AUTHORIZATION_HEADER);
        Log.infof("GW Auth Filter: path=%s, header=%s", path, authHeader);
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            Log.warnf("Missing or invalid Authorization header for path: %s", path);
            abortWithUnauthorized(requestContext, "MISSING_TOKEN", "Valid JWT token required");
            return;
        }

        String token = authHeader.substring(BEARER_PREFIX.length());

        // Validate token and extract user context
        try {
            UserContext userContext = validateToken(token);

            if (userContext == null) {
                Log.warnf("Invalid token for path: %s", path);
                abortWithUnauthorized(requestContext, "INVALID_TOKEN", "Token validation failed");
                return;
            }

            // Forward user context to downstream services via headers
            requestContext.getHeaders().add(USER_ID_HEADER, userContext.userId);
            requestContext.getHeaders().add(ACCOUNT_ID_HEADER, userContext.accountId);
            requestContext.getHeaders().add(ROLES_HEADER, String.join(",", userContext.roles));

            Log.debugf("Authorization successful for user: %s on path: %s", userContext.userId, path);

        } catch (Exception e) {
            Log.errorf(e, "Authorization error for path: %s", path);
            abortWithUnauthorized(requestContext, "AUTH_ERROR", "Authorization processing error");
        }
    }

    private boolean isPublicEndpoint(String path) {
        if (path == null) return false;
        
        // Normalize path: ensure leading slash and remove trailing slash for comparison
        String normalizedPath = path.startsWith("/") ? path : "/" + path;
        if (normalizedPath.length() > 1 && normalizedPath.endsWith("/")) {
            normalizedPath = normalizedPath.substring(0, normalizedPath.length() - 1);
        }

        // Check exact matches
        for (String endpoint : EXACT_PUBLIC_ENDPOINTS) {
            if (normalizedPath.equals(endpoint)) {
                return true;
            }
        }

        // Check prefix/pattern matches
        for (String endpoint : PUBLIC_ENDPOINTS) {
            if (normalizedPath.startsWith(endpoint)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Validate JWT token and extract user context.
     * In production, this would use proper JWT validation.
     * For now, it's a placeholder that demonstrates the pattern.
     */
    private UserContext validateToken(String token) {
        try {
            // TODO: Implement proper JWT validation using a JWT library
            // For now, this is a simplified version

            // Check if token is blacklisted (for logout scenarios)
            String blacklisted = valueCommands.get("blacklist:token:" + token)
                    .await().atMost(Duration.ofSeconds(1));

            if (blacklisted != null) {
                Log.warn("Token is blacklisted");
                return null;
            }

            // TODO: Parse and validate JWT signature
            // This would typically use:
            // - io.quarkus:quarkus-oidc or
            // - com.nimbusds:nimbus-jose-jwt

            // For demonstration, extract from token (in production, use proper JWT parsing)
            // This is a simplified implementation
            if (token.length() < 10) {
                return null;
            }

            // Placeholder: In production, decode JWT and extract claims
            return UserContext.builder()
                    .userId("user-" + token.substring(0, 8))
                    .accountId("account-" + token.substring(0, 8))
                    .roles(java.util.List.of("ROLE_USER"))
                    .build();

        } catch (Exception e) {
            Log.errorf(e, "Token validation error");
            return null;
        }
    }

    private void abortWithUnauthorized(ContainerRequestContext requestContext, String errorCode, String message) {
        requestContext.abortWith(
            Response.status(Response.Status.UNAUTHORIZED)
                .header("Content-Type", "application/json")
                .entity(Map.of(
                    "error", errorCode,
                    "message", message,
                    "timestamp", java.time.Instant.now().toString()
                ))
                .build()
        );
    }

    /**
     * User context extracted from JWT token.
     */
    private static class UserContext {
        private final String userId;
        private final String accountId;
        private final java.util.List<String> roles;

        private UserContext(Builder builder) {
            this.userId = builder.userId;
            this.accountId = builder.accountId;
            this.roles = builder.roles;
        }

        public String getUserId() {
            return userId;
        }

        public String getAccountId() {
            return accountId;
        }

        public java.util.List<String> getRoles() {
            return roles;
        }

        static Builder builder() {
            return new Builder();
        }

        static class Builder {
            private String userId;
            private String accountId;
            private java.util.List<String> roles = java.util.Collections.emptyList();

            Builder userId(String userId) {
                this.userId = userId;
                return this;
            }

            Builder accountId(String accountId) {
                this.accountId = accountId;
                return this;
            }

            Builder roles(java.util.List<String> roles) {
                this.roles = roles;
                return this;
            }

            UserContext build() {
                return new UserContext(this);
            }
        }
    }
}
