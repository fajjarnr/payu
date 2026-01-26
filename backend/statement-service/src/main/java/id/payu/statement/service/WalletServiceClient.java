package id.payu.statement.service;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Client for Wallet Service - Feign could be used alternatively
 */
@Component
public class WalletServiceClient {

    private final RestTemplate restTemplate;

    @Value("${services.wallet.url:http://wallet-service:8004}")
    private String walletServiceUrl;

    public WalletServiceClient() {
        this.restTemplate = new RestTemplate();
    }

    /**
     * Get balance at a specific date (historical balance query)
     * For now, returns current balance - can be enhanced with balance history
     */
    public BigDecimal getBalanceAtDate(UUID userId, LocalDate date) {
        try {
            String url = walletServiceUrl + "/api/v1/wallets/user/" + userId + "/balance";
            WalletBalanceResponse response = restTemplate.getForObject(url, WalletBalanceResponse.class);
            return response != null ? response.getBalance() : BigDecimal.ZERO;
        } catch (Exception e) {
            // Return default balance on error
            return BigDecimal.ZERO;
        }
    }

    @Data
    private static class WalletBalanceResponse {
        private BigDecimal balance;
    }
}
