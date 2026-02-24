package id.payu.promotion.adapter.client;

import id.payu.promotion.domain.port.out.WalletServicePort;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.Map;

/**
 * REST client adapter for wallet-service integration.
 * Implements circuit breaker and retry patterns for resilience.
 */
@Component
public class WalletClient implements WalletServicePort {

    private static final Logger LOG = LoggerFactory.getLogger(WalletClient.class);

    private final RestTemplate restTemplate;
    private final String walletServiceUrl;

    public WalletClient(
            RestTemplate restTemplate,
            @Value("${services.wallet.url:http://localhost:8084}") String walletServiceUrl) {
        this.restTemplate = restTemplate;
        this.walletServiceUrl = walletServiceUrl;
    }

    @Override
    @CircuitBreaker(name = "walletService", fallbackMethod = "creditWalletFallback")
    @Retry(name = "walletService")
    public boolean creditWallet(String accountId, BigDecimal amount, String referenceId, String description) {
        String url = walletServiceUrl + "/api/v1/wallets/" + accountId + "/credit";
        LOG.info("Crediting wallet: accountId={}, amount={}, referenceId={}", accountId, amount, referenceId);

        CreditRequest request = new CreditRequest(amount, referenceId, description);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Idempotency-Key", referenceId);
        HttpEntity<CreditRequest> entity = new HttpEntity<>(request, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);

            boolean success = response.getStatusCode() == HttpStatus.OK;
            if (success) {
                LOG.info("Wallet credited successfully: accountId={}, amount={}", accountId, amount);
            } else {
                LOG.warn("Wallet credit returned non-OK status: {}", response.getStatusCode());
            }
            return success;
        } catch (RestClientException e) {
            LOG.error("Failed to credit wallet: accountId={}, error={}", accountId, e.getMessage());
            throw new WalletCreditException("Failed to credit wallet: " + e.getMessage(), e);
        }
    }

    /**
     * Fallback method for circuit breaker.
     */
    private boolean creditWalletFallback(String accountId, BigDecimal amount, String referenceId,
                                          String description, Exception ex) {
        LOG.error("Circuit breaker fallback for creditWallet: accountId={}, error={}", accountId, ex.getMessage());
        throw new WalletCreditException("Wallet service unavailable, cannot credit cashback", ex);
    }

    /**
     * DTO for credit request.
     */
    public record CreditRequest(BigDecimal amount, String referenceId, String description) {}
}
