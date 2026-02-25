package id.payu.statement.application.service;

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
     * Get current balance for a customer.
     * BUG-BE-051: Renamed from getBalanceAtDate to be explicit about returning current balance.
     */
    public BigDecimal getCurrentBalance(String customerId) {
        try {
            String url = walletServiceUrl + "/api/v1/wallets/customer/" + customerId + "/balance";
            WalletBalanceResponse response = restTemplate.getForObject(url, WalletBalanceResponse.class);
            return response != null ? response.getBalance() : BigDecimal.ZERO;
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch balance for customer " + customerId + ": " + e.getMessage(), e);
        }
    }

    @Data
    private static class WalletBalanceResponse {
        private BigDecimal balance;
    }
}
