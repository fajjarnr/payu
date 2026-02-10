package id.payu.auth.domain.port.in;

import id.payu.auth.domain.model.LoginContext;
import id.payu.auth.dto.SessionValidationResponse;
import org.springframework.security.core.Authentication;

/**
 * Inbound port for risk evaluation and session validation use cases.
 */
public interface RiskEvaluationUseCase {
    int evaluateRisk(LoginContext context);
    void recordSuccessfulLogin(String username, LoginContext context);
    void recordFailedAttempt(String username);
    boolean isAccountActive(String userId);
}
