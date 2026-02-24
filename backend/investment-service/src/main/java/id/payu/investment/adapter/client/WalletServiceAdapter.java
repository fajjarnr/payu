package id.payu.investment.adapter.client;

import id.payu.investment.domain.port.out.WalletServicePort;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Adapter for calling wallet-service REST API from investment-service.
 *
 * BUG-BE-018 Fix: Replaced non-existent /deduct and /credit/{userId} endpoints with
 * the actual wallet-service API:
 *   - deductBalance → POST /{accountId}/reserve + POST /reservations/{id}/commit
 *   - creditBalance → POST /{accountId}/credit
 *   - hasSufficientBalance → GET /{accountId}/balance (reads 'availableBalance', not 'balance')
 *
 * BUG-BE-029 Fix: hasSufficientBalance now reads 'availableBalance' from response.
 */
@Component
@Slf4j
public class WalletServiceAdapter implements WalletServicePort {

    private final RestTemplate restTemplate;
    private final String walletServiceUrl;

    // Track reservation IDs for commit after successful operations
    private final Map<String, String> activeReservations = new ConcurrentHashMap<>();

    public WalletServiceAdapter(
            RestTemplate restTemplate,
            @Value("${services.wallet.url:http://localhost:8084}") String walletServiceUrl) {
        this.restTemplate = restTemplate;
        this.walletServiceUrl = walletServiceUrl;
    }

    /**
     * Deducts balance using wallet-service's reserve → commit flow.
     * Step 1: Reserve the amount (puts it on hold)
     * Step 2: Commit the reservation (finalizes the deduction)
     */
    @Override
    @CircuitBreaker(name = "walletService", fallbackMethod = "deductBalanceFallback")
    @Retry(name = "walletService")
    public void deductBalance(String userId, BigDecimal amount) {
        String referenceId = UUID.randomUUID().toString();

        // Step 1: Reserve balance
        String reserveUrl = walletServiceUrl + "/api/v1/wallets/" + userId + "/reserve";
        log.info("Reserving balance for investment: accountId={}, amount={}, ref={}", userId, amount, referenceId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        Map<String, Object> reserveRequest = Map.of(
                "amount", amount,
                "referenceId", referenceId
        );
        HttpEntity<Map<String, Object>> reserveEntity = new HttpEntity<>(reserveRequest, headers);

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> reserveResponse = restTemplate.postForObject(reserveUrl, reserveEntity, Map.class);

            if (reserveResponse == null || !reserveResponse.containsKey("reservationId")) {
                throw new RuntimeException("Invalid reserve response: no reservationId returned");
            }

            String reservationId = reserveResponse.get("reservationId").toString();
            log.info("Balance reserved: reservationId={}", reservationId);

            // Step 2: Commit the reservation
            String commitUrl = walletServiceUrl + "/api/v1/wallets/reservations/" + reservationId + "/commit";
            restTemplate.postForObject(commitUrl, null, Map.class);
            log.info("Reservation committed: reservationId={}, amount={}", reservationId, amount);

        } catch (Exception e) {
            log.error("Failed to deduct balance via reserve-commit: accountId={}, error={}", userId, e.getMessage());
            throw new RuntimeException("Failed to deduct wallet balance: " + e.getMessage(), e);
        }
    }

    /**
     * Credits balance using wallet-service's /credit endpoint.
     */
    @Override
    @CircuitBreaker(name = "walletService", fallbackMethod = "creditBalanceFallback")
    @Retry(name = "walletService")
    public void creditBalance(String userId, BigDecimal amount) {
        String creditUrl = walletServiceUrl + "/api/v1/wallets/" + userId + "/credit";
        String referenceId = UUID.randomUUID().toString();
        log.info("Crediting balance for investment redemption: accountId={}, amount={}", userId, amount);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Idempotency-Key", referenceId);
        Map<String, Object> creditRequest = Map.of(
                "amount", amount,
                "referenceId", referenceId,
                "description", "Investment redemption credit"
        );
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(creditRequest, headers);

        try {
            restTemplate.postForObject(creditUrl, entity, Map.class);
            log.info("Balance credited: accountId={}, amount={}", userId, amount);
        } catch (Exception e) {
            log.error("Failed to credit wallet: accountId={}, error={}", userId, e.getMessage());
            throw new RuntimeException("Failed to credit wallet balance: " + e.getMessage(), e);
        }
    }

    /**
     * Checks sufficient balance using wallet-service's /balance endpoint.
     * BUG-BE-029 Fix: reads 'availableBalance' (not 'balance') from the response.
     */
    @Override
    @CircuitBreaker(name = "walletService", fallbackMethod = "hasSufficientBalanceFallback")
    public boolean hasSufficientBalance(String userId, BigDecimal amount) {
        try {
            String balanceUrl = walletServiceUrl + "/api/v1/wallets/" + userId + "/balance";
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.getForObject(balanceUrl, Map.class);

            if (response != null) {
                // BUG-BE-029 fix: read 'availableBalance', not 'balance'
                // The wallet-service returns: { balance, availableBalance, reservedBalance, currency }
                Object availableBalanceObj = response.get("availableBalance");
                if (availableBalanceObj == null) {
                    // Fallback: try unwrapping ApiResponse wrapper (response.data.availableBalance)
                    Object data = response.get("data");
                    if (data instanceof Map) {
                        availableBalanceObj = ((Map<?, ?>) data).get("availableBalance");
                    }
                }

                if (availableBalanceObj != null) {
                    BigDecimal availableBalance = new BigDecimal(availableBalanceObj.toString());
                    return availableBalance.compareTo(amount) >= 0;
                }
            }
            log.warn("Could not determine balance for userId={}, defaulting to false", userId);
            return false;
        } catch (Exception e) {
            log.error("Failed to check wallet balance for user {}: {}", userId, e.getMessage());
            return false;
        }
    }

    // Fallback methods for circuit breaker
    private void deductBalanceFallback(String userId, BigDecimal amount, Exception e) {
        log.error("Circuit breaker fallback for deductBalance: userId={}, error={}", userId, e.getMessage());
        throw new RuntimeException("Wallet service unavailable for deduction", e);
    }

    private void creditBalanceFallback(String userId, BigDecimal amount, Exception e) {
        log.error("Circuit breaker fallback for creditBalance: userId={}, error={}", userId, e.getMessage());
        throw new RuntimeException("Wallet service unavailable for credit", e);
    }

    private boolean hasSufficientBalanceFallback(String userId, BigDecimal amount, Exception e) {
        log.error("Circuit breaker fallback for hasSufficientBalance: userId={}, error={}", userId, e.getMessage());
        return false;
    }
}
