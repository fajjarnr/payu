package id.payu.transaction.application.saga;

import id.payu.saga.annotation.SagaOrchestration;
import id.payu.saga.model.SagaStep;
import id.payu.saga.model.StepResult;
import id.payu.saga.orchestrator.SagaOrchestrator;
import id.payu.saga.repository.SagaRepository;
import id.payu.transaction.domain.port.out.BifastServicePort;
import id.payu.transaction.domain.port.out.TransactionEventPublisherPort;
import id.payu.transaction.domain.port.out.TransactionPersistencePort;
import id.payu.transaction.domain.port.out.WalletServicePort;
import id.payu.transaction.dto.BifastTransferRequest;
import id.payu.transaction.dto.BifastTransferResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Saga orchestrator for inter-bank (BiFast) transfer lifecycle.
 * <p>
 * Steps:
 * 1. RESERVE_BALANCE — Reserve funds in sender wallet (compensate: release)
 * 2. INITIATE_BIFAST — Call BiFast API to initiate transfer (compensate: reversal)
 * 3. COMMIT_BALANCE — Commit reserved balance (terminal, no compensation)
 * 4. PUBLISH_EVENT — Emit domain event (non-critical, no compensation)
 * <p>
 * On failure, compensation runs in LIFO order. All state is persisted
 * to `saga_instances` table for crash recovery and audit.
 *
 * @see TransferSagaContext
 */
@Slf4j
@Component
@SagaOrchestration(
        value = "BiFastTransferSaga",
        persistent = true,
        compensating = true,
        timeoutMs = 120_000,
        publishEvents = true,
        eventTopic = "transaction.saga.events"
)
public class TransferSagaOrchestrator extends SagaOrchestrator<TransferSagaContext> {

    private final WalletServicePort walletService;
    private final BifastServicePort bifastService;
    private final TransactionPersistencePort transactionPersistence;

    public TransferSagaOrchestrator(
            SagaRepository sagaRepository,
            WalletServicePort walletService,
            BifastServicePort bifastService,
            TransactionPersistencePort transactionPersistence) {
        super(sagaRepository);
        this.walletService = walletService;
        this.bifastService = bifastService;
        this.transactionPersistence = transactionPersistence;

        initialize("BiFastTransferSaga", List.of(
                buildReserveBalanceStep(),
                buildInitiateBifastStep(),
                buildCommitBalanceStep(),
                buildPublishEventStep()
        ));
    }

    /**
     * Step 1: Reserve balance in sender's wallet.
     * Compensation: Release the reserved balance.
     */
    private SagaStep<TransferSagaContext> buildReserveBalanceStep() {
        return SagaStep.withCompensation(
                "RESERVE_BALANCE",
                ctx -> {
                    log.info("Saga step RESERVE_BALANCE: txn={}, amount={}",
                            ctx.getTransactionId(), ctx.getAmount());
                    try {
                        var response = walletService.reserveBalance(
                                ctx.getSenderAccountId(),
                                ctx.getTransactionId().toString(),
                                ctx.getAmount()
                        );
                        ctx.setBalanceReserved(true);
                        return StepResult.success(ctx, "Balance reserved")
                                .withMetadata("reservationResponse", response);
                    } catch (Exception e) {
                        log.error("Failed to reserve balance for txn={}: {}", ctx.getTransactionId(), e.getMessage());
                        return StepResult.failure(ctx, "Insufficient balance or wallet unavailable", e);
                    }
                },
                ctx -> {
                    if (ctx.isBalanceReserved()) {
                        log.info("Compensating RESERVE_BALANCE: releasing for txn={}", ctx.getTransactionId());
                        try {
                            walletService.releaseBalance(
                                    ctx.getSenderAccountId(),
                                    ctx.getTransactionId().toString(),
                                    ctx.getAmount()
                            );
                            ctx.setBalanceReserved(false);
                        } catch (Exception e) {
                            log.error("Compensation failed for RESERVE_BALANCE: txn={}", ctx.getTransactionId(), e);
                            return StepResult.failure(ctx, "Failed to release reserved balance", e);
                        }
                    }
                    return StepResult.success(ctx, "Balance reservation released");
                }
        );
    }

    /**
     * Step 2: Initiate BiFast transfer with retry.
     * Compensation: Log reversal request (BiFast reversals are manual).
     */
    private SagaStep<TransferSagaContext> buildInitiateBifastStep() {
        return SagaStep.<TransferSagaContext>builder()
                .name("INITIATE_BIFAST")
                .action(ctx -> {
                    log.info("Saga step INITIATE_BIFAST: txn={}, ref={}",
                            ctx.getTransactionId(), ctx.getReferenceNumber());
                    try {
                        BifastTransferRequest request = BifastTransferRequest.builder()
                                .referenceNumber(ctx.getReferenceNumber())
                                .amount(ctx.getAmount())
                                .beneficiaryAccountNumber(ctx.getRecipientAccountNumber())
                                .beneficiaryBankCode(ctx.getRecipientBankCode())
                                .build();
                        BifastTransferResponse response = bifastService.initiateTransfer(request);
                        ctx.setExternalTransferInitiated(true);
                        ctx.setExternalTransferReference(response.getReferenceNumber());
                        return StepResult.success(ctx, Map.of("bifastRef", response.getReferenceNumber()));
                    } catch (Exception e) {
                        log.error("BiFast transfer failed for txn={}: {}", ctx.getTransactionId(), e.getMessage());
                        return StepResult.retryableFailure(ctx, "BiFast transfer failed", e);
                    }
                })
                .compensation(ctx -> {
                    if (ctx.isExternalTransferInitiated()) {
                        log.warn("BiFast transfer initiated but saga failed. Manual reversal needed: txn={}, bifastRef={}",
                                ctx.getTransactionId(), ctx.getExternalTransferReference());
                        // BiFast reversals require manual reconciliation — log for ops team
                    }
                    return StepResult.success(ctx, "BiFast reversal request logged");
                })
                .maxRetries(2)
                .retryDelay(Duration.ofSeconds(2))
                .timeout(Duration.ofSeconds(30))
                .build();
    }

    /**
     * Step 3: Commit the reserved balance (deduct from wallet permanently).
     * No compensation — commitment is terminal.
     */
    private SagaStep<TransferSagaContext> buildCommitBalanceStep() {
        return SagaStep.of("COMMIT_BALANCE", ctx -> {
            log.info("Saga step COMMIT_BALANCE: txn={}", ctx.getTransactionId());
            try {
                walletService.commitBalance(
                        ctx.getSenderAccountId(),
                        ctx.getTransactionId().toString(),
                        ctx.getAmount()
                );
                return StepResult.success(ctx, "Balance committed");
            } catch (Exception e) {
                log.error("Failed to commit balance: txn={}", ctx.getTransactionId(), e);
                return StepResult.failure(ctx, "Balance commit failed", e);
            }
        });
    }

    /**
     * Step 4: Publish domain event (non-critical — continue on failure).
     */
    private SagaStep<TransferSagaContext> buildPublishEventStep() {
        return SagaStep.<TransferSagaContext>builder()
                .name("PUBLISH_EVENT")
                .action(ctx -> {
                    log.info("Saga step PUBLISH_EVENT: txn={}", ctx.getTransactionId());
                    // Event publication is handled by the outbox pattern in the calling code
                    return StepResult.success(ctx, "Event publish delegated to outbox");
                })
                .continueOnFailure(true)
                .build();
    }
}
