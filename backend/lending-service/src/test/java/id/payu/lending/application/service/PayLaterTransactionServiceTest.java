package id.payu.lending.application.service;

import id.payu.lending.domain.model.PayLater;
import id.payu.lending.domain.model.PayLaterStatus;
import id.payu.lending.domain.model.PayLaterTransaction;
import id.payu.lending.domain.model.TransactionStatus;
import id.payu.lending.domain.port.out.PayLaterPersistencePort;
import id.payu.lending.domain.port.out.PayLaterTransactionPersistencePort;
import id.payu.lending.domain.port.out.WalletPaymentPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PayLaterTransactionServiceTest {

    @Mock
    private PayLaterPersistencePort payLaterPersistencePort;
    @Mock
    private PayLaterTransactionPersistencePort transactionPersistencePort;
    @Mock
    private WalletPaymentPort walletPaymentPort;

    private UUID userId;
    private PayLater payLater;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        payLater = new PayLater();
        payLater.setId(UUID.randomUUID());
        payLater.setUserId(userId);
        payLater.setStatus(PayLaterStatus.ACTIVE);
        payLater.setCreditLimit(new BigDecimal("1000000"));
        payLater.setUsedCredit(new BigDecimal("100000"));
        payLater.setAvailableCredit(new BigDecimal("900000"));
        payLater.setCreatedAt(LocalDateTime.now());
        payLater.setUpdatedAt(LocalDateTime.now());
    }

    private PayLaterTransactionService service() {
        return new PayLaterTransactionService(payLaterPersistencePort, transactionPersistencePort, walletPaymentPort);
    }

    @Test
    void recordPurchaseRejectsNonPositiveAmountBeforeChangingCredit() {
        assertThatThrownBy(() -> service().recordPurchase(
                userId, "merchant", new BigDecimal("-1"), "test"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("positive");

        verify(payLaterPersistencePort, never()).save(any());
        verify(transactionPersistencePort, never()).save(any());
    }

    @Test
    void recordPaymentRejectsNonPositiveAmountBeforeChangingCredit() {
        assertThatThrownBy(() -> service().recordPayment(userId, BigDecimal.ZERO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("positive");

        verify(payLaterPersistencePort, never()).save(any());
        verify(transactionPersistencePort, never()).save(any());
    }

    @Test
    void recordPaymentRejectsNullAmountBeforeChangingCredit() {
        assertThatThrownBy(() -> service().recordPayment(userId, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("positive");

        verify(payLaterPersistencePort, never()).save(any());
        verify(transactionPersistencePort, never()).save(any());
    }

    @Test
    void recordPaymentRejectsMoreThanFourDecimalPlaces() {
        assertThatThrownBy(() -> service().recordPayment(userId, new BigDecimal("1.00001")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("4 decimal places");

        verify(payLaterPersistencePort, never()).save(any());
        verify(transactionPersistencePort, never()).save(any());
    }

    @Test
    void recordPurchaseLocksAccountRowForUpdate() {
        when(payLaterPersistencePort.findByUserIdForUpdate(userId)).thenReturn(Optional.of(payLater));
        when(transactionPersistencePort.save(any(PayLaterTransaction.class)))
                .thenAnswer(inv -> {
                    PayLaterTransaction t = inv.getArgument(0);
                    t.setId(UUID.randomUUID());
                    return t;
                });
        when(walletPaymentPort.creditAccount(any(), any(), any(), any(), any())).thenReturn("ledger-1");

        service().recordPurchase(userId, "merchant", new BigDecimal("50000"), "test", "ext-key-1");

        verify(payLaterPersistencePort).findByUserIdForUpdate(userId);
    }

    @Test
    void recordPurchaseIsIdempotentByExternalId() {
        PayLaterTransaction existing = new PayLaterTransaction();
        existing.setId(UUID.randomUUID());
        existing.setStatus(TransactionStatus.COMPLETED);
        when(transactionPersistencePort.findByExternalId("ext-replay-1")).thenReturn(Optional.of(existing));

        PayLaterTransaction result = service().recordPurchase(
                userId, "merchant", new BigDecimal("50000"), "test", "ext-replay-1");

        assertThat(result).isSameAs(existing);
        verify(transactionPersistencePort, never()).save(any(PayLaterTransaction.class));
        verify(walletPaymentPort, never()).creditAccount(any(), any(), any(), any(), any());
        verify(payLaterPersistencePort, never()).save(any(PayLater.class));
    }

    @Test
    void recordPurchaseCreditsWalletAfterSavingTransaction() {
        when(payLaterPersistencePort.findByUserIdForUpdate(userId)).thenReturn(Optional.of(payLater));
        when(transactionPersistencePort.save(any(PayLaterTransaction.class)))
                .thenAnswer(inv -> {
                    PayLaterTransaction t = inv.getArgument(0);
                    t.setId(UUID.randomUUID());
                    return t;
                });
        when(walletPaymentPort.creditAccount(any(), any(), any(), any(), any())).thenReturn("ledger-1");

        service().recordPurchase(userId, "merchant", new BigDecimal("50000"), "test", "ext-key-2");

        verify(walletPaymentPort).creditAccount(
                eq(payLater.getUserId().toString()), eq(new BigDecimal("50000")), eq("IDR"), any(), any());
    }

    @Test
    void recordPurchaseFailsClosedWhenWalletCreditFails() {
        when(payLaterPersistencePort.findByUserIdForUpdate(userId)).thenReturn(Optional.of(payLater));
        when(transactionPersistencePort.save(any(PayLaterTransaction.class)))
                .thenAnswer(inv -> {
                    PayLaterTransaction t = inv.getArgument(0);
                    t.setId(UUID.randomUUID());
                    return t;
                });
        doThrow(new IllegalStateException("wallet down"))
                .when(walletPaymentPort).creditAccount(any(), any(), any(), any(), any());

        assertThatThrownBy(() -> service().recordPurchase(
                userId, "merchant", new BigDecimal("50000"), "test", "ext-key-3"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void recordPaymentDebitsWalletForRepayment() {
        when(payLaterPersistencePort.findByUserIdForUpdate(userId)).thenReturn(Optional.of(payLater));
        when(transactionPersistencePort.save(any(PayLaterTransaction.class)))
                .thenAnswer(inv -> {
                    PayLaterTransaction t = inv.getArgument(0);
                    t.setId(UUID.randomUUID());
                    return t;
                });
        when(walletPaymentPort.collectRepayment(any(), eq(userId), any(), any(), any(), any()))
                .thenReturn("ledger-2");

        service().recordPayment(userId, new BigDecimal("50000"), "ext-pay-1");

        verify(walletPaymentPort).collectRepayment(
                any(), eq(userId), eq(new BigDecimal("50000")), eq("IDR"), any(), any());
    }
}
