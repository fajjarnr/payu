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

    public BusinessMetrics(MeterRegistry registry) {
        this.loginSuccess = Counter.builder("payu.auth.login.success")
                .description("Number of successful login attempts").register(registry);
        this.loginFailure = Counter.builder("payu.auth.login.failure")
                .description("Number of failed login attempts").register(registry);
        this.tokenRefresh = Counter.builder("payu.auth.token.refresh")
                .description("Number of token refresh operations").register(registry);
        this.mfaChallenges = Counter.builder("payu.auth.mfa.challenges")
                .description("Number of MFA challenges issued").register(registry);
    }

    public void recordLoginSuccess() { loginSuccess.increment(); }
    public void recordLoginFailure() { loginFailure.increment(); }
    public void recordTokenRefresh() { tokenRefresh.increment(); }
    public void recordMfaChallenge() { mfaChallenges.increment(); }
}
