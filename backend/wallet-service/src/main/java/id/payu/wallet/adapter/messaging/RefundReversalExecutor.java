package id.payu.wallet.adapter.messaging;

import id.payu.wallet.adapter.persistence.entity.RefundReversalExecutionEntity;
import id.payu.wallet.adapter.persistence.repository.RefundReversalExecutionRepository;
import id.payu.wallet.domain.model.RefundReversalStatus;
import id.payu.wallet.domain.port.in.WalletUseCase;
import id.payu.wallet.interfaces.dto.RefundRequestedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class RefundReversalExecutor {

    private final RefundReversalExecutionRepository executionRepository;
    private final WalletUseCase walletUseCase;

    public void execute(RefundRequestedEvent event) {
        RefundReversalExecutionEntity execution = claim(event);
        if (execution.getStatus() == RefundReversalStatus.COMPLETED) {
            return;
        }
        if (!valid(event)) {
            markReconciliationRequired(execution, "Refund event lacks valid sender, recipient, amount, or currency");
            return;
        }

        try {
            walletUseCase.reverseTransfer(
                    event.senderAccountId(), event.recipientAccountId(), event.amount(),
                    event.currency(), event.refundId(), event.reason());
            execution.setStatus(RefundReversalStatus.COMPLETED);
            execution.setLastError(null);
            execution.setCompletedAt(java.time.LocalDateTime.now());
            executionRepository.saveAndFlush(execution);
        } catch (RuntimeException exception) {
            markReconciliationRequired(execution, exception.getMessage());
            throw exception;
        }
    }

    @Scheduled(fixedDelayString = "${payu.refund-reversal.reconcile-delay:60000}")
    @SchedulerLock(name = "RefundReversalExecutor_reconcile", lockAtMostFor = "PT30S")
    public void reconcile() {
        executionRepository.findByStatusIn(List.of(
                        RefundReversalStatus.PROCESSING,
                        RefundReversalStatus.RECONCILIATION_REQUIRED))
                .forEach(execution -> execute(new RefundRequestedEvent(
                        execution.getRefundId(), execution.getTransactionId(), execution.getAmount(),
                        execution.getCurrency(), execution.getReason(), execution.getSenderAccountId(),
                        execution.getRecipientAccountId())));
    }

    private RefundReversalExecutionEntity claim(RefundRequestedEvent event) {
        RefundReversalExecutionEntity execution = executionRepository.findByRefundId(event.refundId())
                .orElseGet(() -> newExecution(event));
        if (execution.getStatus() != RefundReversalStatus.COMPLETED) {
            execution.setStatus(RefundReversalStatus.PROCESSING);
            execution.setAttempts(execution.getAttempts() + 1);
            executionRepository.saveAndFlush(execution);
        }
        return execution;
    }

    private RefundReversalExecutionEntity newExecution(RefundRequestedEvent event) {
        RefundReversalExecutionEntity execution = new RefundReversalExecutionEntity();
        execution.setRefundId(event.refundId());
        execution.setTransactionId(event.transactionId());
        execution.setSenderAccountId(event.senderAccountId());
        execution.setRecipientAccountId(event.recipientAccountId());
        execution.setAmount(event.amount());
        execution.setCurrency(event.currency());
        execution.setReason(event.reason());
        execution.setAttempts(0);
        return execution;
    }

    private boolean valid(RefundRequestedEvent event) {
        return event.refundId() != null && event.transactionId() != null
                && event.senderAccountId() != null && !event.senderAccountId().isBlank()
                && event.recipientAccountId() != null && !event.recipientAccountId().isBlank()
                && event.amount() != null && event.amount().signum() > 0
                && event.currency() != null && !event.currency().isBlank();
    }

    private void markReconciliationRequired(RefundReversalExecutionEntity execution, String message) {
        execution.setStatus(RefundReversalStatus.RECONCILIATION_REQUIRED);
        execution.setLastError(message == null || message.isBlank() ? "Refund reversal failed" : message);
        executionRepository.saveAndFlush(execution);
        log.error("Refund reversal requires reconciliation: refundId={}, reason={}",
                execution.getRefundId(), execution.getLastError());
    }
}
