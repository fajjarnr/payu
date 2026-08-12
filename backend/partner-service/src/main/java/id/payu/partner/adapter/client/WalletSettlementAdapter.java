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
import java.util.List;
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
        // IMP-1: single atomic wallet transfer (debit+credit in one transaction, idempotent by
        // referenceId). The old reserve→commit→credit sequence had a crash window between
        // commit and credit that required a source-credit compensation; no longer needed.
        Map<String, Object> body = Map.of(
                "senderAccountId", sourceAccountId,
                "recipientAccountId", beneficiaryAccountId,
                "amount", amount,
                "currency", currency,
                "referenceId", referenceId,
                "description", "SNAP-BI payment settlement: " + referenceId);

        walletClient.post()
                .uri("/api/v1/wallets/transfer")
                .headers(target -> target.addAll(headers("snap-transfer-" + referenceId)))
                .body(body)
                .retrieve()
                .toBodilessEntity();
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

    private HttpHeaders headers(String idempotencyKey) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Idempotency-Key", idempotencyKey);

        headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + platformToken());
        return headers;
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<LedgerMovement> ledgerMovementsByReferences(List<String> referenceIds) {
        if (referenceIds == null || referenceIds.isEmpty()) {
            return List.of();
        }
        Map<String, Object> body = Map.of("referenceIds", referenceIds);
        Map<?, ?> response = walletClient.post()
                .uri("/api/v1/reconciliation/ledger-movements")
                .headers(target -> target.addAll(headers("snap-reconcile-" + UUID.randomUUID())))
                .body(body)
                .retrieve()
                .body(Map.class);
        Object data = response == null ? null : response.get("data");
        if (!(data instanceof List<?> rows)) {
            return List.of();
        }
        List<LedgerMovement> movements = new java.util.ArrayList<>();
        for (Object row : rows) {
            if (!(row instanceof Map<?, ?> m)) {
                continue;
            }
            movements.add(new LedgerMovement(
                    str(m.get("accountId")),
                    str(m.get("referenceId")),
                    str(m.get("referenceType")),
                    str(m.get("entryType")),
                    num(m.get("amount")),
                    num(m.get("balanceAfter"))));
        }
        return movements;
    }

    private static String str(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private static BigDecimal num(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return new BigDecimal(String.valueOf(value));
        } catch (NumberFormatException e) {
            return null;
        }
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
