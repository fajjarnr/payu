package id.payu.dispute.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Chargeback Aggregate — state machine (ADR-0054 gap 054C)")
class ChargebackTest {

    private static final UUID TXN = UUID.randomUUID();
    private static final UUID CUST = UUID.randomUUID();
    private static final UUID MERCH = UUID.randomUUID();

    private Chargeback newCb() {
        return Chargeback.create(TXN, CUST, MERCH, new BigDecimal("100000.00"), "IDR", "fraud");
    }

    @Test
    void createSetsOpen() {
        Chargeback cb = newCb();
        assertThat(cb.getStatus()).isEqualTo(ChargebackStatus.OPEN);
        assertThat(cb.getAmount()).isEqualByComparingTo(new BigDecimal("100000.00"));
    }

    @Test
    void happyPathOpenToClosedViaAccepted() {
        Chargeback cb = newCb();
        cb.submit("SCHEME-123");
        assertThat(cb.getStatus()).isEqualTo(ChargebackStatus.SUBMITTED);
        cb.startReview();
        assertThat(cb.getStatus()).isEqualTo(ChargebackStatus.UNDER_REVIEW);
        cb.accept();
        assertThat(cb.getStatus()).isEqualTo(ChargebackStatus.ACCEPTED);
        cb.reverse();
        assertThat(cb.getStatus()).isEqualTo(ChargebackStatus.REVERSED);
        cb.close();
        assertThat(cb.getStatus()).isEqualTo(ChargebackStatus.CLOSED);
        assertThat(cb.isTerminal()).isTrue();
    }

    @Test
    void rejectPath() {
        Chargeback cb = newCb();
        cb.submit("SCHEME-99");
        cb.startReview();
        cb.reject("insufficient evidence");
        assertThat(cb.getStatus()).isEqualTo(ChargebackStatus.REJECTED);
        cb.close();
        assertThat(cb.getStatus()).isEqualTo(ChargebackStatus.CLOSED);
    }

    @Test
    void invalidTransitionsThrow() {
        Chargeback cb = newCb();
        assertThatThrownBy(cb::startReview).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(cb::accept).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> cb.reject("x")).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(cb::reverse).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(cb::close).isInstanceOf(IllegalStateException.class);

        cb.submit("S1");
        assertThatThrownBy(() -> cb.submit("S2")).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(cb::accept).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void bigDecimalPrecisionKept() {
        Chargeback cb = Chargeback.create(TXN, CUST, MERCH, new BigDecimal("12345.6789"), "IDR", "test");
        assertThat(cb.getAmount()).isEqualByComparingTo(new BigDecimal("12345.6789"));
    }
}
