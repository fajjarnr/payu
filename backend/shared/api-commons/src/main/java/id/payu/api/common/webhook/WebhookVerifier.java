package id.payu.api.common.webhook;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;

/**
 * HMAC-SHA256 signature verifier for webhook security.
 *
 * <p>This component provides secure verification of webhook signatures using:
 * <ul>
 *   <li>HMAC-SHA256 signature computation</li>
 *   <li>Timestamp tolerance validation (replay attack prevention)</li>
 *   <li>Constant-time comparison (timing attack prevention)</li>
 * </ul>
 *
 * <p><strong>Signature Format:</strong>
 * <pre>X-Webhook-Signature: sha256=&lt;hex_encoded_hmac&gt;</pre>
 *
 * <p><strong>Signature Construction:</strong>
 * <pre>HMAC-SHA256(timestamp + "." + payload)</pre>
 *
 * @author PayU Platform Engineering
 * @see WebhookHandler
 * @see WebhookConfig
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebhookVerifier {

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final String SIGNATURE_PREFIX = "sha256=";
    private static final int TIMESTAMP_TOLERANCE_SECONDS = 300; // 5 minutes

    private final WebhookConfig config;

    /**
     * Verifies the webhook signature with configured secret and default tolerance.
     *
     * @param payload the raw request body
     * @param signature the signature from X-Webhook-Signature header
     * @param timestamp the timestamp from X-Webhook-Timestamp header
     * @return true if signature is valid and timestamp is within tolerance
     */
    public boolean verify(String payload, String signature, long timestamp) {
        return verify(payload, signature, timestamp, config.getSecret());
    }

    /**
     * Verifies the webhook signature with specific secret and default tolerance.
     *
     * @param payload the raw request body
     * @param signature the signature from X-Webhook-Signature header
     * @param timestamp the timestamp from X-Webhook-Timestamp header
     * @param secret the webhook secret key
     * @return true if signature is valid and timestamp is within tolerance
     */
    public boolean verify(String payload, String signature, long timestamp, String secret) {
        if (!isValidInputs(payload, signature, timestamp, secret)) {
            return false;
        }

        // Check timestamp tolerance (prevent replay attacks)
        if (!isTimestampValid(timestamp)) {
            log.warn("Webhook timestamp outside tolerance: timestamp={}, current={}",
                    timestamp, System.currentTimeMillis());
            return false;
        }

        // Compute expected signature
        String expectedSignature;
        try {
            expectedSignature = computeSignature(payload, timestamp, secret);
        } catch (WebhookSecurityException e) {
            log.error("Failed to compute webhook signature", e);
            return false;
        }

        // Constant-time comparison (prevent timing attacks)
        return constantTimeEquals(signature, expectedSignature);
    }

    /**
     * Verifies signature without timestamp validation.
     *
     * <p><strong>Warning:</strong> Only use this method for testing or when
     * timestamp validation is handled separately.
     *
     * @param payload the raw request body
     * @param signature the signature from X-Webhook-Signature header
     * @param secret the webhook secret key
     * @return true if signature is valid
     */
    public boolean verifyWithoutTimestamp(String payload, String signature, String secret) {
        if (!isValidInputs(payload, signature, System.currentTimeMillis(), secret)) {
            return false;
        }

        String expectedSignature;
        try {
            expectedSignature = computeSignature(payload, System.currentTimeMillis(), secret);
        } catch (WebhookSecurityException e) {
            log.error("Failed to compute webhook signature", e);
            return false;
        }

        return constantTimeEquals(signature, expectedSignature);
    }

    /**
     * Computes the HMAC-SHA256 signature for a payload.
     *
     * @param payload the raw request body
     * @param timestamp the timestamp in milliseconds
     * @param secret the webhook secret key
     * @return the computed signature with "sha256=" prefix
     * @throws WebhookSecurityException if HMAC computation fails
     */
    public String computeSignature(String payload, long timestamp, String secret) {
        try {
            String message = timestamp + "." + payload;

            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            SecretKeySpec secretKeySpec = new SecretKeySpec(
                    secret.getBytes(StandardCharsets.UTF_8),
                    HMAC_ALGORITHM
            );
            mac.init(secretKeySpec);

            byte[] hmacBytes = mac.doFinal(message.getBytes(StandardCharsets.UTF_8));
            String hmacHex = bytesToHex(hmacBytes);

            return SIGNATURE_PREFIX + hmacHex;
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new WebhookSecurityException("Failed to compute HMAC signature", e);
        }
    }

    /**
     * Validates that all required inputs are present.
     */
    private boolean isValidInputs(String payload, String signature, long timestamp, String secret) {
        if (payload == null || payload.isEmpty()) {
            log.warn("Webhook payload is null or empty");
            return false;
        }
        if (signature == null || signature.isEmpty()) {
            log.warn("Webhook signature is null or empty");
            return false;
        }
        if (timestamp <= 0) {
            log.warn("Webhook timestamp is invalid: {}", timestamp);
            return false;
        }
        if (secret == null || secret.isEmpty()) {
            log.warn("Webhook secret is not configured");
            return false;
        }
        return true;
    }

    /**
     * Checks if timestamp is within acceptable tolerance.
     */
    private boolean isTimestampValid(long timestamp) {
        long now = System.currentTimeMillis();
        long toleranceMs = config.getToleranceSeconds() * 1000L;

        // Check if timestamp is not too far in the past
        if (timestamp < now - toleranceMs) {
            return false;
        }

        // Check if timestamp is not in the future (with small buffer for clock skew)
        long clockSkewBuffer = 5000; // 5 seconds
        if (timestamp > now + clockSkewBuffer) {
            return false;
        }

        return true;
    }

    /**
     * Performs constant-time comparison of two strings.
     *
     * <p>This prevents timing attacks by ensuring the comparison takes
     * the same amount of time regardless of where the strings differ.
     *
     * @param a first string
     * @param b second string
     * @return true if strings are equal
     */
    private boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) {
            return false;
        }

        byte[] aBytes = a.getBytes(StandardCharsets.UTF_8);
        byte[] bBytes = b.getBytes(StandardCharsets.UTF_8);

        return MessageDigest.isEqual(aBytes, bBytes);
    }

    /**
     * Converts byte array to hexadecimal string.
     */
    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    /**
     * Exception thrown when webhook security operations fail.
     */
    public static class WebhookSecurityException extends RuntimeException {
        public WebhookSecurityException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
