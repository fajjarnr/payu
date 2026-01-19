package id.payu.transaction.adapter.client;

import id.payu.transaction.domain.port.out.WalletServicePort;
import id.payu.transaction.dto.ReserveBalanceRequest;
import id.payu.transaction.dto.ReserveBalanceResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
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
 * Adapter for calling wallet-service REST API.
 * Implements circuit breaker and retry for resilience.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WalletServiceAdapter implements WalletServicePort {

    @Value("${services.wallet.url:http://localhost:8084}")
    private String walletServiceUrl;

    private final RestTemplate restTemplate;

    // Store reservationId mapping for commit/release
    private final Map<String, String> reservationMapping = new ConcurrentHashMap<>();

    @Override
    @CircuitBreaker(name = "walletService", fallbackMethod = "reserveBalanceFallback")
    @Retry(name = "walletService")
    public ReserveBalanceResponse reserveBalance(UUID accountId, String transactionId, BigDecimal amount) {
        String url = walletServiceUrl + "/api/v1/wallets/" + accountId.toString() + "/reserve";
        log.info("Reserving balance: accountId={}, transactionId={}, amount={}", accountId, transactionId, amount);

        ReserveBalanceRequest request = ReserveBalanceRequest.builder()
                .amount(amount)
                .referenceId(transactionId)
                .build();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<ReserveBalanceRequest> entity = new HttpEntity<>(request, headers);

        try {
            ReserveBalanceResponse response = restTemplate.postForObject(url, entity, ReserveBalanceResponse.class);
            
            if (response != null && response.getReservationId() != null) {
                // Store reservation mapping for later commit/release
                reservationMapping.put(transactionId, response.getReservationId());
                log.info("Balance reserved successfully: reservationId={}", response.getReservationId());
            }
            
            return response;
        } catch (Exception e) {
            log.error("Failed to reserve balance: {}", e.getMessage());
            throw e;
        }
    }

    @Override
    @CircuitBreaker(name = "walletService", fallbackMethod = "commitBalanceFallback")
    @Retry(name = "walletService")
    public void commitBalance(UUID accountId, String transactionId, BigDecimal amount) {
        String reservationId = reservationMapping.get(transactionId);
        if (reservationId == null) {
            log.warn("No reservation found for transactionId={}, skipping commit", transactionId);
            return;
        }

        String url = walletServiceUrl + "/api/v1/wallets/reservations/" + reservationId + "/commit";
        log.info("Committing reservation: reservationId={}", reservationId);

        try {
            restTemplate.postForObject(url, null, Map.class);
            reservationMapping.remove(transactionId);
            log.info("Reservation committed successfully: reservationId={}", reservationId);
        } catch (Exception e) {
            log.error("Failed to commit reservation: {}", e.getMessage());
            throw e;
        }
    }

    @Override
    @CircuitBreaker(name = "walletService", fallbackMethod = "releaseBalanceFallback")
    @Retry(name = "walletService")
    public void releaseBalance(UUID accountId, String transactionId, BigDecimal amount) {
        String reservationId = reservationMapping.get(transactionId);
        if (reservationId == null) {
            log.warn("No reservation found for transactionId={}, skipping release", transactionId);
            return;
        }

        String url = walletServiceUrl + "/api/v1/wallets/reservations/" + reservationId + "/release";
        log.info("Releasing reservation: reservationId={}", reservationId);

        try {
            restTemplate.postForObject(url, null, Map.class);
            reservationMapping.remove(transactionId);
            log.info("Reservation released successfully: reservationId={}", reservationId);
        } catch (Exception e) {
            log.error("Failed to release reservation: {}", e.getMessage());
            throw e;
        }
    }

    // Fallback methods for circuit breaker
    private ReserveBalanceResponse reserveBalanceFallback(UUID accountId, String transactionId, BigDecimal amount, Exception e) {
        log.warn("Circuit breaker fallback for reserveBalance: {}", e.getMessage());
        return ReserveBalanceResponse.builder()
                .status("FAILED")
                .referenceId(transactionId)
                .build();
    }

    private void commitBalanceFallback(UUID accountId, String transactionId, BigDecimal amount, Exception e) {
        log.warn("Circuit breaker fallback for commitBalance: {}", e.getMessage());
        // In production, this should trigger a compensation/retry mechanism
    }

    private void releaseBalanceFallback(UUID accountId, String transactionId, BigDecimal amount, Exception e) {
        log.warn("Circuit breaker fallback for releaseBalance: {}", e.getMessage());
        // In production, this should trigger a compensation/retry mechanism
    }
}
