package id.payu.transaction.adapter.client;

import id.payu.transaction.domain.port.out.WalletServicePort;
import id.payu.transaction.dto.ReserveBalanceRequest;
import id.payu.transaction.dto.ReserveBalanceResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Map;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

/**
 * REST adapter for calling wallet-service REST API.
 * Implements circuit breaker and retry for resilience.
 *
 * @deprecated Use {@link WalletGrpcAdapter} instead (IMP-028: gRPC migration).
 *             Kept as fallback during migration period.
 */
@Deprecated
@Component("walletRestAdapter")
public class WalletRestAdapter implements WalletServicePort {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(WalletRestAdapter.class);



    @Value("${services.wallet.url:http://localhost:8084}")
    private String walletServiceUrl;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public WalletRestAdapter(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

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

        HttpHeaders headers = authorizationHeaders();
        HttpEntity<ReserveBalanceRequest> entity = new HttpEntity<>(request, headers);

        try {
            ResponseEntity<Map> responseEntity = restTemplate.postForEntity(url, entity, Map.class);
            Map<String, Object> body = responseEntity.getBody();
            if (body != null && Boolean.TRUE.equals(body.get("success"))) {
                @SuppressWarnings("unchecked")
                Map<String, Object> data = (Map<String, Object>) body.get("data");
                if (data != null) {
                    ReserveBalanceResponse response = objectMapper.convertValue(data, ReserveBalanceResponse.class);
                    if (response.getReservationId() != null) {
                        log.info("Balance reserved successfully: reservationId={}", response.getReservationId());
                    }
                    return response;
                }
            }
            log.warn("Wallet reserve returned unexpected response: {}", body);
            throw new RuntimeException("Failed to parse wallet reserve response");
        } catch (Exception e) {
            log.error("Failed to reserve balance: {}", e.getMessage());
            throw e;
        }
    }

    @Override
    @CircuitBreaker(name = "walletService", fallbackMethod = "commitBalanceFallback")
    @Retry(name = "walletService")
    public void commitBalance(UUID accountId, String transactionId, String reservationId, BigDecimal amount) {
        if (reservationId == null) {
            log.warn("No reservation ID provided for transactionId={}, skipping commit", transactionId);
            return;
        }

        String url = walletServiceUrl + "/api/v1/wallets/reservations/" + reservationId + "/commit";
        log.info("Committing reservation: reservationId={}", reservationId);

        try {
            restTemplate.postForObject(url, new HttpEntity<>(null, authorizationHeaders()), Map.class);
            log.info("Reservation committed successfully: reservationId={}", reservationId);
        } catch (Exception e) {
            log.error("Failed to commit reservation: {}", e.getMessage());
            throw e;
        }
    }

    @Override
    @CircuitBreaker(name = "walletService", fallbackMethod = "releaseBalanceFallback")
    @Retry(name = "walletService")
    public void releaseBalance(UUID accountId, String transactionId, String reservationId, BigDecimal amount) {
        if (reservationId == null) {
            log.warn("No reservation ID provided for transactionId={}, skipping release", transactionId);
            return;
        }

        String url = walletServiceUrl + "/api/v1/wallets/reservations/" + reservationId + "/release";
        log.info("Releasing reservation: reservationId={}", reservationId);

        try {
            restTemplate.postForObject(url, new HttpEntity<>(null, authorizationHeaders()), Map.class);
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

    private void commitBalanceFallback(UUID accountId, String transactionId, String reservationId, BigDecimal amount, Exception e) {
        log.warn("Circuit breaker fallback for commitBalance: {}", e.getMessage());
        // In production, this should trigger a compensation/retry mechanism
    }

    private void releaseBalanceFallback(UUID accountId, String transactionId, String reservationId, BigDecimal amount, Exception e) {
        log.warn("Circuit breaker fallback for releaseBalance: {}", e.getMessage());
        // In production, this should trigger a compensation/retry mechanism
    }

    @Override
    @CircuitBreaker(name = "walletService", fallbackMethod = "creditBalanceFallback")
    @Retry(name = "walletService")
    public void creditBalance(String accountId, String transactionId, BigDecimal amount) {
        String url = walletServiceUrl + "/api/v1/wallets/" + accountId + "/credit";
        log.info("Crediting balance: accountId={}, transactionId={}, amount={}", accountId, transactionId, amount);

        Map<String, Object> request = Map.of(
                "amount", amount,
                "referenceId", transactionId,
                "description", "Internal transfer credit"
        );

        HttpHeaders headers = authorizationHeaders();
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);

        try {
            restTemplate.postForObject(url, entity, Map.class);
            log.info("Balance credited successfully: accountId={}", accountId);
        } catch (Exception e) {
            log.error("Failed to credit balance: {}", e.getMessage());
            throw e;
        }
    }

    private void creditBalanceFallback(String accountId, String transactionId, BigDecimal amount, Exception e) {
        log.warn("Circuit breaker fallback for creditBalance: {}", e.getMessage());
    }

    @Override
    public String transferBalance(String senderAccountId, String recipientAccountId,
                                  BigDecimal amount, String referenceId) {
        // ponytail: REST fallback stays 3-hop (reserve→commit→credit) because wallet-service
        // exposes the atomic /transfer endpoint only to trusted-service tokens; gRPC is the
        // prod path (payu.grpc.enabled=true). Replace when the REST adapter is removed.
        ReserveBalanceResponse reserved = reserveBalance(UUID.fromString(senderAccountId), referenceId, amount);
        if (!reserved.isSuccess() || reserved.getReservationId() == null) {
            throw new IllegalStateException("Insufficient balance for internal transfer");
        }
        try {
            commitBalance(UUID.fromString(senderAccountId), referenceId, reserved.getReservationId(), amount);
        } catch (Exception commitFailure) {
            try {
                releaseBalance(UUID.fromString(senderAccountId), referenceId, reserved.getReservationId(), amount);
            } catch (Exception releaseFailure) {
                commitFailure.addSuppressed(releaseFailure);
            }
            throw commitFailure;
        }
        creditBalance(recipientAccountId, referenceId, amount);
        return referenceId;
    }

    private HttpHeaders authorizationHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs != null && attrs.getRequest().getHeader("Authorization") != null) {
            headers.set("Authorization", attrs.getRequest().getHeader("Authorization"));
        }
        return headers;
    }
}
