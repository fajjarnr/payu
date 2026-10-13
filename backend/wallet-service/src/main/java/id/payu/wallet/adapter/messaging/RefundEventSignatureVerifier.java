package id.payu.wallet.adapter.messaging;

import id.payu.wallet.interfaces.dto.RefundRequestedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

/**
 * Verifies HMAC-SHA256 authenticity of {@link RefundRequestedEvent} payloads.
 *
 * <p>The dispute-service signs the canonical event fields with the shared
 * {@code payu.refund-event.hmac-secret} before publishing; this consumer
 * rejects anything unsigned or mismatched. Fail-closed: an empty secret
 * rejects every event (no money moves until signing is configured).
 */
@Slf4j
@Component
public class RefundEventSignatureVerifier {

    private static final String HMAC_ALGORITHM = "HmacSHA256";

    private final String hmacSecret;

    public RefundEventSignatureVerifier(
            @Value("${payu.refund-event.hmac-secret:}") String hmacSecret) {
        this.hmacSecret = hmacSecret == null ? "" : hmacSecret;
    }

    public boolean isAuthentic(RefundRequestedEvent event) {
        if (hmacSecret.isBlank() || event == null || event.signature() == null) {
            return false;
        }
        String expected = sign(event, hmacSecret);
        return constantTimeEquals(expected, event.signature());
    }

    static String sign(RefundRequestedEvent event, String secret) {
        String canonical = String.join("|",
                String.valueOf(event.refundId()),
                String.valueOf(event.transactionId()),
                event.amount() == null ? "" : event.amount().toPlainString(),
                nullToEmpty(event.currency()),
                nullToEmpty(event.senderAccountId()),
                nullToEmpty(event.recipientAccountId()));
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            return HexFormat.of().formatHex(mac.doFinal(canonical.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            log.error("Failed to compute refund event HMAC", exception);
            return "";
        }
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private static boolean constantTimeEquals(String left, String right) {
        if (left == null || right == null || left.length() != right.length()) {
            return false;
        }
        int diff = 0;
        for (int i = 0; i < left.length(); i++) {
            diff |= left.charAt(i) ^ right.charAt(i);
        }
        return diff == 0;
    }
}
