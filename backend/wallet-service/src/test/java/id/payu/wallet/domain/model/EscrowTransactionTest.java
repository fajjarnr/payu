package id.payu.wallet.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * QAMVP-007: escrow state machine — happy path, refund/expiry path, illegal
 * transitions, and net-amount accounting.
 */
@DisplayName("EscrowTransaction domain")
class EscrowTransactionTest {

    private EscrowTransaction escrow(EscrowStatus status) {
        return EscrowTransaction.builder()
                .id(UUID.randomUUID())
                .buyerAccountId("buyer-1")
                .sellerAccountId("seller-1")
                .partnerId("partner-1")
                .amount(new BigDecimal("100000.0000"))
                .feeAmount(new BigDecimal("2500.0000"))
                .currency("IDR")
                .status(status)
                .externalReferenceId("EXT-1")
                .description("escrow test")
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    void happyPathCreatedHeldReleasedSettled() {
        EscrowTransaction e = escrow(EscrowStatus.CREATED);
        e.hold("res-1");
        assertThat(e.getStatus()).isEqualTo(EscrowStatus.HELD);
        assertThat(e.getReservationId()).isEqualTo("res-1");
        assertThat(e.getHeldAt()).isNotNull();

        e.release();
        assertThat(e.getStatus()).isEqualTo(EscrowStatus.RELEASED);
        assertThat(e.getReleasedAt()).isNotNull();

        e.settle();
        assertThat(e.getStatus()).isEqualTo(EscrowStatus.SETTLED);
        assertThat(e.getSettledAt()).isNotNull();
    }

    @Test
    void refundFromHeld() {
        EscrowTransaction e = escrow(EscrowStatus.HELD);
        e.refund("buyer changed mind");
        assertThat(e.getStatus()).isEqualTo(EscrowStatus.REFUNDED);
        assertThat(e.getRefundReason()).isEqualTo("buyer changed mind");
        assertThat(e.getRefundedAt()).isNotNull();
    }

    @Test
    void expiryThenRefund() {
        EscrowTransaction e = escrow(EscrowStatus.HELD);
        e.expire();
        assertThat(e.getStatus()).isEqualTo(EscrowStatus.EXPIRED);

        e.refund("auto-refund on expiry");
        assertThat(e.getStatus()).isEqualTo(EscrowStatus.REFUNDED);
    }

    @Test
    void illegalTransitionsThrow() {
        assertThatThrownBy(() -> escrow(EscrowStatus.HELD).hold("x"))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> escrow(EscrowStatus.CREATED).release())
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> escrow(EscrowStatus.HELD).settle())
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> escrow(EscrowStatus.CREATED).refund("x"))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> escrow(EscrowStatus.CREATED).expire())
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> escrow(EscrowStatus.RELEASED).expire())
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void netAmountIsAmountMinusFee() {
        assertThat(escrow(EscrowStatus.CREATED).getNetAmount())
                .isEqualByComparingTo(new BigDecimal("97500.0000"));
    }

    @Test
    void netAmountWithoutFee() {
        EscrowTransaction e = escrow(EscrowStatus.CREATED);
        e.setFeeAmount(null);
        assertThat(e.getNetAmount()).isEqualByComparingTo(new BigDecimal("100000.0000"));
    }

    @Test
    void isExpiredOnlyWhenHeldAndPastExpiry() {
        EscrowTransaction e = escrow(EscrowStatus.HELD);
        e.setExpiresAt(LocalDateTime.now().minusMinutes(1));
        assertThat(e.isExpired()).isTrue();

        EscrowTransaction future = escrow(EscrowStatus.HELD);
        future.setExpiresAt(LocalDateTime.now().plusMinutes(1));
        assertThat(future.isExpired()).isFalse();

        EscrowTransaction noExpiry = escrow(EscrowStatus.HELD);
        assertThat(noExpiry.isExpired()).isFalse();

        EscrowTransaction settled = escrow(EscrowStatus.SETTLED);
        settled.setExpiresAt(LocalDateTime.now().minusMinutes(1));
        assertThat(settled.isExpired()).isFalse();
    }

    @Test
    void settersAndGettersRoundTrip() {
        EscrowTransaction e = escrow(EscrowStatus.CREATED);
        UUID id = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();

        e.setId(id);
        e.setBuyerAccountId("buyer-2");
        e.setSellerAccountId("seller-2");
        e.setPartnerId("partner-2");
        e.setAmount(new BigDecimal("50.0000"));
        e.setFeeAmount(BigDecimal.ONE);
        e.setCurrency("USD");
        e.setStatus(EscrowStatus.HELD);
        e.setExternalReferenceId("EXT-2");
        e.setDescription("d");
        e.setReservationId("res-2");
        e.setExpiresAt(now);
        e.setHeldAt(now);
        e.setReleasedAt(now);
        e.setSettledAt(now);
        e.setRefundedAt(now);
        e.setRefundReason("reason");
        e.setCreatedAt(now);
        e.setUpdatedAt(now);

        assertThat(e.getId()).isEqualTo(id);
        assertThat(e.getBuyerAccountId()).isEqualTo("buyer-2");
        assertThat(e.getSellerAccountId()).isEqualTo("seller-2");
        assertThat(e.getPartnerId()).isEqualTo("partner-2");
        assertThat(e.getAmount()).isEqualByComparingTo(new BigDecimal("50.0000"));
        assertThat(e.getFeeAmount()).isEqualByComparingTo(BigDecimal.ONE);
        assertThat(e.getCurrency()).isEqualTo("USD");
        assertThat(e.getStatus()).isEqualTo(EscrowStatus.HELD);
        assertThat(e.getExternalReferenceId()).isEqualTo("EXT-2");
        assertThat(e.getDescription()).isEqualTo("d");
        assertThat(e.getReservationId()).isEqualTo("res-2");
        assertThat(e.getExpiresAt()).isEqualTo(now);
        assertThat(e.getHeldAt()).isEqualTo(now);
        assertThat(e.getReleasedAt()).isEqualTo(now);
        assertThat(e.getSettledAt()).isEqualTo(now);
        assertThat(e.getRefundedAt()).isEqualTo(now);
        assertThat(e.getRefundReason()).isEqualTo("reason");
        assertThat(e.getCreatedAt()).isEqualTo(now);
        assertThat(e.getUpdatedAt()).isEqualTo(now);
    }
}
