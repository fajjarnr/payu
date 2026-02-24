package id.payu.gateway.adapter.filter;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.BadJOSEException;
import com.nimbusds.jose.proc.JWSKeySelector;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.jwt.proc.ConfigurableJWTProcessor;
import com.nimbusds.jwt.proc.DefaultJWTClaimsVerifier;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import io.quarkus.logging.Log;
import io.quarkus.redis.datasource.ReactiveRedisDataSource;
import io.quarkus.redis.datasource.value.ReactiveValueCommands;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.io.IOException;
import java.net.URL;
import java.text.ParseException;
import java.time.Duration;
import java.util.*;

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
        "/api/v1/accounts",           // Account endpoints (account-service handles its own auth)
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
    @ConfigProperty(name = "quarkus.oidc.token.issuer", defaultValue = "http://localhost:8080/realms/payu")
    String jwtIssuer;

    @Inject
    @ConfigProperty(name = "quarkus.oidc.auth-server-url", defaultValue = "http://localhost:8080/realms/payu")
    String authServerUrl;

    @Inject
    @ConfigProperty(name = "quarkus.oidc.token.audience", defaultValue = "gateway-service")
    String jwtAudience;

    @Inject
    ReactiveRedisDataSource redisDataSource;

    private ReactiveValueCommands<String, String> valueCommands;
    private ConfigurableJWTProcessor<SecurityContext> jwtProcessor;
    private JWKSource<SecurityContext> jwkSource;

    @PostConstruct
    void init() {
        this.valueCommands = redisDataSource.value(String.class);
        initJwtProcessor();
    }

    /**
     * Initialize JWT processor with JWKS from Keycloak.
     * This method sets up the JWT validation pipeline including:
     * - Signature verification using RS256
     * - Issuer validation
     * - Audience validation
     * - Expiration validation
     */
    private void initJwtProcessor() {
        try {
            this.jwtProcessor = new DefaultJWTProcessor<>();

            // Build JWKS URI from auth-server-url
            String jwksUri = buildJwksUri();
            Log.infof("Initializing JWT processor with JWKS URI: %s", jwksUri);

            // Load JWKSet from JWKS endpoint
            JWKSet jwkSet = JWKSet.load(new URL(jwksUri));
            this.jwkSource = new ImmutableJWKSet<>(jwkSet);

            // Configure key selector for RS256 algorithm
            JWSAlgorithm expectedJWSAlg = JWSAlgorithm.RS256;
            JWSKeySelector<SecurityContext> keySelector =
                new JWSVerificationKeySelector<>(expectedJWSAlg, jwkSource);
            jwtProcessor.setJWSKeySelector(keySelector);

            // Configure claims verifier for issuer and audience validation
            Set<String> requiredClaims = new HashSet<>(
                Arrays.asList("sub", "exp", "iat")
            );

            // Build expected claims with issuer and audience
            JWTClaimsSet.Builder expectedClaimsBuilder = new JWTClaimsSet.Builder()
                .issuer(jwtIssuer);

            // Add audience validation if configured
            if (jwtAudience != null && !jwtAudience.isBlank()) {
                expectedClaimsBuilder.audience(jwtAudience);
            }

            DefaultJWTClaimsVerifier<SecurityContext> claimsVerifier =
                new DefaultJWTClaimsVerifier<>(
                    expectedClaimsBuilder.build(),
                    requiredClaims
                );
            jwtProcessor.setJWTClaimsSetVerifier(claimsVerifier);

            Log.info("JWT processor initialized successfully");
        } catch (IOException | ParseException e) {
            Log.errorf(e, "Failed to initialize JWT processor. JWKS URI may be unavailable. " +
                "JWT validation will reject all tokens until JWKS is available.");
            // Don't throw - allow service to start, but JWT validation will fail
            this.jwtProcessor = null;
        }
    }

    /**
     * Build JWKS URI from auth-server-url.
     * Converts: http://localhost:8080/realms/payu
     * To:       http://localhost:8080/realms/payu/protocol/openid-connect/certs
     */
    private String buildJwksUri() {
        String baseUrl = authServerUrl;
        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
        return baseUrl + "/protocol/openid-connect/certs";
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
     * Uses nimbus-jose-jwt library for proper JWT validation including:
     * - Signature verification using RS256 and JWKS from Keycloak
     * - Expiration validation (exp claim)
     * - Issuer validation (iss claim)
     * - Audience validation (aud claim)
     * - Required claims validation (sub, exp, iat)
     *
     * @param token The JWT token string (without Bearer prefix)
     * @return UserContext if token is valid, null otherwise
     */
    private UserContext validateToken(String token) {
        try {
            // Check if token is blacklisted (for logout scenarios)
            String blacklisted = valueCommands.get("blacklist:token:" + token)
                    .await().atMost(Duration.ofSeconds(1));

            if (blacklisted != null) {
                Log.warn("Token is blacklisted");
                return null;
            }

            // Check if JWT processor is initialized
            if (jwtProcessor == null) {
                Log.error("JWT processor not initialized - cannot validate token");
                return null;
            }

            // Parse the JWT token
            SignedJWT signedJWT;
            try {
                signedJWT = SignedJWT.parse(token);
            } catch (ParseException e) {
                Log.warnf("Invalid JWT format: %s", e.getMessage());
                return null;
            }

            // Validate token signature and claims
            JWTClaimsSet claimsSet;
            try {
                claimsSet = jwtProcessor.process(signedJWT, null);
            } catch (BadJOSEException e) {
                Log.warnf("JWT validation failed (signature/claims): %s", e.getMessage());
                return null;
            } catch (JOSEException e) {
                Log.warnf("JWT processing error: %s", e.getMessage());
                return null;
            }

            // Extract user context from claims
            String userId = claimsSet.getSubject();
            if (userId == null || userId.isBlank()) {
                Log.warn("JWT missing subject claim");
                return null;
            }

            // Extract account_id from custom claim (Keycloak realm-specific)
            String accountId = extractAccountId(claimsSet);

            // Extract roles from realm_access claim (Keycloak format)
            List<String> roles = extractRoles(claimsSet);

            Log.debugf("JWT validation successful for user: %s", userId);

            return UserContext.builder()
                    .userId(userId)
                    .accountId(accountId)
                    .roles(roles)
                    .build();

        } catch (Exception e) {
            Log.errorf(e, "Token validation error");
            return null;
        }
    }

    /**
     * Extract account ID from JWT claims.
     * Looks for custom claim 'account_id' or uses subject as fallback.
     */
    private String extractAccountId(JWTClaimsSet claimsSet) {
        Object accountIdClaim = claimsSet.getClaim("account_id");
        if (accountIdClaim != null) {
            return accountIdClaim.toString();
        }
        // Fallback: derive from subject
        return "account-" + claimsSet.getSubject();
    }

    /**
     * Extract roles from JWT claims.
     * Supports Keycloak realm_access format and custom roles claim.
     */
    @SuppressWarnings("unchecked")
    private List<String> extractRoles(JWTClaimsSet claimsSet) {
        List<String> roles = new ArrayList<>();

        try {
            // Keycloak format: realm_access.roles
            Object realmAccess = claimsSet.getClaim("realm_access");
            if (realmAccess instanceof Map) {
                Map<String, Object> realmAccessMap = (Map<String, Object>) realmAccess;
                Object realmRoles = realmAccessMap.get("roles");
                if (realmRoles instanceof List) {
                    for (Object role : (List<?>) realmRoles) {
                        if (role != null) {
                            roles.add(role.toString());
                        }
                    }
                }
            }

            // Alternative: resource_access claim for client-specific roles
            Object resourceAccess = claimsSet.getClaim("resource_access");
            if (resourceAccess instanceof Map) {
                Map<String, Object> resourceAccessMap = (Map<String, Object>) resourceAccess;
                for (Map.Entry<String, Object> entry : resourceAccessMap.entrySet()) {
                    if (entry.getValue() instanceof Map) {
                        Map<String, Object> clientAccess = (Map<String, Object>) entry.getValue();
                        Object clientRoles = clientAccess.get("roles");
                        if (clientRoles instanceof List) {
                            for (Object role : (List<?>) clientRoles) {
                                if (role != null) {
                                    roles.add(entry.getKey() + "_" + role.toString());
                                }
                            }
                        }
                    }
                }
            }

            // Fallback: custom roles claim
            if (roles.isEmpty()) {
                Object rolesClaim = claimsSet.getClaim("roles");
                if (rolesClaim instanceof List) {
                    for (Object role : (List<?>) rolesClaim) {
                        if (role != null) {
                            roles.add(role.toString());
                        }
                    }
                }
            }

        } catch (Exception e) {
            Log.warnf("Error extracting roles from JWT: %s", e.getMessage());
        }

        // Default role if none found
        if (roles.isEmpty()) {
            roles.add("ROLE_USER");
        }

        return roles;
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
