package id.payu.investment.scheduler;

import id.payu.investment.domain.model.InvestmentOperation;
import id.payu.investment.domain.model.InvestmentOperationStatus;
import id.payu.investment.domain.port.out.InvestmentPersistencePort;
import id.payu.investment.domain.port.out.WalletServicePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class InvestmentOperationReconciler {

    private final InvestmentPersistencePort investmentPersistencePort;
    private final WalletServicePort walletServicePort;

    @Scheduled(fixedDelayString = "${payu.investment.operations.reconcile-delay-ms:60000}")
    @SchedulerLock(name = "InvestmentOperationReconciler_reconcile", lockAtMostFor = "PT5M")
    public void reconcile() {
        List<InvestmentOperation> operations = investmentPersistencePort.findInvestmentOperationsForReconciliation(
                List.of(InvestmentOperationStatus.DEBIT_REQUESTED,
                        InvestmentOperationStatus.DEBITED,
                        InvestmentOperationStatus.COMPENSATION_PENDING),
                LocalDateTime.now());
        operations.forEach(this::reconcile);
    }

    private void reconcile(InvestmentOperation operation) {
        try {
            if (operation.getStatus() == InvestmentOperationStatus.DEBIT_REQUESTED) {
                walletServicePort.deductBalance(operation.getUserId(), operation.getAmount(),
                        operation.getDebitReference());
                investmentPersistencePort.markInvestmentOperationCompensationPending(
                        operation.getId(), "Investment debit completed without a committed local operation");
                return;
            }

            walletServicePort.creditBalance(operation.getUserId(), operation.getAmount(),
                    operation.getCompensationReference());
            operation.markCompensated();
            investmentPersistencePort.saveInvestmentOperation(operation);
        } catch (Exception error) {
            log.error("Investment operation reconciliation failed: operationId={}, status={}",
                    operation.getId(), operation.getStatus(), error);
            investmentPersistencePort.markInvestmentOperationRetry(operation.getId(), error.getMessage());
        }
    }
}
