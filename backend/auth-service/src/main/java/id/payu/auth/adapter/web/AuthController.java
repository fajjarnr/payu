package id.payu.auth.adapter.web;

import id.payu.api.common.constant.ErrorCode;
import id.payu.api.common.controller.BaseController;
import id.payu.api.common.controller.RateLimit;
import id.payu.api.common.exception.BusinessException;
import id.payu.api.common.response.ApiResponse;
import id.payu.auth.domain.model.LoginContext;
import id.payu.auth.interfaces.dto.LoginResponse;
import id.payu.auth.interfaces.dto.LogoutRequest;
import id.payu.auth.interfaces.dto.OidcCallbackRequest;
import id.payu.auth.interfaces.dto.RegisterRequest;
import id.payu.auth.interfaces.dto.RefreshTokenRequest;
import id.payu.auth.interfaces.dto.RefreshTokenResponse;
import id.payu.auth.interfaces.dto.SessionValidationResponse;
import id.payu.auth.exception.AuthDomainException;
import id.payu.auth.adapter.security.KeycloakService;
import id.payu.auth.application.service.RefreshTokenService;
import id.payu.auth.application.service.RiskEvaluationService;
import id.payu.auth.application.service.SessionValidationService;
import id.payu.security.annotation.Audited;
import id.payu.security.annotation.AuditLevel;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import id.payu.security.annotation.AuditOperation;

/**
 * Authentication controller for PayU Digital Banking Platform.
 * Handles user authentication, MFA verification, and risk-based authentication.
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Authentication", description = "User authentication and MFA endpoints")
public class AuthController extends BaseController {

    private final KeycloakService keycloakService;
    private final RiskEvaluationService riskEvaluationService;
    private final RefreshTokenService refreshTokenService;
    private final SessionValidationService sessionValidationService;

    /**
     * Authenticate user with username and password.
     * Returns JWT tokens or prompts for MFA if required by risk evaluation.
     */
    private String maskUsername(String username) {
        if (username == null || username.length() < 4) return "***";
        return username.substring(0, 3) + "****" + (username.contains("@") ? username.substring(username.indexOf("@")) : "");
    }

    /**
     * LOGIN-003: OIDC authorization-code + PKCE callback. The browser was
     * redirected to Keycloak's login page (BFF /api/auth/authorize) and
     * Keycloak redirected back with a code; this endpoint exchanges the code
     * and PKCE verifier for tokens. Credentials never reach this service.
     */
    @PostMapping("/callback")
    @Audited(
            operation = id.payu.security.annotation.AuditOperation.LOGIN,
            entityType = "User",
            maskData = true,
            level = AuditLevel.INFO
    )
    @Operation(
            summary = "OIDC authorization code exchange (PKCE)",
            description = """
                    Exchanges an OIDC authorization code + PKCE verifier for tokens.
                    The user authenticated at Keycloak's own login page; this
                    endpoint never receives credentials.

                    **Errors:** 400 AUTH_BUS_009 invalid/expired code, 429 rate
                    limited, 503 identity provider unavailable.
                    """
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Successful exchange",
                    content = @Content(schema = @Schema(implementation = LoginResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Invalid or expired authorization code",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "429",
                    description = "Too many attempts",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            )
    })
    @SecurityRequirements  // No authentication required — the code IS the auth artifact
    @RateLimit(requests = 30, windowSeconds = 60, keyPrefix = "login-callback")
    public ResponseEntity<ApiResponse<?>> callback(
            @Valid @RequestBody OidcCallbackRequest request,
            HttpServletRequest httpRequest
    ) {
        try {
            LoginResponse loginResponse = keycloakService.exchangeAuthorizationCode(
                    request.code(), request.codeVerifier(), request.redirectUri());

            // Risk telemetry (non-critical): derive the username from the ID token.
            // It must never prevent a successful login from returning tokens.
            String username = extractPreferredUsername(loginResponse.accessToken());
            if (username != null) {
                try {
                    riskEvaluationService.recordSuccessfulLogin(
                            username, buildLoginContext(username, httpRequest));
                } catch (Exception riskEx) {
                    log.warn("Failed to record successful login for risk profile (non-critical): {} - {}",
                            riskEx.getClass().getSimpleName(), riskEx.getMessage());
                }
            }

            log.info("OIDC login completed for user: {}", maskUsername(username == null ? "unknown" : username));
            return ResponseEntity.ok(ApiResponse.success(loginResponse));

        } catch (id.payu.api.common.exception.RateLimitExceededException e) {
            // LOGIN-005: rate limited (either limiter) is a deterministic 429
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(ApiResponse.error(
                            ErrorCode.RATE_LIMIT_EXCEEDED.getCode(),
                            ErrorCode.RATE_LIMIT_EXCEEDED.getMessage()
                    ));
        } catch (io.github.resilience4j.ratelimiter.RequestNotPermitted e) {
            // LOGIN-005: Resilience4j @RateLimiter denial is also a 429
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(ApiResponse.error(
                            ErrorCode.RATE_LIMIT_EXCEEDED.getCode(),
                            ErrorCode.RATE_LIMIT_EXCEEDED.getMessage()
                    ));
        } catch (IllegalArgumentException e) {
            // LOGIN-003: Keycloak rejected the code (invalid/expired) — deterministic 400
            log.warn("OIDC code exchange rejected: {}", e.getClass().getSimpleName());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(
                            ErrorCode.AUTH_BUS_009.getCode(),
                            ErrorCode.AUTH_BUS_009.getMessage()
                    ));
        } catch (org.springframework.web.client.ResourceAccessException e) {
            // LOGIN-005: identity provider unreachable — deterministic 503, fail-closed
            log.error("OIDC code exchange failed - identity provider unavailable: {}",
                    e.getClass().getSimpleName());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(ApiResponse.error(
                            ErrorCode.SERVICE_UNAVAILABLE.getCode(),
                            ErrorCode.SERVICE_UNAVAILABLE.getMessage()
                    ));
        } catch (Exception e) {
            // SECURITY: Don't log full stack trace to prevent information disclosure
            log.error("OIDC code exchange failed: {}", e.getClass().getSimpleName());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(
                            ErrorCode.INTERNAL_ERROR.getCode(),
                            ErrorCode.INTERNAL_ERROR.getMessage()
                    ));
        }
    }

    /**
     * Decodes preferred_username from the access token payload (unverified —
     * telemetry only; the token was just issued by Keycloak over TLS).
     */
    private String extractPreferredUsername(String accessToken) {
        if (accessToken == null) return null;
        try {
            String[] parts = accessToken.split("\\.");
            if (parts.length != 3) return null;
            String payload = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
            int idx = payload.indexOf("\"preferred_username\":");
            if (idx < 0) return null;
            int start = payload.indexOf('"', idx + 20) + 1;
            int end = payload.indexOf('"', start);
            if (start <= 0 || end <= start) return null;
            return payload.substring(start, end);
        } catch (Exception e) {
            return null;
        }
    }



    /**
     * Builds login context from HTTP request.
     */
    private LoginContext buildLoginContext(String username, HttpServletRequest request) {
        return new LoginContext(
                username,
                getClientIpAddress(request),
                request.getHeader("X-Device-ID"),
                request.getHeader("User-Agent"),
                System.currentTimeMillis()
        );
    }

    /**
     * Gets the client IP address from request headers.
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }

    /**
     * Refresh access token using refresh token with token rotation.
     *
     * <p>This endpoint implements refresh token rotation for enhanced security:
     * <ul>
     *   <li>Validates the old refresh token</li>
     *   <li>Invalidates the old token after successful refresh</li>
     *   <li>Issues a new refresh token (rotation)</li>
     *   <li>Returns both new access token and new refresh token</li>
     * </ul>
     *
     * <p><b>Security Features:</b>
     * <ul>
     *   <li>Token rotation prevents replay attacks</li>
     *   <li>Refresh tokens stored as hashed values in Redis</li>
     *   <li>7-day expiration on refresh tokens</li>
     *   <li>Detection of token reuse attempts</li>
     * </ul>
     *
     * <p><b>Rate Limiting:</b> 20 requests per minute per IP
     *
     * @param request The refresh token request containing the refresh_token
     * @param httpRequest The HTTP servlet request for rate limiting key
     * @return ApiResponse containing the new tokens
     */
    @PostMapping("/refresh")
    @Audited(
            operation = id.payu.security.annotation.AuditOperation.OTHER,
            entityType = "AuthToken",
            maskData = true,
            level = AuditLevel.INFO
    )
    @Operation(
            summary = "Refresh access token",
            description = """
                    Refreshes an expired access token using a valid refresh token.
                    Implements token rotation where the old refresh token is invalidated
                    and a new one is issued.

                    **Security Features:**
                    - Token rotation prevents replay attacks
                    - Old refresh token is invalidated after successful refresh
                    - Refresh tokens are hashed before storage
                    - Automatic detection of token reuse attempts

                    **Token Lifetime:**
                    - Access token: 1 hour
                    - Refresh token: 7 days

                    **Rate Limiting:** 20 requests per minute per IP
                    """
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Token refreshed successfully",
                    content = @Content(schema = @Schema(implementation = RefreshTokenResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Invalid refresh token | Token expired",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "429",
                    description = "Too many refresh attempts",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            )
    })
    @SecurityRequirements
    @RateLimit(requests = 20, windowSeconds = 60, keyPrefix = "refresh")
    public ResponseEntity<ApiResponse<RefreshTokenResponse>> refreshToken(
            @Valid @RequestBody RefreshTokenRequest request,
            HttpServletRequest httpRequest
    ) {
        try {
            // Step 1: Use Keycloak to get new access token using the provided refresh token
            LoginResponse keycloakResponse = keycloakService.refreshTokenBlocking(request.refreshToken());

            // Step 2: Build response with new tokens
            RefreshTokenResponse response = new RefreshTokenResponse(
                    keycloakResponse.accessToken(),
                    keycloakResponse.refreshToken(),
                    keycloakResponse.expiresIn(),
                    7 * 24 * 3600L, // default 7 days for new refresh token lifetime
                    keycloakResponse.tokenType()
            );

            log.info("Successfully refreshed token for client IP: {}", getClientIpAddress(httpRequest));
            return ResponseEntity.ok(ApiResponse.success(response));

        } catch (org.springframework.security.authentication.BadCredentialsException e) {
            log.warn("Refresh token validation failed for IP: {} - {}",
                    getClientIpAddress(httpRequest), e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(
                            ErrorCode.AUTH_BUS_006.getCode(),
                            ErrorCode.AUTH_BUS_006.getMessage()
                    ));
        } catch (IllegalArgumentException e) {
            // LOGIN-002: a revoked/rotated/expired refresh token (Keycloak
            // invalid_grant) is a deterministic 400, not a 500
            log.warn("Refresh token rejected for IP: {} - {}",
                    getClientIpAddress(httpRequest), e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(
                            ErrorCode.AUTH_BUS_006.getCode(),
                            ErrorCode.AUTH_BUS_006.getMessage()
                    ));
        } catch (id.payu.api.common.exception.RateLimitExceededException e) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(ApiResponse.error(
                            ErrorCode.RATE_LIMIT_EXCEEDED.getCode(),
                            ErrorCode.RATE_LIMIT_EXCEEDED.getMessage()
                    ));
        } catch (Exception e) {
            // SECURITY: Don't log full stack trace to prevent information disclosure
            log.error("Token refresh failed for IP: {} - {}",
                    getClientIpAddress(httpRequest), e.getClass().getSimpleName(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(
                            ErrorCode.INTERNAL_ERROR.getCode(),
                            ErrorCode.INTERNAL_ERROR.getMessage()
                    ));
        }
    }

    /**
     * Register a new user in Keycloak identity provider.
     * Called by account-service during the onboarding flow to provision IAM credentials.
     */
    @PostMapping("/register")
    @Audited(
            operation = id.payu.security.annotation.AuditOperation.CREATE,
            entityType = "User",
            maskData = true,
            level = AuditLevel.INFO
    )
    @Operation(
            summary = "Register user in IAM",
            description = "Creates a new user in Keycloak with the provided credentials. " +
                    "Called internally by account-service during registration."
    )
    @SecurityRequirements
    @RateLimit(requests = 10, windowSeconds = 60, keyPrefix = "register")
    public ResponseEntity<ApiResponse<?>> register(
            @Valid @RequestBody RegisterRequest request
    ) {
        try {
            String userId = keycloakService.createUser(
                    request.username(),
                    request.email(),
                    request.password(),
                    request.fullName()
            );
            log.info("Registered user in IAM: {}", maskUsername(request.username()));
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success(Map.of(
                            "message", "User registered in IAM",
                            "user_id", userId
                    )));
        } catch (IllegalArgumentException e) {
            log.warn("Registration rejected for {}: {}", maskUsername(request.username()), e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(
                            ErrorCode.AUTH_BUS_001.getCode(),
                            e.getMessage()
                    ));
        } catch (Exception e) {
            log.error("IAM registration failed for {}: {}", maskUsername(request.username()), e.getClass().getSimpleName());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(
                            ErrorCode.INTERNAL_ERROR.getCode(),
                            "Failed to register user in identity provider"
                    ));
        }
    }

    /**
     * ACCOUNT-005: delete a provisioned IAM user (saga compensation from
     * account-service when local persistence fails after provisioning).
     * Internal-only like {@link #register(RegisterRequest)}.
     *
     * @param userId the IAM user id to delete
     */
    @DeleteMapping("/users/{userId}")
    @Audited(
            operation = AuditOperation.DELETE,
            entityType = "User",
            level = AuditLevel.INFO
    )
    @Operation(
            summary = "Delete user in IAM",
            description = "Removes a user from Keycloak. Called internally by account-service "
                    + "as saga compensation when registration fails after IAM provisioning."
    )
    @SecurityRequirements
    @RateLimit(requests = 10, windowSeconds = 60, keyPrefix = "delete-user")
    public ResponseEntity<ApiResponse<?>> deleteUser(
            @PathVariable String userId
    ) {
        try {
            keycloakService.deleteUser(userId);
            log.info("Deleted user in IAM: {}", userId);
            return ResponseEntity.ok(ApiResponse.success(Map.of(
                    "message", "User deleted from IAM"
            )));
        } catch (IllegalArgumentException e) {
            log.warn("User deletion rejected: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(
                            ErrorCode.AUTH_BUS_001.getCode(),
                            e.getMessage()
                    ));
        } catch (Exception e) {
            log.error("IAM user deletion failed for {}: {}", userId, e.getClass().getSimpleName());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(
                            ErrorCode.INTERNAL_ERROR.getCode(),
                            "Failed to delete user in identity provider"
                    ));
        }
    }

    /**
     * Validates the current user session without requiring token refresh.
     *
     * <p>This endpoint provides a lightweight way to validate that the user's session
     * is still active and retrieve minimal session information. It does not generate
     * new tokens, making it more efficient than the refresh endpoint for session checks.
     *
     * <p><b>Use Cases:</b>
     * <ul>
     *   <li>Check if session is still valid on app initialization</li>
     *   <li>Verify session before sensitive operations</li>
     *   <li>Get user profile data without full token refresh</li>
     * </ul>
     *
     * <p><b>Response Data:</b>
     * <ul>
     *   <li>valid: true if session is active</li>
     *   <li>user_id: the user's unique identifier</li>
     *   <li>username: the user's username</li>
     *   <li>expires_in: seconds until token expiration</li>
     *   <li>roles: user's assigned roles</li>
     *   <li>session_active: true if session is active</li>
     * </ul>
     *
     * <p><b>Security:</b>
     * <ul>
     *   <li>Requires valid JWT token in Authorization header</li>
     *   <li>Does NOT expose sensitive data (NIK, phone, email)</li>
     *   <li>Checks token expiration and account status</li>
     *   <li>Rate limited to 100 requests per minute</li>
     * </ul>
     *
     * @param authentication The Spring Security authentication object (injected)
     * @return ApiResponse containing session validation result
     */
    @GetMapping("/validate")
    @Operation(
            summary = "Validate session",
            description = """
                    Validates the current user session without requiring token refresh.
                    Returns minimal session data including user ID, username, roles, and token expiration.

                    **Use Cases:**
                    - Check if session is valid on app initialization
                    - Verify session before sensitive operations
                    - Get user profile data without full token refresh

                    **Rate Limiting:** 100 requests per minute per user
                    """
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Session validation result",
                    content = @Content(schema = @Schema(implementation = SessionValidationResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "No valid session (not authenticated)",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            )
    })
    @RateLimit(requests = 100, windowSeconds = 60, keyPrefix = "validate")
    public ResponseEntity<ApiResponse<SessionValidationResponse>> validateSession(
            Authentication authentication
    ) {
        try {
            SessionValidationResponse response = sessionValidationService.validateSession(authentication);

            if (response.valid()) {
                log.debug("Session validated for user: {}", response.userId());
                return ResponseEntity.ok(ApiResponse.success(response));
            } else {
                log.warn("Invalid session validation attempt");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.error(
                                ErrorCode.UNAUTHORIZED.getCode(),
                                ErrorCode.UNAUTHORIZED.getMessage()
                        ));
            }
        } catch (Exception e) {
            log.error("Session validation failed: {}", e.getClass().getSimpleName());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(
                            ErrorCode.INTERNAL_ERROR.getCode(),
                            ErrorCode.INTERNAL_ERROR.getMessage()
                    ));
        }
    }

    /**
     * Logs out by revoking the session at the identity provider (LOGIN-002).
     * Keycloak's end_session endpoint invalidates the refresh token server-side,
     * so a replay of the revoked refresh token is rejected on the next use.
     */
    @PostMapping("/logout")
    @Audited(
            operation = id.payu.security.annotation.AuditOperation.LOGOUT,
            entityType = "AuthToken",
            maskData = true,
            level = AuditLevel.INFO
    )
    @Operation(
            summary = "Logout and revoke session",
            description = """
                    Revokes the session at the identity provider using the refresh token.
                    After revocation, the refresh token cannot be replayed to obtain new tokens.

                    **Rate Limiting:** 20 requests per minute per IP
                    """
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Session revoked successfully"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Invalid or missing refresh token",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "503",
                    description = "Identity provider unavailable",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            )
    })
    @SecurityRequirements
    @RateLimit(requests = 20, windowSeconds = 60, keyPrefix = "logout")
    public ResponseEntity<ApiResponse<Void>> logout(@Valid @RequestBody LogoutRequest request) {
        try {
            keycloakService.revokeSession(request.refreshToken());
            log.info("Session revoked for client IP via logout");
            return ResponseEntity.ok(ApiResponse.success(null));
        } catch (IllegalArgumentException e) {
            log.warn("Logout rejected: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(
                            ErrorCode.AUTH_BUS_006.getCode(),
                            ErrorCode.AUTH_BUS_006.getMessage()
                    ));
        } catch (org.springframework.web.client.ResourceAccessException e) {
            log.error("Logout failed — identity provider unavailable: {}", e.getClass().getSimpleName());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(ApiResponse.error(
                            ErrorCode.SERVICE_UNAVAILABLE.getCode(),
                            ErrorCode.SERVICE_UNAVAILABLE.getMessage()
                    ));
        } catch (Exception e) {
            log.error("Logout failed: {}", e.getClass().getSimpleName());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(
                            ErrorCode.INTERNAL_ERROR.getCode(),
                            ErrorCode.INTERNAL_ERROR.getMessage()
                    ));
        }
    }
}
