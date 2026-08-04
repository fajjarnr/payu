package id.payu.simulator.bifast.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import id.payu.simulator.bifast.config.SimulatorConfig;
import id.payu.simulator.bifast.entity.Transfer;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@ApplicationScoped
public class WebhookDispatcher {

    private static final String SIGNATURE_HEADER = "X-Signature";
    private static final String TIMESTAMP_HEADER = "X-Timestamp";

    @Inject
    ObjectMapper objectMapper;

    @Inject
    SimulatorConfig config;

    private final HttpClient httpClient = HttpClient.newHttpClient();

    public void dispatch(Transfer transfer) {
        if (!config.webhook().enabled()
                || transfer.webhookUrl == null
                || transfer.webhookUrl.isBlank()
                || config.webhook().secret() == null
                || config.webhook().secret().isBlank()) {
            return;
        }

        CompletableFuture.delayedExecutor(
                config.webhook().delayMs(), TimeUnit.MILLISECONDS)
                .execute(() -> sendWithRetry(transfer));
    }

    static String signature(String timestamp, String body, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] digest = mac.doFinal((timestamp + "\n" + body).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Unable to calculate webhook signature", e);
        }
    }

    private void sendWithRetry(Transfer transfer) {
        final String body;
        try {
            body = objectMapper.writeValueAsString(new CallbackPayload(
                    transfer.referenceNumber,
                    transfer.status.name(),
                    transfer.status == id.payu.simulator.bifast.entity.TransferStatus.COMPLETED
                            ? transfer.referenceNumber : null,
                    transfer.failureReason));
        } catch (JsonProcessingException e) {
            Log.errorf(e, "Unable to serialize callback for transfer %s", transfer.referenceNumber);
            return;
        }

        for (int attempt = 0; attempt <= config.webhook().retryCount(); attempt++) {
            String timestamp = Long.toString(Instant.now().getEpochSecond());
            try {
                HttpRequest request = HttpRequest.newBuilder(URI.create(transfer.webhookUrl))
                        .header("Content-Type", "application/json")
                        .header(TIMESTAMP_HEADER, timestamp)
                        .header(SIGNATURE_HEADER, signature(timestamp, body, config.webhook().secret()))
                        .POST(HttpRequest.BodyPublishers.ofString(body))
                        .build();
                HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
                if (response.statusCode() >= 200 && response.statusCode() < 300) {
                    Log.infof("Webhook delivered for transfer %s", transfer.referenceNumber);
                    return;
                }
                Log.warnf("Webhook returned HTTP %d for transfer %s", response.statusCode(), transfer.referenceNumber);
            } catch (Exception e) {
                Log.warnf(e, "Webhook attempt %d failed for transfer %s", attempt + 1, transfer.referenceNumber);
            }

            if (attempt < config.webhook().retryCount()) {
                try {
                    Thread.sleep(config.webhook().retryDelayMs());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
    }

    private record CallbackPayload(
            String disbursementId,
            String status,
            String bankReference,
            String failureReason) {
    }
}
