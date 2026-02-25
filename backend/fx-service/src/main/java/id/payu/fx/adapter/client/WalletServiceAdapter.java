package id.payu.fx.adapter.client;

import id.payu.fx.domain.port.out.WalletServicePort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.Map;

/**
 * REST adapter for wallet-service integration used during FX conversions.
 * Calls wallet-service endpoints to debit source currency and credit target currency.
 */
@Slf4j
@Component
public class WalletServiceAdapter implements WalletServicePort {

    private final RestTemplate restTemplate;
    private final String walletServiceUrl;

    public WalletServiceAdapter(
            RestTemplate restTemplate,
            @Value("${payu.services.wallet-service.url:http://wallet-service:8080}") String walletServiceUrl) {
        this.restTemplate = restTemplate;
        this.walletServiceUrl = walletServiceUrl;
    }

    @Override
    public boolean debit(String accountId, String transactionId, BigDecimal amount, String currency) {
        try {
            log.info("Debiting wallet: accountId={}, amount={} {}, txId={}", accountId, amount, currency, transactionId);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-Idempotency-Key", "fx-debit-" + transactionId);

            Map<String, Object> body = Map.of(
                    "accountId", accountId,
                    "transactionId", transactionId,
                    "amount", amount,
                    "currency", currency,
                    "type", "FX_DEBIT"
            );

            restTemplate.postForEntity(
                    walletServiceUrl + "/api/v1/wallets/debit",
                    new HttpEntity<>(body, headers),
                    Void.class
            );
            return true;
        } catch (Exception e) {
            log.error("Failed to debit wallet: accountId={}, amount={} {}: {}",
                    accountId, amount, currency, e.getMessage(), e);
            return false;
        }
    }

    @Override
    public boolean credit(String accountId, String transactionId, BigDecimal amount, String currency) {
        try {
            log.info("Crediting wallet: accountId={}, amount={} {}, txId={}", accountId, amount, currency, transactionId);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-Idempotency-Key", "fx-credit-" + transactionId);

            Map<String, Object> body = Map.of(
                    "accountId", accountId,
                    "transactionId", transactionId,
                    "amount", amount,
                    "currency", currency,
                    "type", "FX_CREDIT"
            );

            restTemplate.postForEntity(
                    walletServiceUrl + "/api/v1/wallets/credit",
                    new HttpEntity<>(body, headers),
                    Void.class
            );
            return true;
        } catch (Exception e) {
            log.error("Failed to credit wallet: accountId={}, amount={} {}: {}",
                    accountId, amount, currency, e.getMessage(), e);
            return false;
        }
    }

    @Override
    public void reverseDebit(String accountId, String transactionId, BigDecimal amount, String currency) {
        try {
            log.info("Reversing wallet debit: accountId={}, amount={} {}, txId={}", accountId, amount, currency, transactionId);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-Idempotency-Key", "fx-reverse-" + transactionId);

            Map<String, Object> body = Map.of(
                    "accountId", accountId,
                    "transactionId", transactionId,
                    "amount", amount,
                    "currency", currency,
                    "type", "FX_REVERSAL"
            );

            restTemplate.postForEntity(
                    walletServiceUrl + "/api/v1/wallets/credit",
                    new HttpEntity<>(body, headers),
                    Void.class
            );
        } catch (Exception e) {
            log.error("CRITICAL: Failed to reverse wallet debit: accountId={}, amount={} {}: {}",
                    accountId, amount, currency, e.getMessage(), e);
            // At this point, manual intervention may be needed. Log critical error.
        }
    }
}
