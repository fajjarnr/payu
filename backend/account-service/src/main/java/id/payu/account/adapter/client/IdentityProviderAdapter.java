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
    public String provisionUser(String username, String email, String password, String fullName) {
        log.info("Provisioning IAM identity for user: {}", maskUsername(username));

        Map<String, String> request = Map.of(
            "username", username,
            "email", email,
            "password", password,
            "fullName", fullName != null ? fullName : username
        );

        Map<String, Object> response = gatewayClient.registerIdentity(request);
        String userId = extractUserId(response);

        if (userId == null || userId.isBlank()) {
            log.warn("IAM registration response missing user_id for user: {}", maskUsername(username));
            return null;
        }

        log.info("IAM identity provisioned successfully for user: {}", maskUsername(username));
        return userId;
    }

    private String provisionUserFallback(String username, String email, String password, String fullName, Throwable throwable) {
        log.error("Failed to provision IAM identity for user {} after retries: {}",
                maskUsername(username), throwable.getMessage());
        throw new RuntimeException(
            "Identity provider unavailable. Please try again later.", throwable
        );
    }

    @Override
    @CircuitBreaker(name = "authService", fallbackMethod = "deleteUserFallback")
    public void deleteUser(String iamUserId) {
        log.info("Deleting IAM identity: {}", iamUserId);
        gatewayClient.deleteIdentity(iamUserId);
        log.info("IAM identity deleted: {}", iamUserId);
    }

    private void deleteUserFallback(String iamUserId, Throwable throwable) {
        // ACCOUNT-005: compensation is best-effort — the IAM user is left in
        // place and the ERROR line is the orphan alert for manual cleanup.
        log.error("Failed to delete IAM identity {} (orphan risk, manual cleanup required): {}",
                iamUserId, throwable.getMessage());
    }

    @SuppressWarnings("unchecked")
    private String extractUserId(Map<String, Object> response) {
        if (response == null) {
            return null;
        }

        Object data = response.get("data");
        if (data instanceof Map<?, ?> dataMap) {
            Object userId = ((Map<String, Object>) dataMap).get("user_id");
            if (userId != null) {
                return userId.toString();
            }
        }

        return null;
    }

    private String maskUsername(String username) {
        if (username == null || username.length() < 4) return "***";
        return username.substring(0, 3) + "****";
    }
}
