package id.payu.auth.application.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class BusinessMetrics {
    private final Counter loginSuccess;
    private final Counter loginFailure;
    private final Counter tokenRefresh;
    private final Counter mfaChallenges;
    private final Counter revokedRefresh;
    private final Counter dpopNonceRetry;
    private final Counter dpopInvalid;
    private final Counter deviceCodeIssued;
    private final Counter deviceTokenPolled;

    public BusinessMetrics(MeterRegistry registry) {
        this.loginSuccess = Counter.builder("payu.auth.login.success")
                .description("Number of successful login attempts").register(registry);
        this.loginFailure = Counter.builder("payu.auth.login.failure")
                .description("Number of failed login attempts").register(registry);
        this.tokenRefresh = Counter.builder("payu.auth.token.refresh")
                .description("Number of token refresh operations").register(registry);
        this.mfaChallenges = Counter.builder("payu.auth.mfa.challenges")
                .description("Number of MFA challenges issued").register(registry);
        this.revokedRefresh = Counter.builder("keycloak_revoked_refresh_total")
                .description("Revoked refresh token replays rejected (maxReuse=0)").register(registry);
        this.dpopNonceRetry = Counter.builder("dpop_nonce_retry_total")
                .description("DPoP nonce challenges issued").register(registry);
        this.dpopInvalid = Counter.builder("dpop_invalid_proof_total")
                .description("Invalid DPoP proofs rejected").register(registry);
        this.deviceCodeIssued = Counter.builder("payu.auth.device.code_issued")
                .description("Device authorization codes issued").register(registry);
        this.deviceTokenPolled = Counter.builder("payu.auth.device.token_polled")
                .description("Device token polls").register(registry);
    }

    public void recordLoginSuccess() { loginSuccess.increment(); }
    public void recordLoginFailure() { loginFailure.increment(); }
    public void recordTokenRefresh() { tokenRefresh.increment(); }
    public void recordMfaChallenge() { mfaChallenges.increment(); }
    public void recordRevokedRefresh() { revokedRefresh.increment(); }
    public void recordDpopNonceRetry() { dpopNonceRetry.increment(); }
    public void recordDpopInvalid() { dpopInvalid.increment(); }
    public void recordDeviceCodeIssued() { deviceCodeIssued.increment(); }
    public void recordDeviceTokenPolled() { deviceTokenPolled.increment(); }
}
