package id.payu.statement.application.service;

import id.payu.statement.domain.port.out.WalletServicePort;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * REST client for Wallet Service.
 *
 * @deprecated IMP-028: Use {@link id.payu.statement.adapter.client.WalletGrpcAdapter} instead.
 *             This REST client is retained for fallback purposes during the gRPC migration period.
 */
@Deprecated(since = "IMP-028", forRemoval = false)
@Component("walletRestAdapter")
public class WalletServiceClient implements WalletServicePort {

    private final RestTemplate restTemplate;

    @Value("${services.wallet.url:http://wallet-service:8004}")
    private String walletServiceUrl;

    public WalletServiceClient() {
        // BUG-ARCH-006 FIX: Configure RestTemplate with timeouts instead of bare new RestTemplate()
        org.springframework.http.client.SimpleClientHttpRequestFactory factory = new org.springframework.http.client.SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(10000);
        this.restTemplate = new RestTemplate(factory);
    }

    /**
     * Get current balance for a customer.
     * BUG-BE-051: Renamed from getBalanceAtDate to be explicit about returning current balance.
     */
    @Override
    public BigDecimal getCurrentBalance(String customerId) {
        try {
            String url = walletServiceUrl + "/api/v1/wallets/customer/" + customerId + "/balance";
            WalletBalanceResponse response = restTemplate.getForObject(url, WalletBalanceResponse.class);
            return response != null ? response.getBalance() : BigDecimal.ZERO;
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch balance for customer " + customerId + ": " + e.getMessage(), e);
        }
    }

    @Override
    public java.util.Optional<BigDecimal> getBalanceAsOf(String customerId, LocalDate endDate) {
        // IMP-3: ledger snapshot is served over gRPC (WalletGrpcAdapter, @Primary).
        // This deprecated REST fallback has no balance-as-of endpoint — report
        // empty so the service falls back to its derivation path.
        log.warn("getBalanceAsOf not supported by REST wallet client (gRPC is primary); customerId={}", customerId);
        return java.util.Optional.empty();
    }

    private static final org.slf4j.Logger log =
            org.slf4j.LoggerFactory.getLogger(WalletServiceClient.class);

    @Data
    private static class WalletBalanceResponse {
        private BigDecimal balance;
    }
}
