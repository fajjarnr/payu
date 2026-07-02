package id.payu.saga.service;

import id.payu.saga.config.SagaProperties;
import id.payu.saga.entity.SagaInstance;
import id.payu.saga.model.SagaState;
import id.payu.saga.repository.SagaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Service for recovering failed or stalled sagas.
 */
@Slf4j
@RequiredArgsConstructor
public class SagaRecoveryService {

    private final SagaRepository sagaRepository;
    private final SagaProperties properties;

    /**
     * Recover a specific saga by ID.
     */
    public boolean recoverSaga(String sagaId) {
        log.info("Attempting to recover saga: {}", sagaId);

        return sagaRepository.findBySagaId(sagaId)
                .map(this::recoverSagaInstance)
                .orElse(false);
    }

    /**
     * Recover a saga instance.
     */
    protected boolean recoverSagaInstance(SagaInstance instance) {
        String sagaId = instance.getSagaId();
        SagaState currentState = SagaState.valueOf(instance.getCurrentState());

        if (currentState.isTerminal()) {
            log.info("Saga {} is already in terminal state: {}", sagaId, currentState);
            return false;
        }

        if (instance.isMaxRetriesExceeded()) {
            log.warn("Saga {} has exceeded max retries, marking as FAILED", sagaId);
            instance.transitionTo(SagaState.FAILED.name());
            instance.recordError("RECOVERY", "Max retries exceeded during recovery");
            sagaRepository.save(instance);
            return false;
        }

        // Increment retry count
        instance.incrementRetry();
        instance.transitionTo(SagaState.RETRYING.name());
        sagaRepository.save(instance);

        log.info("Saga {} marked for retry (attempt {}/{})",
                sagaId, instance.getRetryCount(), instance.getMaxRetries());

        return true;
    }

    /**
     * Recover all sagas that can be retried.
     */
    public int recoverRetryableSagas() {
        List<SagaInstance> retryable = sagaRepository.findRetryableSagas(
                Instant.now().minus(Duration.ofMinutes(5)));

        log.info("Found {} sagas eligible for recovery", retryable.size());

        int recovered = 0;
        for (SagaInstance instance : retryable) {
            if (recoverSagaInstance(instance)) {
                recovered++;
            }
        }

        return recovered;
    }

    /**
     * Recover all stalled sagas.
     */
    public int recoverStalledSagas(Duration stallThreshold) {
        List<SagaInstance> stalled = sagaRepository.findStalledSagas(
                Instant.now().minus(stallThreshold));

        log.info("Found {} stalled sagas", stalled.size());

        int recovered = 0;
        for (SagaInstance instance : stalled) {
            // Check if stalled in a compensating state
            if (instance.getCurrentState().startsWith("COMPENSAT")) {
                log.warn("Saga {} is stalled during compensation, may require manual intervention", instance.getSagaId());
                continue;
            }

            if (recoverSagaInstance(instance)) {
                recovered++;
            }
        }

        return recovered;
    }

    /**
     * Cancel a saga (if possible).
     */
    public boolean cancelSaga(String sagaId, String reason) {
        log.info("Attempting to cancel saga: {} - reason: {}", sagaId, reason);

        return sagaRepository.findBySagaId(sagaId)
                .map(instance -> {
                    SagaState currentState = SagaState.valueOf(instance.getCurrentState());

                    if (currentState.isTerminal()) {
                        log.warn("Cannot cancel saga {} - already in terminal state: {}",
                                sagaId, currentState);
                        return false;
                    }

                    instance.transitionTo(SagaState.CANCELLED.name());
                    instance.recordError("CANCEL", reason);
                    instance.complete();
                    sagaRepository.save(instance);

                    log.info("Saga {} cancelled successfully", sagaId);
                    return true;
                })
                .orElse(false);
    }

    /**
     * Pause a saga for manual review.
     */
    public boolean pauseSaga(String sagaId, String reason) {
        log.info("Pausing saga: {} - reason: {}", sagaId, reason);

        return sagaRepository.findBySagaId(sagaId)
                .map(instance -> {
                    SagaState currentState = SagaState.valueOf(instance.getCurrentState());

                    if (currentState.isTerminal() || currentState == SagaState.PAUSED) {
                        log.warn("Cannot pause saga {} - in state: {}", sagaId, currentState);
                        return false;
                    }

                    instance.transitionTo(SagaState.PAUSED.name());
                    instance.recordError("PAUSE", reason);
                    sagaRepository.save(instance);

                    log.info("Saga {} paused successfully", sagaId);
                    return true;
                })
                .orElse(false);
    }

    /**
     * Resume a paused saga.
     */
    public boolean resumeSaga(String sagaId) {
        log.info("Resuming saga: {}", sagaId);

        return sagaRepository.findBySagaId(sagaId)
                .map(instance -> {
                    if (!SagaState.PAUSED.name().equals(instance.getCurrentState())) {
                        log.warn("Cannot resume saga {} - not in PAUSED state", sagaId);
                        return false;
                    }

                    instance.transitionTo(SagaState.STARTED.name());
                    sagaRepository.save(instance);

                    log.info("Saga {} resumed successfully", sagaId);
                    return true;
                })
                .orElse(false);
    }

    /**
     * Scheduled recovery job.
     */
    @SchedulerLock(name = "SagaRecoveryService_scheduledRecovery", lockAtLeastFor = "PT1S", lockAtMostFor = "PT5M")
    @Scheduled(fixedDelay = 300000) // Every 5 minutes
    public void scheduledRecovery() {
        if (!properties.isCompensationEnabled()) {
            return;
        }

        log.debug("Running scheduled saga recovery");

        int recovered = recoverRetryableSagas();
        if (recovered > 0) {
            log.info("Recovered {} sagas during scheduled job", recovered);
        }

        int stalledRecovered = recoverStalledSagas(Duration.ofMinutes(30));
        if (stalledRecovered > 0) {
            log.info("Recovered {} stalled sagas during scheduled job", stalledRecovered);
        }
    }
}
