package id.payu.dispute.adapter.messaging;

import id.payu.dispute.domain.model.Refund;
import id.payu.dispute.domain.model.TransactionDetails;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

/**
 * Signs refund command payloads with HMAC-SHA256 so the wallet-service
 * consumer can reject forged events. Canonical field order MUST match
 * {@code RefundEventSignatureVerifier} in wallet-service.
 */
@Slf4j
@Component
public class RefundEventSigner {

    private static final String HMAC_ALGORITHM = "HmacSHA256";

    private final String hmacSecret;

    public RefundEventSigner(
            @Value("${payu.refund-event.hmac-secret:}") String hmacSecret) {
        this.hmacSecret = hmacSecret == null ? "" : hmacSecret;
    }

    public String sign(Refund refund, TransactionDetails transaction) {
        String canonical = String.join("|",
                refund.getId().toString(),
                refund.getTransactionId().toString(),
                refund.getAmount().toPlainString(),
                transaction.currency(),
                transaction.senderAccountId(),
                transaction.recipientAccountId());
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(hmacSecret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            return HexFormat.of().formatHex(mac.doFinal(canonical.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            log.error("Failed to sign refund event {}", refund.getId(), exception);
            throw new IllegalStateException("Refund event signing not configured", exception);
        }
    }
}
