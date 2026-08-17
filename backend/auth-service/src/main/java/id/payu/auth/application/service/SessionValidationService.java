package id.payu.auth.application.service;

import id.payu.auth.interfaces.dto.SessionValidationResponse;
import id.payu.auth.exception.AuthDomainException;
import id.payu.api.common.constant.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service for validating user sessions and JWT tokens.
 * Provides session validation without requiring token refresh.
 *
 * Thread-safe: Uses stateless operations with Spring Security's Authentication.
 * Cacheable: Validation results are cached to reduce JWT decoding overhead.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SessionValidationService {

    private final JwtDecoder jwtDecoder;
    private final RiskEvaluationService riskEvaluationService;

    /**
     * Validates the current session based on JWT token from Authentication.
     *
     * @param authentication the Spring Security authentication object
     * @return SessionValidationResponse with validation result and user data
     * @throws AuthDomainException if token is invalid or expired
     */
    public SessionValidationResponse validateSession(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            log.warn("Session validation failed: No authentication");
            return SessionValidationResponse.invalid();
        }

        try {
            // Extract JWT from authentication principal
            Jwt jwt = (Jwt) authentication.getPrincipal();

            // Validate token expiration
            if (isTokenExpired(jwt)) {
                log.warn("Session validation failed: Token expired for user: {}", jwt.getSubject());
                return SessionValidationResponse.invalid();
            }

            // Extract user claims
            String userId = jwt.getSubject();
            String username = jwt.getClaimAsString("preferred_username");
            if (username == null) {
                username = jwt.getClaimAsString("user_name");
            }
            if (username == null) {
                username = userId;
            }

            // Extract roles from JWT
            Set<String> roles = extractRoles(jwt);

            // Calculate token expiration time in seconds
            long expiresIn = calculateExpiresIn(jwt);

            // Check if user account is active (not locked/disabled)
            boolean isAccountActive = riskEvaluationService.isAccountActive(userId);

            if (!isAccountActive) {
                log.warn("Session validation failed: Account not active for user: {}", userId);
                return SessionValidationResponse.invalid();
            }

            log.debug("Session validated successfully for user: {}", userId);
            return SessionValidationResponse.valid(userId, username, expiresIn, roles);

        } catch (ClassCastException e) {
            log.error("Session validation failed: Invalid authentication principal type", e);
            return SessionValidationResponse.invalid();
        } catch (JwtException e) {
            log.error("Session validation failed: JWT validation error", e);
            return SessionValidationResponse.invalid();
        } catch (Exception e) {
            log.error("Session validation failed: Unexpected error", e);
            return SessionValidationResponse.invalid();
        }
    }

    /**
     * Checks if the JWT token has expired.
     *
     * @param jwt the JWT token
     * @return true if token is expired, false otherwise
     */
    private boolean isTokenExpired(Jwt jwt) {
        Instant expiresAt = jwt.getExpiresAt();
        return expiresAt != null && Instant.now().isAfter(expiresAt);
    }

    /**
     * Extracts roles from JWT claims.
     *
     * @param jwt the JWT token
     * @return Set of role names
     */
    private Set<String> extractRoles(Jwt jwt) {
        // Try to extract from realm_access.roles
        @SuppressWarnings("unchecked")
        var realmAccess = jwt.getClaimAsMap("realm_access");
        if (realmAccess != null && realmAccess.containsKey("roles")) {
            var roles = (java.util.List<String>) realmAccess.get("roles");
            if (roles != null) {
                return roles.stream()
                        .map(String::toString)
                        .collect(Collectors.toSet());
            }
        }

        // Try to extract from resource_access
        @SuppressWarnings("unchecked")
        var resourceAccess = jwt.getClaimAsMap("resource_access");
        if (resourceAccess != null) {
            @SuppressWarnings("unchecked")
            var clientAccess = (java.util.Map<String, Object>) resourceAccess.get("payu-client");
            if (clientAccess != null && clientAccess.containsKey("roles")) {
                @SuppressWarnings("unchecked")
                var roles = (java.util.List<String>) clientAccess.get("roles");
                if (roles != null) {
                    return roles.stream()
                            .map(String::toString)
                            .collect(Collectors.toSet());
                }
            }
        }

        // Fallback to scope claim
        var scope = jwt.getClaimAsString("scope");
        if (scope != null) {
            return Set.of(scope.split(" "));
        }

        return Set.of();
    }

    /**
     * Calculates the remaining time until token expiration in seconds.
     *
     * @param jwt the JWT token
     * @return seconds until expiration, or 0 if token is expired
     */
    private long calculateExpiresIn(Jwt jwt) {
        Instant expiresAt = jwt.getExpiresAt();
        if (expiresAt == null) {
            return 0L;
        }
        long secondsRemaining = expiresAt.getEpochSecond() - Instant.now().getEpochSecond();
        return Math.max(0L, secondsRemaining);
    }

    /**
     * Validates a JWT token string directly without Spring Security context.
     * This is useful for validation in non-HTTP contexts or testing.
     *
     * @param tokenString the JWT token string
     * @return SessionValidationResponse with validation result
     */
    public SessionValidationResponse validateToken(String tokenString) {
        try {
            Jwt jwt = jwtDecoder.decode(tokenString);

            if (isTokenExpired(jwt)) {
                return SessionValidationResponse.invalid();
            }

            String userId = jwt.getSubject();
            String username = jwt.getClaimAsString("preferred_username");
            if (username == null) {
                username = jwt.getClaimAsString("user_name");
            }
            if (username == null) {
                username = userId;
            }

            Set<String> roles = extractRolesFromJwtOnly(jwt);
            long expiresIn = calculateExpiresIn(jwt);

            boolean isAccountActive = riskEvaluationService.isAccountActive(userId);
            if (!isAccountActive) {
                return SessionValidationResponse.invalid();
            }

            return SessionValidationResponse.valid(userId, username, expiresIn, roles);

        } catch (JwtException e) {
            log.error("Token validation failed: {}", e.getMessage());
            return SessionValidationResponse.invalid();
        }
    }

    /**
     * Extracts roles from JWT claims only (no authentication object).
     *
     * @param jwt the JWT token
     * @return Set of role names
     */
    private Set<String> extractRolesFromJwtOnly(Jwt jwt) {
        // Try to extract from realm_access.roles
        @SuppressWarnings("unchecked")
        var realmAccess = jwt.getClaimAsMap("realm_access");
        if (realmAccess != null && realmAccess.containsKey("roles")) {
            var roles = (java.util.List<String>) realmAccess.get("roles");
            if (roles != null) {
                return roles.stream()
                        .map(String::toString)
                        .collect(Collectors.toSet());
            }
        }

        // Try to extract from resource_access
        @SuppressWarnings("unchecked")
        var resourceAccess = jwt.getClaimAsMap("resource_access");
        if (resourceAccess != null) {
            @SuppressWarnings("unchecked")
            var clientAccess = (java.util.Map<String, Object>) resourceAccess.get("payu-client");
            if (clientAccess != null && clientAccess.containsKey("roles")) {
                @SuppressWarnings("unchecked")
                var roles = (java.util.List<String>) clientAccess.get("roles");
                if (roles != null) {
                    return roles.stream()
                            .map(String::toString)
                            .collect(Collectors.toSet());
                }
            }
        }

        // Fallback to scope claim
        var scope = jwt.getClaimAsString("scope");
        if (scope != null) {
            return Set.of(scope.split(" "));
        }

        return Set.of();
    }
}
