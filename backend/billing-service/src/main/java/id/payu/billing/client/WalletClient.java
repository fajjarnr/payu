package id.payu.billing.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;

/**
 * REST client for wallet-service using Spring RestTemplate.
 */
@Component
public class WalletClient {

    private final RestTemplate restTemplate;
    private final String walletServiceBaseUrl;

    public WalletClient(
            RestTemplate restTemplate,
            @Value("${spring.web.client.wallet-service.base-url}") String walletServiceBaseUrl) {
        this.restTemplate = restTemplate;
        this.walletServiceBaseUrl = walletServiceBaseUrl;
    }

    /**
     * Reserve balance from wallet.
     */
    public ReserveResponse reserveBalance(String accountId, ReserveRequest request) {
        String url = walletServiceBaseUrl + "/api/v1/wallets/" + accountId + "/reserve";
        return restTemplate.postForObject(url, request, ReserveResponse.class);
    }

    /**
     * Commit a reservation.
     */
    public void commitReservation(String reservationId) {
        String url = walletServiceBaseUrl + "/api/v1/wallets/reservations/" + reservationId + "/commit";
        restTemplate.postForObject(url, null, Void.class);
    }

    /**
     * Release a reservation.
     */
    public void releaseReservation(String reservationId) {
        String url = walletServiceBaseUrl + "/api/v1/wallets/reservations/" + reservationId + "/release";
        restTemplate.postForObject(url, null, Void.class);
    }

    /**
     * Request DTO for reserving balance.
     */
    public record ReserveRequest(BigDecimal amount, String referenceId) {}

    /**
     * Response DTO for reserve operation.
     */
    public record ReserveResponse(String reservationId, String accountId, String referenceId, String status) {}
}
