package id.payu.saga.service;

import id.payu.saga.config.SagaProperties;
import id.payu.saga.entity.SagaInstance;
import id.payu.saga.model.SagaState;
import id.payu.saga.repository.SagaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link SagaMonitorService}.
 * Tests statistics, stalled saga detection, and health checks.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SagaMonitorService")
class SagaMonitorServiceTest {

    @Mock
    private SagaRepository sagaRepository;

    @Mock
    private SagaProperties properties;

    @InjectMocks
    private SagaMonitorService monitorService;

    @Nested
    @DisplayName("getSagaStatistics()")
    class StatisticsTests {

        @Test
        @DisplayName("should return count for each state")
        void shouldReturnCountForEachState() {
            when(sagaRepository.findByCurrentState(anyString())).thenReturn(List.of());
            when(sagaRepository.findByCurrentState("COMPLETED")).thenReturn(
                List.of(SagaInstance.create("Saga", SagaState.COMPLETED.name(), Map.of()))
            );
            when(sagaRepository.findByCurrentState("FAILED")).thenReturn(
                List.of(
                    SagaInstance.create("Saga", SagaState.FAILED.name(), Map.of()),
                    SagaInstance.create("Saga", SagaState.FAILED.name(), Map.of()),
                    SagaInstance.create("Saga", SagaState.FAILED.name(), Map.of())
                )
            );

            Map<String, Long> stats = monitorService.getSagaStatistics();

            assertThat(stats).hasSize(SagaState.values().length);
            assertThat(stats.get("COMPLETED")).isEqualTo(1L);
            assertThat(stats.get("FAILED")).isEqualTo(3L);
        }
    }

    @Nested
    @DisplayName("getSagaStatisticsByType()")
    class StatisticsByTypeTests {

        @Test
        @DisplayName("should filter statistics by saga type")
        void shouldFilterByType() {
            when(sagaRepository.countBySagaTypeAndCurrentState(anyString(), anyString())).thenReturn(0L);
            when(sagaRepository.countBySagaTypeAndCurrentState("TransferSaga", "COMPLETED")).thenReturn(5L);

            Map<String, Long> stats = monitorService.getSagaStatisticsByType("TransferSaga");

            assertThat(stats.get("COMPLETED")).isEqualTo(5L);
        }
    }

    @Nested
    @DisplayName("health()")
    class HealthCheckTests {

        @Test
        @DisplayName("should return UP when no stalled sagas")
        void shouldReturnUpWhenHealthy() {
            when(sagaRepository.findIncompleteSagas()).thenReturn(List.of());
            when(sagaRepository.findStalledSagas(any(Instant.class))).thenReturn(List.of());

            SagaMonitorService.SagaHealth health = monitorService.health();

            assertThat(health.getStatus()).isEqualTo(SagaHealthStatus.UP);
            assertThat(health.getIncompleteSagas()).isZero();
            assertThat(health.getStalledSagas()).isZero();
        }

        @Test
        @DisplayName("should return DEGRADED when stalled > 10")
        void shouldReturnDegradedWhenStalled() {
            when(sagaRepository.findIncompleteSagas()).thenReturn(List.of());

            List<SagaInstance> stalledList = new ArrayList<>();
            for (int i = 0; i < 15; i++) {
                stalledList.add(SagaInstance.create("Saga", SagaState.IN_PROGRESS.name(), Map.of()));
            }
            when(sagaRepository.findStalledSagas(any(Instant.class))).thenReturn(stalledList);

            SagaMonitorService.SagaHealth health = monitorService.health();

            assertThat(health.getStatus()).isEqualTo(SagaHealthStatus.DEGRADED);
            assertThat(health.getStalledSagas()).isEqualTo(15);
        }

        @Test
        @DisplayName("should return DOWN when stalled > 100")
        void shouldReturnDownWhenManyStalled() {
            when(sagaRepository.findIncompleteSagas()).thenReturn(List.of());

            List<SagaInstance> stalledList = new ArrayList<>();
            for (int i = 0; i < 101; i++) {
                stalledList.add(SagaInstance.create("Saga", SagaState.IN_PROGRESS.name(), Map.of()));
            }
            when(sagaRepository.findStalledSagas(any(Instant.class))).thenReturn(stalledList);

            SagaMonitorService.SagaHealth health = monitorService.health();

            assertThat(health.getStatus()).isEqualTo(SagaHealthStatus.DOWN);
        }

        @Test
        @DisplayName("should return DEGRADED when incomplete > 1000")
        void shouldReturnDegradedWhenManyIncomplete() {
            List<SagaInstance> incompleteList = new ArrayList<>();
            for (int i = 0; i < 1001; i++) {
                incompleteList.add(SagaInstance.create("Saga", SagaState.IN_PROGRESS.name(), Map.of()));
            }
            when(sagaRepository.findIncompleteSagas()).thenReturn(incompleteList);
            when(sagaRepository.findStalledSagas(any(Instant.class))).thenReturn(List.of());

            SagaMonitorService.SagaHealth health = monitorService.health();

            assertThat(health.getStatus()).isEqualTo(SagaHealthStatus.DEGRADED);
        }
    }

    @Nested
    @DisplayName("checkStalledSagas()")
    class CheckStalledTests {

        @Test
        @DisplayName("should skip when monitoring disabled")
        void shouldSkipWhenDisabled() {
            when(properties.isMonitoringEnabled()).thenReturn(false);

            monitorService.checkStalledSagas();

            verify(sagaRepository, never()).findStalledSagas(any());
        }

        @Test
        @DisplayName("should detect stalled sagas when monitoring enabled")
        void shouldDetectStalledSagas() {
            when(properties.isMonitoringEnabled()).thenReturn(true);
            when(sagaRepository.findStalledSagas(any(Instant.class))).thenReturn(List.of(
                    SagaInstance.create("TransferSaga", SagaState.IN_PROGRESS.name(), Map.of())
            ));

            monitorService.checkStalledSagas();

            verify(sagaRepository).findStalledSagas(any(Instant.class));
        }
    }
}
