package id.payu.auth.controller;

import id.payu.auth.dto.LoginContext;
import id.payu.auth.dto.LoginRequest;
import id.payu.auth.dto.LoginResponse;
import id.payu.auth.dto.MFAResponse;
import id.payu.auth.dto.MFAVerifyRequest;
import id.payu.auth.exception.MFAException;
import id.payu.auth.service.KeycloakService;
import id.payu.auth.service.MFATokenService;
import id.payu.auth.service.RiskEvaluationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.CompletableFuture;

/**
 * Authentication controller using Spring MVC (servlet API).
 * Handles login, MFA verification, and risk evaluation.
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final KeycloakService keycloakService;
    private final RiskEvaluationService riskEvaluationService;
    private final MFATokenService mfaTokenService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request,
                                    HttpServletRequest httpRequest) {
        LoginContext context = buildLoginContext(request.username(), httpRequest);

        try {
            // Validate credentials (blocking call)
            Boolean isValid = keycloakService.validateCredentialsBlocking(request.username(), request.password());

            if (Boolean.FALSE.equals(isValid)) {
                return ResponseEntity.badRequest().build();
            }

            // Evaluate risk (synchronous)
            RiskEvaluationService.RiskEvaluationResult riskResult =
                    riskEvaluationService.evaluateRisk(context);

            if (riskResult.isMfaRequired()) {
                var mfaToken = mfaTokenService.generateMFAToken(request.username());
                return ResponseEntity.ok(
                        new MFAResponse(
                                true,
                                mfaToken.mfaToken(),
                                (mfaToken.expiresAt() - System.currentTimeMillis()) / 1000,
                                riskResult.getMessage()
                        )
                );
            }

            // Complete login (blocking)
            LoginResponse loginResponse = keycloakService.loginBlocking(request.username(), request.password());
            return ResponseEntity.ok(loginResponse);

        } catch (Exception e) {
            log.error("Login failed for user: {}", request.username(), e);
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/mfa/verify")
    public ResponseEntity<LoginResponse> verifyMFA(@Valid @RequestBody MFAVerifyRequest request,
                                                    HttpServletRequest httpRequest) {
        LoginContext context = buildLoginContext(request.username(), httpRequest);

        try {
            LoginResponse response = keycloakService.verifyMFAAndCompleteLoginBlocking(
                    request.mfaToken(),
                    request.otpCode(),
                    request.username(),
                    "",
                    context
            );
            return ResponseEntity.ok(response);

        } catch (MFAException e) {
            log.warn("MFA verification failed: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("MFA verification failed for user: {}", request.username(), e);
            return ResponseEntity.badRequest().build();
        }
    }

    private LoginContext buildLoginContext(String username, HttpServletRequest request) {
        return new LoginContext(
                username,
                getClientIpAddress(request),
                request.getHeader("X-Device-ID"),
                request.getHeader("User-Agent"),
                System.currentTimeMillis()
        );
    }

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
