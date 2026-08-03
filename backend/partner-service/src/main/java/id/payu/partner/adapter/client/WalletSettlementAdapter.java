package id.payu.partner.adapter.client;

import id.payu.partner.domain.port.out.WalletSettlementPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Wallet-service adapter for SNAP payment settlement.
 */
@Component
public class WalletSettlementAdapter implements WalletSettlementPort {

    private final RestClient restClient;

    public WalletSettlementAdapter(
        @Value("${payu.services.wallet-service.url:http://wallet-service:8080}") String walletServiceUrl) {
        this.restClient = RestClient.builder().baseUrl(walletServiceUrl).build();
    }

    @Override
    public void settle(String sourceAccountId, String beneficiaryAccountId,
                       BigDecimal amount, String currency, String referenceId) {
        String reservationId = reserve(sourceAccountId, amount, referenceId);
        commit(reservationId, referenceId);

        try {
            credit(beneficiaryAccountId, amount, referenceId,
                    "SNAP-BI payment settlement: " + referenceId);
        } catch (RuntimeException creditFailure) {
            try {
                credit(sourceAccountId, amount, referenceId + "-reversal",
                        "SNAP-BI payment compensation: " + referenceId);
            } catch (RuntimeException compensationFailure) {
                creditFailure.addSuppressed(compensationFailure);
            }
            throw creditFailure;
        }
    }

    private String reserve(String accountId, BigDecimal amount, String referenceId) {
        HttpHeaders headers = headers("snap-reserve-" + referenceId);
        Map<String, Object> body = Map.of(
                "amount", amount,
                "referenceId", referenceId);

        Map<?, ?> response = restClient.post()
                .uri("/api/v1/wallets/" + accountId + "/reserve")
                .headers(target -> target.addAll(headers))
                .body(body)
                .retrieve()
                .body(Map.class);
        Map<?, ?> data = response == null ? null : (Map<?, ?>) response.get("data");
        String reservationId = data == null ? null : String.valueOf(data.get("reservationId"));
        if (reservationId == null || "null".equals(reservationId)) {
            throw new IllegalStateException("Wallet reserve returned no reservation ID");
        }
        return reservationId;
    }

    private void commit(String reservationId, String referenceId) {
        restClient.post()
                .uri("/api/v1/wallets/reservations/" + reservationId + "/commit")
                .headers(target -> target.addAll(headers("snap-commit-" + referenceId)))
                .retrieve()
                .toBodilessEntity();
    }

    private void credit(String accountId, BigDecimal amount, String referenceId, String description) {
        Map<String, Object> body = Map.of(
                "amount", amount,
                "referenceId", referenceId,
                "description", description);
        restClient.post()
                .uri("/api/v1/wallets/" + accountId + "/credit")
                .headers(target -> target.addAll(headers("snap-credit-" + referenceId)))
                .body(body)
                .retrieve()
                .toBodilessEntity();
    }

    private HttpHeaders headers(String idempotencyKey) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Idempotency-Key", idempotencyKey);

        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            String authorization = attributes.getRequest().getHeader(HttpHeaders.AUTHORIZATION);
            if (authorization != null) {
                headers.set(HttpHeaders.AUTHORIZATION, authorization);
            }
        }
        return headers;
    }
}
