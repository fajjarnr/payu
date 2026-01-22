package id.payu.investment.adapter.client;

import id.payu.investment.domain.port.out.WalletServicePort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.Map;

@Component
@Slf4j
public class WalletServiceAdapter implements WalletServicePort {

    private final RestTemplate restTemplate;
    private final String walletServiceUrl;

    public WalletServiceAdapter(
            RestTemplate restTemplate,
            @Value("${services.wallet.url:http://localhost:8084}") String walletServiceUrl) {
        this.restTemplate = restTemplate;
        this.walletServiceUrl = walletServiceUrl;
    }

    @Override
    public void deductBalance(String userId, BigDecimal amount) {
        try {
            restTemplate.postForObject(
                    walletServiceUrl + "/api/v1/wallets/{userId}/deduct",
                    Map.of("amount", amount),
                    Void.class,
                    userId);
            log.info("Deducted {} from wallet for user {}", amount, userId);
        } catch (Exception e) {
            log.error("Failed to deduct balance from wallet for user {}", userId, e);
            throw new RuntimeException("Failed to deduct wallet balance", e);
        }
    }

    @Override
    public void creditBalance(String userId, BigDecimal amount) {
        try {
            restTemplate.postForObject(
                    walletServiceUrl + "/api/v1/wallets/{userId}/credit",
                    Map.of("amount", amount),
                    Void.class,
                    userId);
            log.info("Credited {} to wallet for user {}", amount, userId);
        } catch (Exception e) {
            log.error("Failed to credit wallet for user {}", userId, e);
            throw new RuntimeException("Failed to credit wallet balance", e);
        }
    }

    @Override
    public boolean hasSufficientBalance(String userId, BigDecimal amount) {
        try {
            Map<?, ?> response = restTemplate.getForObject(
                    walletServiceUrl + "/api/v1/wallets/{userId}/balance",
                    Map.class,
                    userId);
            if (response != null && response.containsKey("balance")) {
                BigDecimal balance = new BigDecimal(response.get("balance").toString());
                return balance.compareTo(amount) >= 0;
            }
            return false;
        } catch (Exception e) {
            log.error("Failed to check wallet balance for user {}", userId, e);
            return false;
        }
    }
}
