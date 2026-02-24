package id.payu.transaction.adapter.client;

import id.payu.transaction.domain.port.out.AccountServicePort;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Adapter for calling account-service REST API.
 * Implements circuit breaker and retry for resilience.
 *
 * <p>This adapter follows the Hexagonal Architecture pattern by implementing
 * the {@link AccountServicePort} interface defined in the domain layer.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AccountServiceAdapter implements AccountServicePort {

    @Value("${services.account.url:http://localhost:8081}")
    private String accountServiceUrl;

    private final RestTemplate restTemplate;

    @Override
    @CircuitBreaker(name = "accountService", fallbackMethod = "getAccountIdsByUserIdFallback")
    @Retry(name = "accountService")
    public List<UUID> getAccountIdsByUserId(String userId) {
        if (userId == null || userId.isBlank()) {
            log.warn("UserId is null or blank, returning empty account list");
            return Collections.emptyList();
        }

        String url = accountServiceUrl + "/api/v1/accounts/users/" + userId + "/account-ids";
        log.debug("Fetching account IDs for user: {}", userId);

        try {
            ResponseEntity<List<UUID>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<UUID>>() {}
            );

            List<UUID> accountIds = response.getBody();
            if (accountIds == null) {
                accountIds = Collections.emptyList();
            }

            log.debug("Found {} accounts for user: {}", accountIds.size(), userId);
            return accountIds;
        } catch (Exception e) {
            log.error("Failed to fetch account IDs for user {}: {}", userId, e.getMessage());
            throw e;
        }
    }

    /**
     * Fallback method for circuit breaker.
     * Returns empty list to fail-safe (deny access) when account service is unavailable.
     */
    private List<UUID> getAccountIdsByUserIdFallback(String userId, Exception e) {
        log.warn("Circuit breaker fallback for getAccountIdsByUserId: {}. UserId: {}", e.getMessage(), userId);
        // Fail-safe: return empty list so authorization fails (deny by default)
        return Collections.emptyList();
    }
}
