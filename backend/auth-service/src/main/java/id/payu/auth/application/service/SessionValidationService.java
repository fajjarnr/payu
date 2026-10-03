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
 * Session validation without token refresh.
 * Thread-safe (stateless); results are cached to reduce JWT decoding overhead.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SessionValidationService {

    private final JwtDecoder jwtDecoder;
    private final RiskEvaluationService riskEvaluationService;

    public SessionValidationResponse validateSession(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            log.warn("Session validation failed: No authentication");
            return SessionValidationResponse.invalid();
        }

        try {
            Jwt jwt = (Jwt) authentication.getPrincipal();

            if (isTokenExpired(jwt)) {
                log.warn("Session validation failed: Token expired for user: {}", jwt.getSubject());
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

            Set<String> roles = extractRoles(jwt);

            long expiresIn = calculateExpiresIn(jwt);

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

    private boolean isTokenExpired(Jwt jwt) {
        Instant expiresAt = jwt.getExpiresAt();
        return expiresAt != null && Instant.now().isAfter(expiresAt);
    }

    private Set<String> extractRoles(Jwt jwt) {
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

        var scope = jwt.getClaimAsString("scope");
        if (scope != null) {
            return Set.of(scope.split(" "));
        }

        return Set.of();
    }

    private long calculateExpiresIn(Jwt jwt) {
        Instant expiresAt = jwt.getExpiresAt();
        if (expiresAt == null) {
            return 0L;
        }
        long secondsRemaining = expiresAt.getEpochSecond() - Instant.now().getEpochSecond();
        return Math.max(0L, secondsRemaining);
    }

    /**
     * Validates a token string directly, without a Spring Security context.
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

    private Set<String> extractRolesFromJwtOnly(Jwt jwt) {
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

        var scope = jwt.getClaimAsString("scope");
        if (scope != null) {
            return Set.of(scope.split(" "));
        }

        return Set.of();
    }
}
