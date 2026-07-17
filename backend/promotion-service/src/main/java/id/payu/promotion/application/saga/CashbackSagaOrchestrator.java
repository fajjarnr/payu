package id.payu.promotion.application.saga;

import id.payu.promotion.domain.model.Cashback;
import id.payu.promotion.domain.port.out.CashbackPersistencePort;
import id.payu.promotion.domain.port.out.WalletServicePort;
import id.payu.saga.model.SagaResult;
import id.payu.saga.model.SagaStep;
import id.payu.saga.model.StepResult;
import id.payu.saga.orchestrator.SagaOrchestrator;
import id.payu.saga.repository.SagaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import id.payu.promotion.domain.CashbackStatus;

/**
 * Saga orchestrator for cashback credit workflow.
 * Ensures atomicity between wallet credit and cashback record creation.
 *
 * Saga Steps:
 * 1. CREDIT_WALLET - Credit amount to user wallet via wallet-service
 * 2. RECORD_CASHBACK - Create cashback record with CREDITED status
 *
 * Compensation:
 * - If RECORD_CASHBACK fails, wallet credit is NOT reversed (money already given)
 *   but cashback record stays in PENDING status for manual reconciliation.
 */
@Component
public class CashbackSagaOrchestrator extends SagaOrchestrator<CashbackSagaContext> implements id.payu.promotion.domain.port.in.CashbackSagaUseCase {

    private static final Logger LOG = LoggerFactory.getLogger(CashbackSagaOrchestrator.class);

    private final WalletServicePort walletServicePort;
    private final CashbackPersistencePort cashbackRepository;

    public CashbackSagaOrchestrator(
            SagaRepository sagaRepository,
            @Qualifier("sagaTaskExecutor") TaskExecutor sagaTaskExecutor,
            @Qualifier("sagaRetryScheduler") ScheduledExecutorService sagaRetryScheduler,
            PlatformTransactionManager transactionManager,
            WalletServicePort walletServicePort,
            CashbackPersistencePort cashbackRepository) {
        super(sagaRepository, sagaTaskExecutor, sagaRetryScheduler, transactionManager);
        this.walletServicePort = walletServicePort;
        this.cashbackRepository = cashbackRepository;

        initialize("CASHBACK_CREDIT_SAGA", List.of(
            SagaStep.<CashbackSagaContext>withCompensation(
                "CREDIT_WALLET",
                this::creditWalletStep,
                this::compensateCreditWallet
            ),
            SagaStep.<CashbackSagaContext>withCompensation(
                "RECORD_CASHBACK",
                this::recordCashbackStep,
                this::compensateRecordCashback
            )
        ));
    }

    /**
     * Step 1: Credit wallet via wallet-service.
     * This must succeed before we mark cashback as CREDITED.
     */
    private StepResult<CashbackSagaContext> creditWalletStep(CashbackSagaContext context) {
        LOG.info("Saga Step: CREDIT_WALLET for account={}", context.getAccountId());

        try {
            boolean credited = walletServicePort.creditWallet(
                context.getAccountId(),
                context.getAmount(),
                context.getTransactionId(),
                "CashbackEntity for transaction " + context.getTransactionId()
            );

            if (credited) {
                context.setWalletCredited(true);
                LOG.info("Wallet credited successfully: account={}, amount={}",
                    context.getAccountId(), context.getAmount());
                return StepResult.success(context, Map.of(
                    "accountId", context.getAccountId(),
                    "amount", context.getAmount()
                ));
            } else {
                LOG.error("Wallet credit returned false: account={}", context.getAccountId());
                return StepResult.failure(context, "Wallet credit returned false");
            }
        } catch (Exception e) {
            LOG.error("Wallet credit failed: account={}, error={}", context.getAccountId(), e.getMessage());
            return StepResult.failure(context, "Wallet credit failed: " + e.getMessage(), e);
        }
    }

    /**
     * Compensation for CREDIT_WALLET step.
     * Note: In a real cashback scenario, we typically don't reverse credits
     * as it creates a poor user experience. Instead, we log for reconciliation.
     */
    private StepResult<CashbackSagaContext> compensateCreditWallet(CashbackSagaContext context) {
        LOG.warn("Saga Compensation: CREDIT_WALLET for account={}", context.getAccountId());
        // In production, you might want to create a reversal transaction
        // For now, we log and mark as success since we don't want to fail compensation
        return StepResult.success(context, "Credit wallet compensation logged");
    }

    /**
     * Step 2: Record cashback with CREDITED status.
     * Only executed after wallet credit succeeds.
     */
    private StepResult<CashbackSagaContext> recordCashbackStep(CashbackSagaContext context) {
        LOG.info("Saga Step: RECORD_CASHBACK for transaction={}", context.getTransactionId());

        try {
            var request = context.getRequest();

            Cashback cashback = new Cashback();
            cashback.setAccountId(request.accountId());
            cashback.setTransactionId(request.transactionId());
            cashback.setTransactionAmount(request.transactionAmount());
            cashback.setCashbackAmount(context.getAmount());
            cashback.setPercentage(calculatePercentage(context.getAmount(), request.transactionAmount()));
            cashback.setMerchantCode(request.merchantCode());
            cashback.setCategoryCode(request.categoryCode());
            cashback.setCashbackCode(request.cashbackCode());
            // IMPORTANT: Status is only set to CREDITED after wallet credit succeeds
            cashback.setStatus(CashbackStatus.CREDITED);
            cashback.setCreditedAt(LocalDateTime.now());

            cashback = cashbackRepository.save(cashback);
            context.setCashback(cashback);
            context.setCashbackId(cashback.getId());
            context.setCashbackRecorded(true);

            LOG.info("CashbackEntity recorded successfully: id={}", cashback.getId());
            return StepResult.success(context, Map.of(
                "cashbackId", cashback.getId().toString()
            ));
        } catch (Exception e) {
            LOG.error("Failed to record cashback: error={}", e.getMessage());
            return StepResult.failure(context, "Failed to record cashback: " + e.getMessage(), e);
        }
    }

    /**
     * Compensation for RECORD_CASHBACK step.
     * If recording fails, we mark the cashback as PENDING for manual reconciliation.
     */
    private StepResult<CashbackSagaContext> compensateRecordCashback(CashbackSagaContext context) {
        LOG.warn("Saga Compensation: RECORD_CASHBACK for transaction={}", context.getTransactionId());

        try {
            // If cashback was created, mark it as VOIDED for reconciliation
            if (context.getCashbackId() != null) {
                var cashbackOpt = cashbackRepository.findById(context.getCashbackId());
                if (cashbackOpt.isPresent()) {
                    Cashback cashback = cashbackOpt.get();
                    cashback.setStatus(CashbackStatus.VOIDED);
                    cashbackRepository.save(cashback);
                    LOG.info("CashbackEntity marked as VOIDED: id={}", context.getCashbackId());
                }
            }
            return StepResult.success(context, "Record cashback compensation completed");
        } catch (Exception e) {
            LOG.error("Failed to compensate record cashback: error={}", e.getMessage());
            return StepResult.failure(context, "Compensation failed: " + e.getMessage(), e);
        }
    }

    private BigDecimal calculatePercentage(BigDecimal cashbackAmount, BigDecimal transactionAmount) {
        if (transactionAmount.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return cashbackAmount.divide(transactionAmount, 4, java.math.RoundingMode.HALF_EVEN)
            .multiply(BigDecimal.valueOf(100));
    }

    /**
     * Execute cashback saga with the given request.
     *
     * @param context the saga context containing request data
     * @return SagaResult containing the execution result
     */
    public SagaResult<CashbackSagaContext> executeCashbackSaga(CashbackSagaContext context) {
        return execute(context);
    }

    @Override
    public id.payu.promotion.domain.model.CashbackSagaOutcome execute(id.payu.promotion.domain.model.CashbackCommand command) {
        var request = new id.payu.promotion.dto.CreateCashbackRequest(command.accountId(), command.transactionId(), command.transactionAmount(), command.merchantCode(), command.categoryCode(), command.cashbackCode());
        SagaResult<CashbackSagaContext> result = executeCashbackSaga(new CashbackSagaContext(request));
        Cashback cashback = result.getData() == null ? null : result.getData().getCashback();
        return new id.payu.promotion.domain.model.CashbackSagaOutcome(result.isSuccess(), result.isCompensated(), cashback,
            result.getErrorMessage(), result.getErrorStep(), result.getFinalState().name());
    }
}
