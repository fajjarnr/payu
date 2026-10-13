package id.payu.wallet.adapter.messaging;

import id.payu.wallet.interfaces.dto.RefundRequestedEvent;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class RefundEventSignatureVerifierTest {

    private static final UUID REFUND_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440001");
    private static final UUID TRANSACTION_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440002");

    private RefundRequestedEvent signedEvent(String secret) {
        RefundRequestedEvent unsigned = new RefundRequestedEvent(
                REFUND_ID, TRANSACTION_ID, new BigDecimal("100.00"), "IDR",
                "reason", "sender", "recipient");
        String signature = RefundEventSignatureVerifier.sign(unsigned, secret);
        return new RefundRequestedEvent(
                REFUND_ID, TRANSACTION_ID, new BigDecimal("100.00"), "IDR",
                "reason", "sender", "recipient", signature);
    }

    @Test
    void acceptsCorrectlySignedEvent() {
        RefundEventSignatureVerifier verifier = new RefundEventSignatureVerifier("test-secret");
        assertThat(verifier.isAuthentic(signedEvent("test-secret"))).isTrue();
    }

    @Test
    void rejectsTamperedAmount() {
        RefundEventSignatureVerifier verifier = new RefundEventSignatureVerifier("test-secret");
        RefundRequestedEvent signed = signedEvent("test-secret");
        RefundRequestedEvent tampered = new RefundRequestedEvent(
                signed.refundId(), signed.transactionId(), new BigDecimal("999.00"),
                signed.currency(), signed.reason(), signed.senderAccountId(),
                signed.recipientAccountId(), signed.signature());
        assertThat(verifier.isAuthentic(tampered)).isFalse();
    }

    @Test
    void rejectsUnsignedEventAndEmptySecret() {
        assertThat(new RefundEventSignatureVerifier("test-secret").isAuthentic(
                new RefundRequestedEvent(REFUND_ID, TRANSACTION_ID, new BigDecimal("100.00"),
                        "IDR", "reason", "sender", "recipient"))).isFalse();
        assertThat(new RefundEventSignatureVerifier("").isAuthentic(signedEvent("test-secret"))).isFalse();
    }
}
