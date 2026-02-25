package id.payu.auth.adapter.web;

import id.payu.api.common.constant.ErrorCode;
import id.payu.api.common.controller.BaseController;
import id.payu.api.common.controller.RateLimit;
import id.payu.api.common.exception.BusinessException;
import id.payu.api.common.response.ApiResponse;
import id.payu.auth.domain.model.LoginContext;
import id.payu.auth.dto.LoginRequest;
import id.payu.auth.dto.LoginResponse;
import id.payu.auth.dto.RefreshTokenRequest;
import id.payu.auth.dto.RefreshTokenResponse;
import id.payu.auth.dto.SessionValidationResponse;
import id.payu.auth.exception.AuthDomainException;
import id.payu.auth.adapter.security.KeycloakService;
import id.payu.auth.adapter.persistence.RefreshTokenService;
import id.payu.auth.application.service.RiskEvaluationService;
import id.payu.auth.application.service.SessionValidationService;
import id.payu.security.annotation.Audited;
import id.payu.security.annotation.Audited.AuditLevel;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
    @PostMapping("/login")
    @Audited(
            operation = id.payu.security.annotation.Audited.Operation.LOGIN,
            entityType = "User",
            maskData = true,
            level = AuditLevel.INFO
    )
    @Operation(
            summary = "User login",
            description = """
                    Authenticates a user with username and password.
                    Returns JWT access token on success or prompts for MFA if risk evaluation requires it.

                    **Risk-based Authentication:**
                    - Low risk: Returns access token immediately
                    - Medium/High risk: Returns MFA token for OTP verification

                    **Rate Limiting:** 10 requests per minute per IP
                    """
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Successful login",
                    content = @Content(schema = @Schema(implementation = LoginResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Invalid credentials | Invalid request format",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "429",
                    description = "Too many login attempts",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            )
    })
    @SecurityRequirements  // No authentication required for login
    @RateLimit(requests = 10, windowSeconds = 60, keyPrefix = "login")
    public ResponseEntity<ApiResponse<?>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest
    ) {
        LoginContext context = buildLoginContext(request.username(), httpRequest);

        try {
            // BUG-BE-154: Removed double password call — login directly instead of
            // validating credentials first then logging in again (was sending password twice to Keycloak)

            // Evaluate risk for telemetry
            riskEvaluationService.evaluateRisk(context);

            // Direct login — returns tokens if credentials are valid, throws on failure
            LoginResponse loginResponse = keycloakService.loginBlocking(
                    request.username(),
                    request.password()
            );

            if (loginResponse == null) {
                riskEvaluationService.recordFailedAttempt(request.username());
                log.warn("Failed login attempt for user: {}", request.username());
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error(
                                ErrorCode.AUTH_BUS_001.getCode(),
                                ErrorCode.AUTH_BUS_001.getMessage()
                        ));
            }

            // Clear failed attempts counter on successful login
            riskEvaluationService.recordSuccessfulLogin(request.username(), context);

            log.info("Successful login for user: {}", request.username());
            return ResponseEntity.ok(ApiResponse.success(loginResponse));

        } catch (org.springframework.security.authentication.BadCredentialsException e) {
            // BUG-BE-154: Handle invalid credentials from loginBlocking directly
            riskEvaluationService.recordFailedAttempt(request.username());
            log.warn("Failed login attempt for user: {}", request.username());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(
                            ErrorCode.AUTH_BUS_001.getCode(),
                            ErrorCode.AUTH_BUS_001.getMessage()
                    ));
        } catch (Exception e) {
            // SECURITY: Don't log full stack trace to prevent information disclosure
            log.error("Login failed for user: {} - {}", request.username(), e.getClass().getSimpleName());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(
                            ErrorCode.INTERNAL_ERROR.getCode(),
                            ErrorCode.INTERNAL_ERROR.getMessage()
                    ));
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
            operation = id.payu.security.annotation.Audited.Operation.OTHER,
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
}
