package id.payu.transaction.application.service;

import id.payu.transaction.domain.port.in.DisbursementUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Async wrapper for disbursement processing.
 *
 * <p>The controller calls {@link #processDisbursementAsync(UUID)} to dispatch
 * the BI-FAST processing to a separate thread, returning HTTP 201 immediately.
 * The actual processing happens in the {@code SimpleAsyncTaskExecutor} thread pool
 * managed by Spring's @EnableAsync.
 *
 * <p>Why a separate bean (not @Async on DisbursementService.processDisbursement)?
 * Self-invocation of @Async methods does not go through the Spring proxy, so the
 * annotation would be ignored. A separate bean guarantees proxy interception.
 *
 * <p>Errors are logged but NOT propagated — the controller returns 201 regardless.
 * The async worker retries failed disbursements via the outbox pattern.
 *
 * <p>BUG-TXN-ASYNC-001 Fix (iter 44): DisbursementController called
 * {@code disbursementUseCase.processDisbursement(id)} synchronously despite the
 * comment claiming "asynchronously via Spring @Async equivalent". The fix:
 * 1. Added {@code @EnableAsync} to TransactionServiceApplication
 * 2. Extracted async dispatch to this separate bean
 * 3. Replaced synchronous call in controller with async dispatch
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AsyncDisbursementProcessorService {

    private final DisbursementUseCase disbursementUseCase;

    @Async
    public void processDisbursementAsync(UUID disbursementId) {
        log.info("Async processing disbursement: id={}", disbursementId);
        try {
            disbursementUseCase.processDisbursement(disbursementId);
            log.debug("Async processing completed: id={}", disbursementId);
        } catch (Exception e) {
            log.warn("Async processing failed for {}, will be retried by async worker: {}",
                    disbursementId, e.getMessage(), e);
        }
    }
}
