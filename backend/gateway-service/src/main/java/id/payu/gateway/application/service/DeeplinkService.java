package id.payu.gateway.application.service;

import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Map;

/**
 * Generates signed deep links for the PayU mobile app.
 * URL scheme: payu://pay, payu://topup, payu://transfer
 *
 * Part of E-15 IMP-046: Checkout Deeplink
 */
@ApplicationScoped
public class DeeplinkService {

    @ConfigProperty(name = "payu.deeplink.secret", defaultValue = "payu-deeplink-secret-key-change-me")
    String deeplinkSecret;

    private static final String SCHEME = "payu://";

    /**
     * Generate a signed deeplink URL.
     */
    public DeeplinkResult generateDeeplink(DeeplinkRequest request) {
        String action = request.action() != null ? request.action() : "pay";

        StringBuilder urlBuilder = new StringBuilder(SCHEME)
                .append(action)
                .append("?");

        // Add parameters
        if (request.token() != null) {
            urlBuilder.append("token=").append(encode(request.token())).append("&");
        }
        if (request.amount() != null) {
            urlBuilder.append("amount=").append(encode(request.amount())).append("&");
        }
        if (request.orderId() != null) {
            urlBuilder.append("orderId=").append(encode(request.orderId())).append("&");
        }
        if (request.partnerId() != null) {
            urlBuilder.append("partnerId=").append(encode(request.partnerId())).append("&");
        }

        // Add timestamp
        Instant expiresAt = Instant.now().plus(
                request.expiryMinutes() != null ? request.expiryMinutes() : 60,
                ChronoUnit.MINUTES);
        urlBuilder.append("exp=").append(expiresAt.getEpochSecond()).append("&");

        // Sign the URL
        String dataToSign = urlBuilder.toString();
        String signature = sign(dataToSign);
        urlBuilder.append("sig=").append(encode(signature));

        String deeplinkUrl = urlBuilder.toString();

        // Also generate a universal link (HTTPS fallback)
        String universalLink = "https://app.payu.id/" + action + "?token=" +
                encode(request.token() != null ? request.token() : "") +
                "&sig=" + encode(signature);

        Log.infof("Generated deeplink: action=%s token=%s", action, request.token());

        return new DeeplinkResult(
                deeplinkUrl,
                universalLink,
                action,
                expiresAt,
                Map.of(
                        "scheme", SCHEME + action,
                        "iosUniversalLink", universalLink,
                        "androidIntentUri", "intent://" + action +
                                "#Intent;scheme=payu;package=id.payu.app;end"
                )
        );
    }

    private String sign(String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(
                    deeplinkSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKey);
            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException("Failed to sign deeplink", e);
        }
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    /**
     * Deeplink generation request.
     */
    public record DeeplinkRequest(
            String action,     // pay, topup, transfer
            String token,      // checkout token
            String amount,
            String orderId,
            String partnerId,
            Integer expiryMinutes
    ) {}

    /**
     * Deeplink generation result with multiple link formats.
     */
    public record DeeplinkResult(
            String deeplinkUrl,
            String universalLink,
            String action,
            Instant expiresAt,
            Map<String, String> platforms
    ) {}
}
