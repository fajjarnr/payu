package id.payu.saga.service;

import id.payu.saga.config.SagaProperties;
import id.payu.saga.entity.SagaInstance;
import id.payu.saga.model.SagaState;
import id.payu.saga.repository.SagaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link SagaRecoveryService}.
 * Tests saga recovery, cancellation, pause/resume, and scheduled recovery.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SagaRecoveryService")
class SagaRecoveryServiceTest {

    @Mock
    private SagaRepository sagaRepository;

    @Mock
    private SagaProperties properties;

    @InjectMocks
    private SagaRecoveryService recoveryService;

    @Nested
    @DisplayName("recoverSaga()")
    class RecoverSagaTests {

        @Test
        @DisplayName("should recover non-terminal saga within retry limits")
        void shouldRecoverNonTerminalSaga() {
            // TIMED_OUT is non-terminal and retryable
            SagaInstance instance = SagaInstance.create("TransferSaga", SagaState.TIMED_OUT.name(), Map.of());
            instance.setRetryCount(1);
            instance.setMaxRetries(3);
            when(sagaRepository.findBySagaId("saga-1")).thenReturn(Optional.of(instance));
            when(sagaRepository.save(any(SagaInstance.class))).thenAnswer(inv -> inv.getArgument(0));

            boolean result = recoveryService.recoverSaga("saga-1");

            assertThat(result).isTrue();
            assertThat(instance.getCurrentState()).isEqualTo(SagaState.RETRYING.name());
            assertThat(instance.getRetryCount()).isEqualTo(2);
            verify(sagaRepository).save(instance);
        }

        @Test
        @DisplayName("should not recover terminal saga")
        void shouldNotRecoverTerminalSaga() {
            SagaInstance instance = SagaInstance.create("TransferSaga", SagaState.COMPLETED.name(), Map.of());
            when(sagaRepository.findBySagaId("saga-1")).thenReturn(Optional.of(instance));

            boolean result = recoveryService.recoverSaga("saga-1");

            assertThat(result).isFalse();
            verify(sagaRepository, never()).save(any());
        }

        @Test
        @DisplayName("should mark as FAILED when max retries exceeded")
        void shouldMarkAsFailedWhenMaxRetriesExceeded() {
            // TIMED_OUT is non-terminal, so recovery code will try to recover it
            SagaInstance instance = SagaInstance.create("TransferSaga", SagaState.TIMED_OUT.name(), Map.of());
            instance.setRetryCount(3);
            instance.setMaxRetries(3);
            when(sagaRepository.findBySagaId("saga-1")).thenReturn(Optional.of(instance));
            when(sagaRepository.save(any(SagaInstance.class))).thenAnswer(inv -> inv.getArgument(0));

            boolean result = recoveryService.recoverSaga("saga-1");

            assertThat(result).isFalse();
            assertThat(instance.getCurrentState()).isEqualTo(SagaState.FAILED.name());
            assertThat(instance.getErrorMessage()).contains("Max retries exceeded");
        }

        @Test
        @DisplayName("should return false when saga not found")
        void shouldReturnFalseWhenNotFound() {
            when(sagaRepository.findBySagaId("nonexistent")).thenReturn(Optional.empty());

            boolean result = recoveryService.recoverSaga("nonexistent");

            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("cancelSaga()")
    class CancelSagaTests {

        @Test
        @DisplayName("should cancel non-terminal saga")
        void shouldCancelNonTerminalSaga() {
            SagaInstance instance = SagaInstance.create("TransferSaga", SagaState.IN_PROGRESS.name(), Map.of());
            when(sagaRepository.findBySagaId("saga-1")).thenReturn(Optional.of(instance));
            when(sagaRepository.save(any(SagaInstance.class))).thenAnswer(inv -> inv.getArgument(0));

            boolean result = recoveryService.cancelSaga("saga-1", "User requested cancellation");

            assertThat(result).isTrue();
            assertThat(instance.getCurrentState()).isEqualTo(SagaState.CANCELLED.name());
            assertThat(instance.getErrorMessage()).isEqualTo("User requested cancellation");
            assertThat(instance.getCompletedAt()).isNotNull();
        }

        @Test
        @DisplayName("should not cancel terminal saga")
        void shouldNotCancelTerminalSaga() {
            SagaInstance instance = SagaInstance.create("TransferSaga", SagaState.COMPLETED.name(), Map.of());
            when(sagaRepository.findBySagaId("saga-1")).thenReturn(Optional.of(instance));

            boolean result = recoveryService.cancelSaga("saga-1", "Too late");

            assertThat(result).isFalse();
            assertThat(instance.getCurrentState()).isEqualTo(SagaState.COMPLETED.name());
        }

        @Test
        @DisplayName("should return false when saga not found")
        void shouldReturnFalseForMissingSaga() {
            when(sagaRepository.findBySagaId("missing")).thenReturn(Optional.empty());

            boolean result = recoveryService.cancelSaga("missing", "reason");

            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("pauseSaga()")
    class PauseSagaTests {

        @Test
        @DisplayName("should pause active saga")
        void shouldPauseActiveSaga() {
            SagaInstance instance = SagaInstance.create("TransferSaga", SagaState.IN_PROGRESS.name(), Map.of());
            when(sagaRepository.findBySagaId("saga-1")).thenReturn(Optional.of(instance));
            when(sagaRepository.save(any(SagaInstance.class))).thenAnswer(inv -> inv.getArgument(0));

            boolean result = recoveryService.pauseSaga("saga-1", "Manual review needed");

            assertThat(result).isTrue();
            assertThat(instance.getCurrentState()).isEqualTo(SagaState.PAUSED.name());
        }

        @Test
        @DisplayName("should not pause terminal saga")
        void shouldNotPauseTerminalSaga() {
            SagaInstance instance = SagaInstance.create("TransferSaga", SagaState.COMPLETED.name(), Map.of());
            when(sagaRepository.findBySagaId("saga-1")).thenReturn(Optional.of(instance));

            boolean result = recoveryService.pauseSaga("saga-1", "reason");

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("should not pause already paused saga")
        void shouldNotPauseAlreadyPaused() {
            SagaInstance instance = SagaInstance.create("TransferSaga", SagaState.PAUSED.name(), Map.of());
            when(sagaRepository.findBySagaId("saga-1")).thenReturn(Optional.of(instance));

            boolean result = recoveryService.pauseSaga("saga-1", "reason");

            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("resumeSaga()")
    class ResumeSagaTests {

        @Test
        @DisplayName("should resume paused saga")
        void shouldResumePausedSaga() {
            SagaInstance instance = SagaInstance.create("TransferSaga", SagaState.PAUSED.name(), Map.of());
            when(sagaRepository.findBySagaId("saga-1")).thenReturn(Optional.of(instance));
            when(sagaRepository.save(any(SagaInstance.class))).thenAnswer(inv -> inv.getArgument(0));

            boolean result = recoveryService.resumeSaga("saga-1");

            assertThat(result).isTrue();
            assertThat(instance.getCurrentState()).isEqualTo(SagaState.STARTED.name());
        }

        @Test
        @DisplayName("should not resume non-paused saga")
        void shouldNotResumeNonPausedSaga() {
            SagaInstance instance = SagaInstance.create("TransferSaga", SagaState.IN_PROGRESS.name(), Map.of());
            when(sagaRepository.findBySagaId("saga-1")).thenReturn(Optional.of(instance));

            boolean result = recoveryService.resumeSaga("saga-1");

            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("recoverRetryableSagas()")
    class RecoverRetryableSagasTests {

        @Test
        @DisplayName("should recover all retryable sagas within limits")
        void shouldRecoverRetryableSagas() {
            SagaInstance s1 = SagaInstance.create("Saga1", SagaState.TIMED_OUT.name(), Map.of());
            s1.setRetryCount(0);
            s1.setMaxRetries(3);
            SagaInstance s2 = SagaInstance.create("Saga2", SagaState.TIMED_OUT.name(), Map.of());
            s2.setRetryCount(1);
            s2.setMaxRetries(3);

            when(sagaRepository.findRetryableSagas(any(Instant.class))).thenReturn(List.of(s1, s2));
            when(sagaRepository.save(any(SagaInstance.class))).thenAnswer(inv -> inv.getArgument(0));

            int recovered = recoveryService.recoverRetryableSagas();

            assertThat(recovered).isEqualTo(2);
            verify(sagaRepository, times(2)).save(any(SagaInstance.class));
        }

        @Test
        @DisplayName("should return 0 when no retryable sagas")
        void shouldReturnZeroWhenNone() {
            when(sagaRepository.findRetryableSagas(any(Instant.class))).thenReturn(Collections.emptyList());

            int recovered = recoveryService.recoverRetryableSagas();

            assertThat(recovered).isZero();
        }
    }

    @Nested
    @DisplayName("scheduledRecovery()")
    class ScheduledRecoveryTests {

        @Test
        @DisplayName("should skip when compensation disabled")
        void shouldSkipWhenDisabled() {
            when(properties.isCompensationEnabled()).thenReturn(false);

            recoveryService.scheduledRecovery();

            verify(sagaRepository, never()).findRetryableSagas(any());
            verify(sagaRepository, never()).findStalledSagas(any());
        }

        @Test
        @DisplayName("should run recovery when compensation enabled")
        void shouldRunRecoveryWhenEnabled() {
            when(properties.isCompensationEnabled()).thenReturn(true);
            when(sagaRepository.findRetryableSagas(any(Instant.class))).thenReturn(Collections.emptyList());
            when(sagaRepository.findStalledSagas(any(Instant.class))).thenReturn(Collections.emptyList());

            recoveryService.scheduledRecovery();

            verify(sagaRepository).findRetryableSagas(any(Instant.class));
            verify(sagaRepository).findStalledSagas(any(Instant.class));
        }
    }
}
