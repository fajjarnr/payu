package id.payu.lending.application.service;

import id.payu.lending.domain.port.out.PayLaterPersistencePort;
import id.payu.lending.domain.port.out.PayLaterTransactionPersistencePort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PayLaterTransactionServiceTest {

    @Mock
    private PayLaterPersistencePort payLaterPersistencePort;

    @Mock
    private PayLaterTransactionPersistencePort transactionPersistencePort;

    @Test
    void recordPurchaseRejectsNonPositiveAmountBeforeChangingCredit() {
        UUID userId = UUID.randomUUID();

        assertThatThrownBy(() -> service().recordPurchase(
                userId, "merchant", new BigDecimal("-1"), "test"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("positive");

        verify(payLaterPersistencePort, never()).save(any());
        verify(transactionPersistencePort, never()).save(any());
    }

    @Test
    void recordPaymentRejectsNonPositiveAmountBeforeChangingCredit() {
        UUID userId = UUID.randomUUID();

        assertThatThrownBy(() -> service().recordPayment(userId, BigDecimal.ZERO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("positive");

        verify(payLaterPersistencePort, never()).save(any());
        verify(transactionPersistencePort, never()).save(any());
    }

    @Test
    void recordPaymentRejectsNullAmountBeforeChangingCredit() {
        UUID userId = UUID.randomUUID();

        assertThatThrownBy(() -> service().recordPayment(userId, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("positive");

        verify(payLaterPersistencePort, never()).save(any());
        verify(transactionPersistencePort, never()).save(any());
    }

    @Test
    void recordPaymentRejectsMoreThanFourDecimalPlaces() {
        UUID userId = UUID.randomUUID();

        assertThatThrownBy(() -> service().recordPayment(userId, new BigDecimal("1.00001")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("4 decimal places");

        verify(payLaterPersistencePort, never()).save(any());
        verify(transactionPersistencePort, never()).save(any());
    }

    private PayLaterTransactionService service() {
        return new PayLaterTransactionService(payLaterPersistencePort, transactionPersistencePort);
    }

}
