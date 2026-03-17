package id.payu.saga.service;

import id.payu.saga.config.SagaProperties;
import id.payu.saga.entity.SagaInstance;
import id.payu.saga.model.SagaState;
import id.payu.saga.repository.SagaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service for monitoring saga execution and health.
 */
@Slf4j
@RequiredArgsConstructor
public class SagaMonitorService {

    private final SagaRepository sagaRepository;
    private final SagaProperties properties;

    /**
     * Get saga statistics by state (across all saga types).
     */
    public Map<String, Long> getSagaStatistics() {
        Map<String, Long> stats = new HashMap<>();

        for (SagaState state : SagaState.values()) {
            long count = sagaRepository.findByCurrentState(state.name()).size();
            stats.put(state.name(), count);
        }

        return stats;
    }

    /**
     * Get saga statistics for a specific saga type.
     */
    public Map<String, Long> getSagaStatisticsByType(String sagaType) {
        Map<String, Long> stats = new HashMap<>();

        for (SagaState state : SagaState.values()) {
            long count = sagaRepository.countBySagaTypeAndCurrentState(sagaType, state.name());
            stats.put(state.name(), count);
        }

        return stats;
    }

    /**
     * Get all stalled sagas (not updated for a threshold period).
     */
    public List<SagaInstance> getStalledSagas(Duration threshold) {
        Instant cutoff = Instant.now().minus(threshold);
        return sagaRepository.findStalledSagas(cutoff);
    }

    /**
     * Get sagas that can be retried.
     */
    public List<SagaInstance> getRetryableSagas(Duration retryInterval) {
        Instant cutoff = Instant.now().minus(retryInterval);
        return sagaRepository.findRetryableSagas(cutoff);
    }

    /**
     * Get incomplete sagas.
     */
    public List<SagaInstance> getIncompleteSagas() {
        return sagaRepository.findIncompleteSagas();
    }

    /**
     * Health check for saga system.
     */
    public SagaHealth health() {
        long incompleteCount = sagaRepository.findIncompleteSagas().size();
        long stalledCount = getStalledSagas(Duration.ofHours(1)).size();

        SagaHealth.SagaHealthStatus status;
        if (stalledCount > 100) {
            status = SagaHealth.SagaHealthStatus.DOWN;
        } else if (stalledCount > 10 || incompleteCount > 1000) {
            status = SagaHealth.SagaHealthStatus.DEGRADED;
        } else {
            status = SagaHealth.SagaHealthStatus.UP;
        }

        return SagaHealth.builder()
                .status(status)
                .incompleteSagas(incompleteCount)
                .stalledSagas(stalledCount)
                .build();
    }

    /**
     * Scheduled check for stalled sagas.
     */
    @Scheduled(fixedDelay = 60000) // Every minute
    public void checkStalledSagas() {
        if (!properties.isMonitoringEnabled()) {
            return;
        }

        List<SagaInstance> stalled = getStalledSagas(Duration.ofMinutes(10));
        if (!stalled.isEmpty()) {
            log.warn("Detected {} stalled saga instances", stalled.size());

            Map<String, Long> byType = stalled.stream()
                    .collect(Collectors.groupingBy(SagaInstance::getSagaType, Collectors.counting()));

            byType.forEach((type, count) ->
                log.warn("  - {}: {} stalled instances", type, count));
        }
    }

    /**
     * Saga health information.
     */
    @lombok.Builder
    @lombok.Data
    public static class SagaHealth {
        private SagaHealthStatus status;
        private long incompleteSagas;
        private long stalledSagas;

        public enum SagaHealthStatus {
            UP, DEGRADED, DOWN
        }
    }
}
