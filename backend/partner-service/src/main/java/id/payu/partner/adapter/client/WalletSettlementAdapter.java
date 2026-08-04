package id.payu.partner.adapter.client;

import id.payu.partner.domain.port.out.WalletSettlementPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Wallet-service adapter for SNAP payment settlement.
 */
@Component
public class WalletSettlementAdapter implements WalletSettlementPort {

    private final RestClient walletClient;
    private final RestClient keycloakClient;
    private final String keycloakClientId;
    private final String keycloakClientSecret;
    private volatile ServiceToken serviceToken;

    public WalletSettlementAdapter(
        @Value("${payu.services.wallet-service.url:http://wallet-service:8080}") String walletServiceUrl,
        @Value("${PAYU_KEYCLOAK_SERVER_URL:http://payu-keycloak-service.payu-sso.svc.cluster.local:8080}")
        String keycloakServerUrl,
        @Value("${PAYU_KEYCLOAK_REALM:payu}") String keycloakRealm,
        @Value("${PAYU_KEYCLOAK_CLIENT_ID:payu-backend}") String keycloakClientId,
        @Value("${PAYU_KEYCLOAK_CLIENT_SECRET:}") String keycloakClientSecret) {
        this.walletClient = RestClient.builder().baseUrl(walletServiceUrl).build();
        this.keycloakClient = RestClient.builder()
                .baseUrl(keycloakServerUrl + "/realms/" + keycloakRealm)
                .build();
        this.keycloakClientId = keycloakClientId;
        this.keycloakClientSecret = keycloakClientSecret;
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

    @Override
    public void reverse(String senderAccountId, String recipientAccountId,
                        BigDecimal amount, String currency, UUID refundId, String description) {
        Map<String, Object> body = new HashMap<>();
        body.put("senderAccountId", senderAccountId);
        body.put("recipientAccountId", recipientAccountId);
        body.put("amount", amount);
        body.put("currency", currency);
        body.put("refundId", refundId);
        body.put("description", description);

        walletClient.post()
                .uri("/api/v1/wallets/transfer/reverse")
                .headers(target -> target.addAll(headers("snap-refund-" + refundId)))
                .body(body)
                .retrieve()
                .toBodilessEntity();
    }

    private String reserve(String accountId, BigDecimal amount, String referenceId) {
        HttpHeaders headers = headers("snap-reserve-" + referenceId);
        Map<String, Object> body = Map.of(
                "amount", amount,
                "referenceId", referenceId);

        Map<?, ?> response = walletClient.post()
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
        walletClient.post()
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
        walletClient.post()
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

        headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + platformToken());
        return headers;
    }

    private String platformToken() {
        ServiceToken cached = serviceToken;
        if (cached != null && cached.expiresAt().isAfter(Instant.now().plusSeconds(30))) {
            return cached.value();
        }

        synchronized (this) {
            cached = serviceToken;
            if (cached != null && cached.expiresAt().isAfter(Instant.now().plusSeconds(30))) {
                return cached.value();
            }

            MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
            form.add("grant_type", "client_credentials");
            form.add("client_id", keycloakClientId);
            form.add("client_secret", keycloakClientSecret);

            Map<?, ?> response = keycloakClient.post()
                    .uri("/protocol/openid-connect/token")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(Map.class);
            String value = response == null ? null : Objects.toString(response.get("access_token"), null);
            if (value == null || value.isBlank()) {
                throw new IllegalStateException("Keycloak service token response did not contain access_token");
            }

            long expiresIn = response.get("expires_in") instanceof Number number
                    ? number.longValue()
                    : Long.parseLong(Objects.toString(response.get("expires_in"), "300"));
            serviceToken = new ServiceToken(value, Instant.now().plusSeconds(Math.max(1, expiresIn - 30)));
            return value;
        }
    }

    private record ServiceToken(String value, Instant expiresAt) {
    }
}
