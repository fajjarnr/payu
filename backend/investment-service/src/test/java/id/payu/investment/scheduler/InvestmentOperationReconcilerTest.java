package id.payu.investment.scheduler;

import id.payu.investment.domain.model.InvestmentOperation;
import id.payu.investment.domain.port.out.InvestmentPersistencePort;
import id.payu.investment.domain.port.out.WalletServicePort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InvestmentOperationReconcilerTest {

    @Mock
    private InvestmentPersistencePort investmentPersistencePort;
    @Mock
    private WalletServicePort walletServicePort;

    @Test
    void requestedOperationIsDebitedWithStableReferenceThenMarkedForCompensation() {
        InvestmentOperation operation = operation();
        when(investmentPersistencePort.findInvestmentOperationsForReconciliation(any(), any()))
                .thenReturn(List.of(operation));

        new InvestmentOperationReconciler(investmentPersistencePort, walletServicePort).reconcile();

        verify(walletServicePort).deductBalance(operation.getUserId(), operation.getAmount(), operation.getDebitReference());
        verify(investmentPersistencePort).markInvestmentOperationCompensationPending(
                eq(operation.getId()), eq("Investment debit completed without a committed local operation"));
    }

    @Test
    void compensationPendingOperationIsCreditedOnceAndCompleted() {
        InvestmentOperation operation = operation();
        operation.markCompensationPending("local transaction timeout");
        when(investmentPersistencePort.findInvestmentOperationsForReconciliation(any(), any()))
                .thenReturn(List.of(operation));

        new InvestmentOperationReconciler(investmentPersistencePort, walletServicePort).reconcile();

        verify(walletServicePort).creditBalance(operation.getUserId(), operation.getAmount(), operation.getCompensationReference());
        verify(investmentPersistencePort).saveInvestmentOperation(operation);
    }

    private InvestmentOperation operation() {
        return InvestmentOperation.requested("idem-1", "account-1", "user-1",
                id.payu.investment.domain.model.InvestmentOperationType.GOLD_PURCHASE,
                "XAU", null, new BigDecimal("100.00"), new BigDecimal("1250000.00"), "IDR");
    }
}
