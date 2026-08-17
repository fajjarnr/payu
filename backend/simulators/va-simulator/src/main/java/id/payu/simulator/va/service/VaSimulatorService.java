package id.payu.simulator.va.service;

import id.payu.simulator.va.interfaces.dto.VaInquiryRequest;
import id.payu.simulator.va.interfaces.dto.VaInquiryResponse;
import id.payu.simulator.va.interfaces.dto.VaPaymentRequest;
import id.payu.simulator.va.interfaces.dto.VaPaymentResponse;
import id.payu.simulator.va.entity.VirtualAccount;
import io.quarkus.logging.Log;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.UUID;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import id.payu.simulator.va.entity.VaStatus;

/**
 * Service for Virtual Account simulation.
 * Provides deterministic behavior for VA payment testing.
 *
 * <p>Features:
 * <ul>
 *   <li>VA inquiry validation</li>
 *   <li>Payment processing with callback to PayU</li>
 *   <li>Deterministic responses for consistent testing</li>
 *   <li>Auto-expiry of pending VAs</li>
 * </ul>
 */
@ApplicationScoped
public class VaSimulatorService {

    @ConfigProperty(name = "va.simulator.callback.url", defaultValue = "http://localhost:8083/api/v1/payments/va/callback")
    String callbackUrl;

    @ConfigProperty(name = "va.simulator.deterministic", defaultValue = "true")
    boolean deterministic;

    @ConfigProperty(name = "va.simulator.callback.timeout", defaultValue = "10")
    int callbackTimeoutSeconds;

    @ConfigProperty(name = "payu.callback.signature.secret")
    Optional<String> callbackSignatureSecret;

    private final HttpClient httpClient;

    public VaSimulatorService() {
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    }

    /**
     * Inquire about a Virtual Account.
     * Returns VA details if valid and pending.
     */
    @Transactional
    public VaInquiryResponse inquiry(VaInquiryRequest request) {
        Log.infof("VA Inquiry: vaNumber=%s, bank=%s", request.vaNumber(), request.bankCode());

        VirtualAccount va = VirtualAccount.findByVaNumber(request.vaNumber());

        if (va == null) {
            Log.warnf("VA not found: %s", request.vaNumber());
            return VaInquiryResponse.notFound(request.vaNumber());
        }

        // Check if already paid
        if (va.status == VaStatus.PAID) {
            Log.warnf("VA already paid: %s", request.vaNumber());
            return VaInquiryResponse.alreadyPaid(request.vaNumber());
        }

        // Check expiry
        if (va.expiresAt.isBefore(Instant.now())) {
            Log.warnf("VA expired: %s", request.vaNumber());
            va.markExpired();
            va.persist();
            return VaInquiryResponse.expired(request.vaNumber());
        }

        // Success
        return VaInquiryResponse.success(
            va.vaNumber,
            va.bankCode,
            va.customerName,
            va.amount,
            va.currency,
            va.description,
            DateTimeFormatter.ISO_INSTANT.format(va.expiresAt)
        );
    }

    /**
     * Process payment to a Virtual Account.
     * Validates, records payment, and sends callback to PayU.
     */
    @Transactional
    public VaPaymentResponse processPayment(VaPaymentRequest request) {
        Log.infof("VA Payment: vaNumber=%s, amount=%s %s",
            request.vaNumber(), request.amount(), request.currency());

        VirtualAccount va = VirtualAccount.findByVaNumber(request.vaNumber());

        if (va == null) {
            Log.warnf("VA not found for payment: %s", request.vaNumber());
            return VaPaymentResponse.notFound(request.vaNumber());
        }

        // Check if already paid
        if (va.status == VaStatus.PAID) {
            Log.warnf("VA already paid: %s", request.vaNumber());
            return VaPaymentResponse.alreadyPaid(request.vaNumber());
        }

        // Check expiry
        if (va.expiresAt.isBefore(Instant.now())) {
            Log.warnf("VA expired: %s", request.vaNumber());
            va.markExpired();
            va.persist();
            return VaPaymentResponse.expired(request.vaNumber());
        }

        // Validate amount (exact match required)
        if (!deterministic && request.amount().compareTo(va.amount) != 0) {
            // In non-deterministic mode, allow exact match only
            Log.warnf("VA amount mismatch: expected=%s, received=%s", va.amount, request.amount());
            return VaPaymentResponse.amountMismatch(request.vaNumber(), va.amount, request.amount());
        }

        // Generate payment reference
        String paymentRef = generatePaymentReference(request.bankCode());

        // Mark as paid
        va.markPaid(request.amount(), paymentRef);
        va.persist();

        // Send callback to PayU
        String callbackStatus = sendCallback(va, request, paymentRef);

        Log.infof("VA payment successful: vaNumber=%s, ref=%s, callback=%s",
            va.vaNumber, paymentRef, callbackStatus);

        return VaPaymentResponse.success(
            va.vaNumber,
            paymentRef,
            request.amount(),
            va.currency,
            callbackStatus
        );
    }

    /**
     * Register a new Virtual Account (called by PayU to simulate bank VA creation).
     */
    @Transactional
    public VirtualAccount registerVa(String vaNumber, String bankCode, String bankName,
                                      String partnerId, BigDecimal amount,
                                      String currency, Instant expiresAt,
                                      String callbackUrl, String externalId) {
        Log.infof("Registering VA: number=%s, bank=%s, partner=%s", vaNumber, bankCode, partnerId);

        VirtualAccount va = new VirtualAccount(
            vaNumber, bankCode, bankName, partnerId, amount, currency, expiresAt
        );
        va.callbackUrl = callbackUrl;
        va.externalId = externalId;
        va.persist();

        return va;
    }

    /**
     * Scheduled job to expire pending VAs.
     * Runs every 5 minutes.
     */
    @Scheduled(every = "5m")
    @Transactional
    public void expirePendingVas() {
        int expired = VirtualAccount.update(
            "status = ?1 WHERE status = ?2 AND expiresAt < ?3",
            VaStatus.EXPIRED,
            VaStatus.PENDING,
            Instant.now()
        );

        if (expired > 0) {
            Log.infof("Expired %d pending Virtual Accounts", expired);
        }
    }

    /**
     * Send callback to PayU to notify about VA payment.
     */
    private String sendCallback(VirtualAccount va, VaPaymentRequest request, String paymentRef) {
        try {
            String callbackEndpoint = va.callbackUrl != null ? va.callbackUrl : callbackUrl;

            // Build callback payload
            String jsonPayload = String.format(
                "{\"vaNumber\":\"%s\",\"amount\":%s,\"currency\":\"%s\",\"paymentReference\":\"%s\",\"paidAt\":\"%s\",\"customerAccountNumber\":\"%s\",\"customerAccountName\":\"%s\"}",
                va.vaNumber,
                request.amount().toPlainString(),
                va.currency,
                paymentRef,
                Instant.now().toString(),
                request.customerAccountNumber(),
                request.customerAccountName() != null ? request.customerAccountName() : ""
            );

            String timestamp = String.valueOf(Instant.now().getEpochSecond());
            String signature = calculateSignature(timestamp, jsonPayload);
            String idempotencyKey = UUID.nameUUIDFromBytes(
                paymentRef.getBytes(StandardCharsets.UTF_8)).toString();

            HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(callbackEndpoint))
                .header("Content-Type", "application/json")
                .header("X-Timestamp", timestamp)
                .header("X-Signature", signature)
                .header("X-Idempotency-Key", idempotencyKey)
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                .timeout(Duration.ofSeconds(callbackTimeoutSeconds))
                .build();

            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                Log.infof("Callback successful to %s: HTTP %d", callbackEndpoint, response.statusCode());
                return "SUCCESS";
            } else {
                Log.warnf("Callback failed to %s: HTTP %d, body=%s",
                    callbackEndpoint, response.statusCode(), response.body());
                return "FAILED";
            }

        } catch (Exception e) {
            Log.errorf(e, "Callback error for VA %s", va.vaNumber);
            return "ERROR: " + e.getMessage();
        }
    }

    private String calculateSignature(String timestamp, String body) {
        if (callbackSignatureSecret.isEmpty() || callbackSignatureSecret.get().isBlank()) {
            throw new IllegalStateException("PAYU_CALLBACK_SIGNATURE_SECRET is not configured");
        }
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(callbackSignatureSecret.get().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(
                (timestamp + "\n" + body).getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.GeneralSecurityException e) {
            throw new IllegalStateException("Unable to sign VA callback", e);
        }
    }

    private String generatePaymentReference(String bankCode) {
        return String.format("VA%s-%s-%s",
            bankCode.toUpperCase(),
            java.time.LocalDate.now().toString().replace("-", ""),
            UUID.randomUUID().toString().substring(0, 8).toUpperCase()
        );
    }
}
