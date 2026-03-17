package id.payu.saga.orchestrator;

import id.payu.saga.entity.SagaInstance;
import id.payu.saga.model.*;
import id.payu.saga.repository.SagaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link SagaOrchestrator}.
 * Tests saga execution, compensation (LIFO), retry logic, and step preconditions.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SagaOrchestrator")
class SagaOrchestratorTest {

    @Mock
    private SagaRepository sagaRepository;

    @Mock
    private org.springframework.transaction.PlatformTransactionManager transactionManager;

    @Captor
    private ArgumentCaptor<SagaInstance> instanceCaptor;

    private TestSagaOrchestrator orchestrator;
    private final ScheduledExecutorService retryScheduler = Executors.newScheduledThreadPool(2);

    /**
     * Concrete test implementation of the abstract SagaOrchestrator.
     */
    static class TestSagaOrchestrator extends SagaOrchestrator<Map<String, Object>> {
        public TestSagaOrchestrator(SagaRepository sagaRepository,
                                    org.springframework.core.task.TaskExecutor taskExecutor,
                                    ScheduledExecutorService retryScheduler,
                                    org.springframework.transaction.PlatformTransactionManager transactionManager) {
            super(sagaRepository, taskExecutor, retryScheduler, transactionManager);
        }

        public void init(String sagaType, List<SagaStep<Map<String, Object>>> steps) {
            initialize(sagaType, steps);
        }
    }

    @BeforeEach
    void setUp() {
        org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor taskExecutor =
            new org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor();
        taskExecutor.setCorePoolSize(2);
        taskExecutor.setMaxPoolSize(4);
        taskExecutor.setQueueCapacity(50);
        taskExecutor.setThreadNamePrefix("saga-test-");
        taskExecutor.initialize();
        orchestrator = new TestSagaOrchestrator(sagaRepository, taskExecutor, retryScheduler, transactionManager);

        // Mock save to return the same instance
        lenient().when(sagaRepository.save(any(SagaInstance.class))).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(sagaRepository.findBySagaId(anyString())).thenAnswer(inv -> {
            // Return the last saved instance
            return Optional.of(SagaInstance.create("TestSaga", SagaState.STARTED.name(), Map.of()));
        });
    }

    @Nested
    @DisplayName("Successful execution")
    class SuccessfulExecutionTests {

        @Test
        @DisplayName("should execute all steps and return SUCCESS")
        void shouldExecuteAllSteps() {
            AtomicInteger executionOrder = new AtomicInteger(0);
            List<Integer> order = new ArrayList<>();

            List<SagaStep<Map<String, Object>>> steps = List.of(
                    SagaStep.of("step1", data -> {
                        order.add(executionOrder.incrementAndGet());
                        return StepResult.success(data);
                    }),
                    SagaStep.of("step2", data -> {
                        order.add(executionOrder.incrementAndGet());
                        return StepResult.success(data);
                    }),
                    SagaStep.of("step3", data -> {
                        order.add(executionOrder.incrementAndGet());
                        return StepResult.success(data);
                    })
            );

            orchestrator.init("TestSaga", steps);
            SagaResult<Map<String, Object>> result = orchestrator.execute(new HashMap<>());

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getFinalState()).isEqualTo(SagaState.COMPLETED);
            assertThat(order).containsExactly(1, 2, 3);
        }

        @Test
        @DisplayName("should pass updated context between steps")
        void shouldPassContextBetweenSteps() {
            List<SagaStep<Map<String, Object>>> steps = List.of(
                    SagaStep.of("addAge", data -> {
                        data.put("age", 25);
                        return StepResult.success(data);
                    }),
                    SagaStep.of("addName", data -> {
                        data.put("name", "John");
                        return StepResult.success(data);
                    })
            );

            orchestrator.init("TestSaga", steps);
            SagaResult<Map<String, Object>> result = orchestrator.execute(new HashMap<>());

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData()).containsEntry("age", 25).containsEntry("name", "John");
        }
    }

    @Nested
    @DisplayName("Step failure and compensation")
    class FailureAndCompensationTests {

        @Test
        @DisplayName("should trigger compensation in LIFO order on failure")
        void shouldCompensateInLIFOOrder() {
            List<String> compensationOrder = new ArrayList<>();

            // Capture the saga instance
            when(sagaRepository.findBySagaId(anyString())).thenAnswer(inv -> {
                SagaInstance instance = SagaInstance.create("TestSaga", SagaState.STARTED.name(), Map.of());
                instance.recordStepCompletion("step1", Map.of());
                instance.recordStepCompletion("step2", Map.of());
                return Optional.of(instance);
            });

            List<SagaStep<Map<String, Object>>> steps = List.of(
                    SagaStep.withCompensation("step1",
                            data -> StepResult.success(data),
                            data -> { compensationOrder.add("step1"); return StepResult.success(data); }),
                    SagaStep.withCompensation("step2",
                            data -> StepResult.success(data),
                            data -> { compensationOrder.add("step2"); return StepResult.success(data); }),
                    SagaStep.withCompensation("step3",
                            data -> StepResult.failure(data, "Step 3 failed"),
                            data -> { compensationOrder.add("step3"); return StepResult.success(data); })
            );

            orchestrator.init("TestSaga", steps);
            SagaResult<Map<String, Object>> result = orchestrator.execute(new HashMap<>());

            // Step3 failed, so steps 2 and 1 should be compensated in reverse order
            assertThat(result.getFinalState()).isEqualTo(SagaState.COMPENSATED);
            assertThat(compensationOrder).containsExactly("step2", "step1");
        }

        @Test
        @DisplayName("should return FAILED when step fails without compensation trigger")
        void shouldReturnFailedWithoutCompensation() {
            List<SagaStep<Map<String, Object>>> steps = List.of(
                    SagaStep.of("step1", data -> StepResult.success(data)),
                    SagaStep.<Map<String, Object>>builder()
                            .name("step2")
                            .action(data -> StepResult.<Map<String, Object>>builder()
                                    .success(false)
                                    .context(data)
                                    .message("Validation failed")
                                    .triggerCompensation(false)
                                    .build())
                            .build()
            );

            orchestrator.init("TestSaga", steps);
            SagaResult<Map<String, Object>> result = orchestrator.execute(new HashMap<>());

            assertThat(result.getFinalState()).isEqualTo(SagaState.FAILED);
            assertThat(result.getErrorMessage()).isEqualTo("Validation failed");
            assertThat(result.getErrorStep()).isEqualTo("step2");
        }

        @Test
        @DisplayName("should return COMPENSATION_FAILED when compensation step fails")
        void shouldReturnCompensationFailed() {
            when(sagaRepository.findBySagaId(anyString())).thenAnswer(inv -> {
                SagaInstance instance = SagaInstance.create("TestSaga", SagaState.STARTED.name(), Map.of());
                instance.recordStepCompletion("step1", Map.of());
                return Optional.of(instance);
            });

            List<SagaStep<Map<String, Object>>> steps = List.of(
                    SagaStep.withCompensation("step1",
                            data -> StepResult.success(data),
                            data -> StepResult.failure(data, "Compensation failed!")),
                    SagaStep.withCompensation("step2",
                            data -> StepResult.failure(data, "Step 2 fails"),
                            data -> StepResult.success(data))
            );

            orchestrator.init("TestSaga", steps);
            SagaResult<Map<String, Object>> result = orchestrator.execute(new HashMap<>());

            assertThat(result.getFinalState()).isEqualTo(SagaState.COMPENSATION_FAILED);
        }
    }

    @Nested
    @DisplayName("continueOnFailure behavior")
    class ContinueOnFailureTests {

        @Test
        @DisplayName("should continue to next step when continueOnFailure=true")
        void shouldContinueOnNonCriticalFailure() {
            AtomicInteger step3Executed = new AtomicInteger(0);

            List<SagaStep<Map<String, Object>>> steps = List.of(
                    SagaStep.of("step1", data -> StepResult.success(data)),
                    SagaStep.<Map<String, Object>>builder()
                            .name("optionalStep")
                            .action(data -> StepResult.failure(data, "non-critical failure"))
                            .continueOnFailure(true)
                            .build(),
                    SagaStep.of("step3", data -> {
                        step3Executed.incrementAndGet();
                        return StepResult.success(data);
                    })
            );

            orchestrator.init("TestSaga", steps);
            SagaResult<Map<String, Object>> result = orchestrator.execute(new HashMap<>());

            assertThat(result.isSuccess()).isTrue();
            assertThat(step3Executed.get()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("Step preconditions")
    class PreconditionTests {

        @Test
        @DisplayName("should skip step when precondition is not met")
        void shouldSkipStepWhenPreconditionFails() {
            AtomicInteger skippedStepExecCount = new AtomicInteger(0);

            List<SagaStep<Map<String, Object>>> steps = List.of(
                    SagaStep.of("step1", data -> {
                        data.put("skipNext", true);
                        return StepResult.success(data);
                    }),
                    SagaStep.<Map<String, Object>>builder()
                            .name("conditionalStep")
                            .action(data -> {
                                skippedStepExecCount.incrementAndGet();
                                return StepResult.success(data);
                            })
                            .precondition(data -> !Boolean.TRUE.equals(data.get("skipNext")))
                            .build(),
                    SagaStep.of("step3", data -> StepResult.success(data))
            );

            orchestrator.init("TestSaga", steps);
            SagaResult<Map<String, Object>> result = orchestrator.execute(new HashMap<>());

            assertThat(result.isSuccess()).isTrue();
            assertThat(skippedStepExecCount.get()).isZero();
        }
    }

    @Nested
    @DisplayName("Retry logic (executeStepWithRetry)")
    class RetryLogicTests {

        @Test
        @DisplayName("should retry step on exception up to maxRetries")
        void shouldRetryOnException() {
            AtomicInteger attempts = new AtomicInteger(0);

            List<SagaStep<Map<String, Object>>> steps = List.of(
                    SagaStep.<Map<String, Object>>builder()
                            .name("flakyStep")
                            .action(data -> {
                                if (attempts.incrementAndGet() <= 2) {
                                    throw new RuntimeException("Transient error #" + attempts.get());
                                }
                                return StepResult.success(data);
                            })
                            .maxRetries(2) // Allow 2 retries  
                            .retryDelay(java.time.Duration.ofMillis(10)) // Keep test fast
                            .build()
            );

            orchestrator.init("TestSaga", steps);
            SagaResult<Map<String, Object>> result = orchestrator.execute(new HashMap<>());

            assertThat(result.isSuccess()).isTrue();
            assertThat(attempts.get()).isEqualTo(3); // 1 initial + 2 retries
        }

        @Test
        @DisplayName("should fail after exhausting retries")
        void shouldFailAfterExhaustingRetries() {
            AtomicInteger attempts = new AtomicInteger(0);

            List<SagaStep<Map<String, Object>>> steps = List.of(
                    SagaStep.<Map<String, Object>>builder()
                            .name("failingStep")
                            .action(data -> {
                                attempts.incrementAndGet();
                                throw new RuntimeException("Always fails");
                            })
                            .maxRetries(1)
                            .retryDelay(java.time.Duration.ofMillis(10))
                            .build()
            );

            orchestrator.init("TestSaga", steps);
            SagaResult<Map<String, Object>> result = orchestrator.execute(new HashMap<>());

            assertThat(result.isFailure()).isTrue();
            assertThat(attempts.get()).isEqualTo(2); // 1 initial + 1 retry
        }
    }

    @Nested
    @DisplayName("Exception handling")
    class ExceptionHandlingTests {

        @Test
        @DisplayName("should handle unexpected runtime exception in step")
        void shouldHandleUnexpectedException() {
            List<SagaStep<Map<String, Object>>> steps = List.of(
                    SagaStep.of("explodingStep", data -> {
                        throw new RuntimeException("Unexpected NPE");
                    })
            );

            orchestrator.init("TestSaga", steps);
            // Should not throw — should be caught and return a failed result
            SagaResult<Map<String, Object>> result = orchestrator.execute(new HashMap<>());

            assertThat(result.isFailure() || result.isCompensated()).isTrue();
        }
    }

    @Nested
    @DisplayName("Saga persistence")
    class PersistenceTests {

        @Test
        @DisplayName("should save saga instance on start")
        void shouldSaveOnStart() {
            List<SagaStep<Map<String, Object>>> steps = List.of(
                    SagaStep.of("step1", data -> StepResult.success(data))
            );

            orchestrator.init("TestSaga", steps);
            orchestrator.execute(new HashMap<>());

            // At minimum: initial save, step transition save, completion save
            verify(sagaRepository, atLeast(2)).save(any(SagaInstance.class));
        }

        @Test
        @DisplayName("should persist COMPLETED state on success")
        void shouldPersistCompletedState() {
            List<SagaStep<Map<String, Object>>> steps = List.of(
                    SagaStep.of("step1", data -> StepResult.success(data))
            );

            orchestrator.init("TestSaga", steps);
            orchestrator.execute(new HashMap<>());

            verify(sagaRepository, atLeast(1)).save(instanceCaptor.capture());
            List<SagaInstance> savedInstances = instanceCaptor.getAllValues();

            // The last save should have COMPLETED state
            SagaInstance lastSaved = savedInstances.get(savedInstances.size() - 1);
            assertThat(lastSaved.getCurrentState()).isEqualTo("COMPLETED");
        }
    }
}
