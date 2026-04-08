package id.payu.account.adapter.client;

import id.payu.account.domain.port.out.IdentityProviderPort;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Adapter for provisioning users in Keycloak via auth-service (through gateway).
 * Follows the same Hexagonal Architecture pattern as KycVerificationAdapter.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class IdentityProviderAdapter implements IdentityProviderPort {

    private final GatewayClient gatewayClient;

    @Override
    @CircuitBreaker(name = "authService", fallbackMethod = "provisionUserFallback")
    @Retry(name = "authService")
    public void provisionUser(String username, String email, String password, String fullName) {
        log.info("Provisioning IAM identity for user: {}", maskUsername(username));

        Map<String, String> request = Map.of(
            "username", username,
            "email", email,
            "password", password,
            "fullName", fullName != null ? fullName : username
        );

        gatewayClient.registerIdentity(request);
        log.info("IAM identity provisioned successfully for user: {}", maskUsername(username));
    }

    private void provisionUserFallback(String username, String email, String password, String fullName, Throwable throwable) {
        log.error("Failed to provision IAM identity for user {} after retries: {}",
                maskUsername(username), throwable.getMessage());
        throw new RuntimeException(
            "Identity provider unavailable. Please try again later.", throwable
        );
    }

    private String maskUsername(String username) {
        if (username == null || username.length() < 4) return "***";
        return username.substring(0, 3) + "****";
    }
}
