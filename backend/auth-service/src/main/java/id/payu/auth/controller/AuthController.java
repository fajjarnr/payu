package id.payu.auth.controller;

import id.payu.api.common.constant.ErrorCode;
import id.payu.api.common.controller.BaseController;
import id.payu.api.common.controller.RateLimit;
import id.payu.api.common.exception.BusinessException;
import id.payu.api.common.response.ApiResponse;
import id.payu.auth.dto.LoginContext;
import id.payu.auth.dto.LoginRequest;
import id.payu.auth.dto.LoginResponse;
import id.payu.auth.dto.MFAResponse;
import id.payu.auth.dto.MFAVerifyRequest;
import id.payu.auth.exception.MFAException;
import id.payu.auth.service.KeycloakService;
import id.payu.auth.service.MFATokenService;
import id.payu.auth.service.RiskEvaluationService;
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
    private final MFATokenService mfaTokenService;

    /**
     * Authenticate user with username and password.
     * Returns JWT tokens or prompts for MFA if required by risk evaluation.
     */
    @PostMapping("/login")
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
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Successful login or MFA required",
                    content = @Content(schema = @Schema(oneOf = {LoginResponse.class, MFAResponse.class}))
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
            // Validate credentials
            Boolean isValid = keycloakService.validateCredentialsBlocking(
                    request.username(),
                    request.password()
            );

            if (Boolean.FALSE.equals(isValid)) {
                log.warn("Failed login attempt for user: {}", request.username());
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error(
                                ErrorCode.AUTH_BUS_001.getCode(),
                                ErrorCode.AUTH_BUS_001.getMessage()
                        ));
            }

            // Evaluate risk
            RiskEvaluationService.RiskEvaluationResult riskResult =
                    riskEvaluationService.evaluateRisk(context);

            if (riskResult.isMfaRequired()) {
                var mfaToken = mfaTokenService.generateMFAToken(request.username());
                return ResponseEntity.ok(
                        ApiResponse.success(new MFAResponse(
                                true,
                                mfaToken.mfaToken(),
                                (mfaToken.expiresAt() - System.currentTimeMillis()) / 1000,
                                riskResult.getMessage()
                        ))
                );
            }

            // Complete login
            LoginResponse loginResponse = keycloakService.loginBlocking(
                    request.username(),
                    request.password()
            );

            log.info("Successful login for user: {}", request.username());
            return ResponseEntity.ok(ApiResponse.success(loginResponse));

        } catch (Exception e) {
            log.error("Login failed for user: {}", request.username(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(
                            ErrorCode.INTERNAL_ERROR.getCode(),
                            ErrorCode.INTERNAL_ERROR.getMessage()
                    ));
        }
    }

    /**
     * Verify MFA OTP code and complete authentication.
     */
    @PostMapping("/mfa/verify")
    @Operation(
            summary = "Verify MFA code",
            description = """
                    Verifies the OTP code sent to the user's registered device
                    and completes the authentication process.

                    **Rate Limiting:** 10 requests per minute per IP
                    """
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "MFA verified successfully, returns JWT tokens",
                    content = @Content(schema = @Schema(implementation = LoginResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Invalid OTP | Expired MFA token",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "429",
                    description = "Too many verification attempts",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            )
    })
    @SecurityRequirements
    @RateLimit(requests = 10, windowSeconds = 60, keyPrefix = "mfa")
    public ResponseEntity<ApiResponse<LoginResponse>> verifyMFA(
            @Valid @RequestBody MFAVerifyRequest request,
            HttpServletRequest httpRequest
    ) {
        LoginContext context = buildLoginContext(request.username(), httpRequest);

        try {
            LoginResponse response = keycloakService.verifyMFAAndCompleteLoginBlocking(
                    request.mfaToken(),
                    request.otpCode(),
                    request.username(),
                    "",
                    context
            );

            log.info("Successful MFA verification for user: {}", request.username());
            return ResponseEntity.ok(ApiResponse.success(response));

        } catch (MFAException e) {
            log.warn("MFA verification failed for user: {} - {}",
                    request.username(), e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(
                            ErrorCode.AUTH_BUS_004.getCode(),
                            ErrorCode.AUTH_BUS_004.getMessage()
                    ));
        } catch (Exception e) {
            log.error("MFA verification failed for user: {}", request.username(), e);
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
}
