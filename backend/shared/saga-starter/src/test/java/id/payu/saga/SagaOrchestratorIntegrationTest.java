package id.payu.saga;

import id.payu.saga.entity.SagaInstance;
import id.payu.saga.model.SagaResult;
import id.payu.saga.model.SagaState;
import id.payu.saga.model.SagaStep;
import id.payu.saga.model.StepResult;
import id.payu.saga.orchestrator.SagaOrchestrator;
import id.payu.saga.repository.SagaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link SagaOrchestrator}.
 * Tests saga orchestration with real H2 database persistence.
 */
@SpringBootTest(classes = TestConfig.class)
@ActiveProfiles("test")
@DisplayName("SagaOrchestrator Integration Tests")
class SagaOrchestratorIntegrationTest {

    @Autowired
    private SagaRepository sagaRepository;

    private TestSagaOrchestrator orchestrator;

    /**
     * Concrete test implementation of the abstract SagaOrchestrator.
     */
    static class TestSagaOrchestrator extends SagaOrchestrator<Map<String, Object>> {
        public TestSagaOrchestrator(SagaRepository sagaRepository) {
            super(sagaRepository);
        }

        public void init(String sagaType, List<SagaStep<Map<String, Object>>> steps) {
            initialize(sagaType, steps);
        }
    }

    @BeforeEach
    void setUp() {
        orchestrator = new TestSagaOrchestrator(sagaRepository);
    }

    @Nested
    @DisplayName("Saga Lifecycle - Start and State Verification")
    class SagaStartAndStateTests {

        @Test
        @DisplayName("should start a new saga and verify initial state")
        void shouldStartNewSagaAndVerifyState() {
            // Given
            List<SagaStep<Map<String, Object>>> steps = List.of(
                    SagaStep.of("step1", data -> StepResult.success(data))
            );
            orchestrator.init("TestSaga", steps);

            // When
            Map<String, Object> initialData = new HashMap<>();
            initialData.put("testKey", "testValue");
            SagaResult<Map<String, Object>> result = orchestrator.execute(initialData);

            // Then
            assertThat(result.getSagaId()).isNotNull();
            assertThat(result.getSagaType()).isEqualTo("TestSaga");

            // Verify persistence
            Optional<SagaInstance> savedInstance = sagaRepository.findBySagaId(result.getSagaId());
            assertThat(savedInstance).isPresent();
            assertThat(savedInstance.get().getSagaType()).isEqualTo("TestSaga");
            assertThat(savedInstance.get().getCurrentState()).isEqualTo("COMPLETED");
        }

        @Test
        @DisplayName("should execute saga with specific ID")
        void shouldExecuteWithSpecificId() {
            // Given
            String specificId = UUID.randomUUID().toString();
            List<SagaStep<Map<String, Object>>> steps = List.of(
                    SagaStep.of("step1", data -> StepResult.success(data))
            );
            orchestrator.init("TestSaga", steps);

            // When
            SagaResult<Map<String, Object>> result = orchestrator.executeWithId(specificId, new HashMap<>());

            // Then
            assertThat(result.getSagaId()).isEqualTo(specificId);

            // Verify persistence
            Optional<SagaInstance> savedInstance = sagaRepository.findBySagaId(specificId);
            assertThat(savedInstance).isPresent();
            assertThat(savedInstance.get().getSagaId()).isEqualTo(specificId);
        }
    }

    @Nested
    @DisplayName("Saga Step Execution")
    class SagaStepExecutionTests {

        @Test
        @DisplayName("should execute saga step successfully")
        void shouldExecuteStepSuccessfully() {
            // Given
            AtomicInteger stepExecutionCount = new AtomicInteger(0);
            List<SagaStep<Map<String, Object>>> steps = List.of(
                    SagaStep.of("processOrder", data -> {
                        stepExecutionCount.incrementAndGet();
                        data.put("orderProcessed", true);
                        data.put("orderId", "ORD-123");
                        return StepResult.success(data, "Order processed successfully");
                    })
            );
            orchestrator.init("OrderSaga", steps);

            // When
            SagaResult<Map<String, Object>> result = orchestrator.execute(new HashMap<>());

            // Then
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getFinalState()).isEqualTo(SagaState.COMPLETED);
            assertThat(stepExecutionCount.get()).isEqualTo(1);
            assertThat(result.getData()).containsEntry("orderProcessed", true);
            assertThat(result.getData()).containsEntry("orderId", "ORD-123");
        }

        @Test
        @DisplayName("should execute multiple steps in sequence")
        void shouldExecuteMultipleStepsInSequence() {
            // Given
            List<String> executionOrder = new ArrayList<>();
            List<SagaStep<Map<String, Object>>> steps = List.of(
                    SagaStep.of("validateOrder", data -> {
                        executionOrder.add("validateOrder");
                        data.put("validated", true);
                        return StepResult.success(data);
                    }),
                    SagaStep.of("reserveInventory", data -> {
                        executionOrder.add("reserveInventory");
                        data.put("inventoryReserved", true);
                        return StepResult.success(data);
                    }),
                    SagaStep.of("processPayment", data -> {
                        executionOrder.add("processPayment");
                        data.put("paymentProcessed", true);
                        return StepResult.success(data);
                    }),
                    SagaStep.of("confirmOrder", data -> {
                        executionOrder.add("confirmOrder");
                        data.put("orderConfirmed", true);
                        return StepResult.success(data);
                    })
            );
            orchestrator.init("OrderProcessingSaga", steps);

            // When
            SagaResult<Map<String, Object>> result = orchestrator.execute(new HashMap<>());

            // Then
            assertThat(result.isSuccess()).isTrue();
            assertThat(executionOrder).containsExactly(
                    "validateOrder", "reserveInventory", "processPayment", "confirmOrder"
            );
            assertThat(result.getData())
                    .containsEntry("validated", true)
                    .containsEntry("inventoryReserved", true)
                    .containsEntry("paymentProcessed", true)
                    .containsEntry("orderConfirmed", true);
        }

        @Test
        @DisplayName("should pass context data between steps")
        void shouldPassContextBetweenSteps() {
            // Given
            List<SagaStep<Map<String, Object>>> steps = List.of(
                    SagaStep.of("generateOrderId", data -> {
                        data.put("orderId", UUID.randomUUID().toString());
                        return StepResult.success(data);
                    }),
                    SagaStep.of("calculateTotal", data -> {
                        String orderId = (String) data.get("orderId");
                        assertThat(orderId).isNotNull();
                        data.put("totalAmount", 150.00);
                        return StepResult.success(data);
                    }),
                    SagaStep.of("applyDiscount", data -> {
                        Double total = (Double) data.get("totalAmount");
                        assertThat(total).isEqualTo(150.00);
                        data.put("finalAmount", total * 0.9); // 10% discount
                        return StepResult.success(data);
                    })
            );
            orchestrator.init("CheckoutSaga", steps);

            // When
            SagaResult<Map<String, Object>> result = orchestrator.execute(new HashMap<>());

            // Then
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData()).containsKey("orderId");
            assertThat(result.getData()).containsEntry("totalAmount", 150.00);
            assertThat(result.getData()).containsEntry("finalAmount", 135.00);
        }
    }

    @Nested
    @DisplayName("Saga Failure and Compensation")
    class SagaFailureAndCompensationTests {

        @Test
        @DisplayName("should handle saga step failure with compensation")
        void shouldHandleStepFailureWithCompensation() {
            // Given
            List<String> compensationOrder = new ArrayList<>();
            List<SagaStep<Map<String, Object>>> steps = List.of(
                    SagaStep.withCompensation("reserveInventory",
                            data -> {
                                data.put("inventoryReserved", true);
                                return StepResult.success(data);
                            },
                            data -> {
                                compensationOrder.add("reserveInventory");
                                data.put("inventoryReleased", true);
                                return StepResult.success(data);
                            }
                    ),
                    SagaStep.withCompensation("processPayment",
                            data -> {
                                data.put("paymentProcessed", true);
                                return StepResult.success(data);
                            },
                            data -> {
                                compensationOrder.add("processPayment");
                                data.put("paymentRefunded", true);
                                return StepResult.success(data);
                            }
                    ),
                    SagaStep.withCompensation("shipOrder",
                            data -> StepResult.failure(data, "Shipping service unavailable"),
                            data -> {
                                compensationOrder.add("shipOrder");
                                return StepResult.success(data);
                            }
                    )
            );
            orchestrator.init("OrderSaga", steps);

            // When
            SagaResult<Map<String, Object>> result = orchestrator.execute(new HashMap<>());

            // Then
            assertThat(result.isCompensated()).isTrue();
            assertThat(result.getFinalState()).isEqualTo(SagaState.COMPENSATED);
            assertThat(compensationOrder).containsExactly("processPayment", "reserveInventory");

            // Verify persistence
            Optional<SagaInstance> savedInstance = sagaRepository.findBySagaId(result.getSagaId());
            assertThat(savedInstance).isPresent();
            assertThat(savedInstance.get().getCurrentState()).isEqualTo("COMPENSATED");
        }

        @Test
        @DisplayName("should compensate steps in reverse order (LIFO)")
        void shouldCompensateInReverseOrder() {
            // Given
            List<String> executionOrder = new ArrayList<>();
            List<String> compensationOrder = new ArrayList<>();

            List<SagaStep<Map<String, Object>>> steps = List.of(
                    SagaStep.withCompensation("step1",
                            data -> {
                                executionOrder.add("step1");
                                return StepResult.success(data);
                            },
                            data -> {
                                compensationOrder.add("step1");
                                return StepResult.success(data);
                            }
                    ),
                    SagaStep.withCompensation("step2",
                            data -> {
                                executionOrder.add("step2");
                                return StepResult.success(data);
                            },
                            data -> {
                                compensationOrder.add("step2");
                                return StepResult.success(data);
                            }
                    ),
                    SagaStep.withCompensation("step3",
                            data -> {
                                executionOrder.add("step3");
                                return StepResult.success(data);
                            },
                            data -> {
                                compensationOrder.add("step3");
                                return StepResult.success(data);
                            }
                    ),
                    SagaStep.of("failingStep",
                            data -> StepResult.failure(data, "Intentional failure"))
            );
            orchestrator.init("TestSaga", steps);

            // When
            SagaResult<Map<String, Object>> result = orchestrator.execute(new HashMap<>());

            // Then
            assertThat(executionOrder).containsExactly("step1", "step2", "step3");
            assertThat(compensationOrder).containsExactly("step3", "step2", "step1");
        }

        @Test
        @DisplayName("should handle compensation failure")
        void shouldHandleCompensationFailure() {
            // Given
            List<SagaStep<Map<String, Object>>> steps = List.of(
                    SagaStep.withCompensation("step1",
                            data -> StepResult.success(data),
                            data -> StepResult.failure(data, "Compensation failed!")
                    ),
                    SagaStep.of("failingStep",
                            data -> StepResult.failure(data, "Step failed"))
            );
            orchestrator.init("TestSaga", steps);

            // When
            SagaResult<Map<String, Object>> result = orchestrator.execute(new HashMap<>());

            // Then
            assertThat(result.getFinalState()).isEqualTo(SagaState.COMPENSATION_FAILED);
            assertThat(result.getErrorMessage()).contains("Compensation failed");

            // Verify persistence
            Optional<SagaInstance> savedInstance = sagaRepository.findBySagaId(result.getSagaId());
            assertThat(savedInstance).isPresent();
            assertThat(savedInstance.get().getCurrentState()).isEqualTo("COMPENSATION_FAILED");
        }
    }

    @Nested
    @DisplayName("Saga Completion")
    class SagaCompletionTests {

        @Test
        @DisplayName("should complete saga successfully")
        void shouldCompleteSagaSuccessfully() {
            // Given
            List<SagaStep<Map<String, Object>>> steps = List.of(
                    SagaStep.of("step1", data -> {
                        data.put("step1Completed", true);
                        return StepResult.success(data);
                    }),
                    SagaStep.of("step2", data -> {
                        data.put("step2Completed", true);
                        return StepResult.success(data);
                    })
            );
            orchestrator.init("CompleteSaga", steps);

            // When
            Map<String, Object> initialData = new HashMap<>();
            initialData.put("initial", true);
            SagaResult<Map<String, Object>> result = orchestrator.execute(initialData);

            // Then
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getFinalState()).isEqualTo(SagaState.COMPLETED);
            assertThat(result.getData())
                    .containsEntry("initial", true)
                    .containsEntry("step1Completed", true)
                    .containsEntry("step2Completed", true);

            // Verify persistence
            Optional<SagaInstance> savedInstance = sagaRepository.findBySagaId(result.getSagaId());
            assertThat(savedInstance).isPresent();
            assertThat(savedInstance.get().getCurrentState()).isEqualTo("COMPLETED");
            assertThat(savedInstance.get().getCompletedAt()).isNotNull();
            assertThat(savedInstance.get().getCompletedSteps()).containsExactly("step1", "step2");
        }

        @Test
        @DisplayName("should mark saga as completed with metadata")
        void shouldCompleteWithMetadata() {
            // Given
            List<SagaStep<Map<String, Object>>> steps = List.of(
                    SagaStep.of("step1", data -> {
                        Map<String, Object> metadata = new HashMap<>();
                        metadata.put("processingTime", 100);
                        metadata.put("processorId", "PROC-001");
                        return StepResult.success(data, metadata);
                    })
            );
            orchestrator.init("MetadataSaga", steps);

            // When
            SagaResult<Map<String, Object>> result = orchestrator.execute(new HashMap<>());

            // Then
            assertThat(result.isSuccess()).isTrue();

            // Verify persistence includes step context
            Optional<SagaInstance> savedInstance = sagaRepository.findBySagaId(result.getSagaId());
            assertThat(savedInstance).isPresent();
            assertThat(savedInstance.get().getStepContext()).containsKey("step1");
        }
    }

    @Nested
    @DisplayName("Saga Status Query")
    class SagaStatusQueryTests {

        @Test
        @DisplayName("should query saga status by ID")
        void shouldQuerySagaStatusById() {
            // Given
            List<SagaStep<Map<String, Object>>> steps = List.of(
                    SagaStep.of("step1", data -> StepResult.success(data))
            );
            orchestrator.init("QuerySaga", steps);
            SagaResult<Map<String, Object>> result = orchestrator.execute(new HashMap<>());

            // When
            Optional<SagaInstance> instance = sagaRepository.findBySagaId(result.getSagaId());

            // Then
            assertThat(instance).isPresent();
            assertThat(instance.get().getSagaType()).isEqualTo("QuerySaga");
            assertThat(instance.get().getCurrentState()).isEqualTo("COMPLETED");
            assertThat(instance.get().getStartedAt()).isNotNull();
            assertThat(instance.get().getCompletedAt()).isNotNull();
        }

        @Test
        @DisplayName("should find sagas by type")
        void shouldFindSagasByType() {
            // Given
            List<SagaStep<Map<String, Object>>> steps = List.of(
                    SagaStep.of("step1", data -> StepResult.success(data))
            );
            orchestrator.init("TypeA", steps);
            orchestrator.execute(new HashMap<>());
            orchestrator.execute(new HashMap<>());

            // When
            List<SagaInstance> typeASagas = sagaRepository.findBySagaType("TypeA");

            // Then
            assertThat(typeASagas).hasSize(2);
            assertThat(typeASagas).allMatch(s -> s.getSagaType().equals("TypeA"));
        }

        @Test
        @DisplayName("should find sagas by state")
        void shouldFindSagasByState() {
            // Given - Create a completed saga
            List<SagaStep<Map<String, Object>>> successSteps = List.of(
                    SagaStep.of("step1", data -> StepResult.success(data))
            );
            orchestrator.init("StatusSaga", successSteps);
            SagaResult<Map<String, Object>> successResult = orchestrator.execute(new HashMap<>());

            // When
            List<SagaInstance> completedSagas = sagaRepository.findByCurrentState("COMPLETED");

            // Then
            assertThat(completedSagas).isNotEmpty();
            assertThat(completedSagas.stream()
                    .map(SagaInstance::getSagaId))
                    .contains(successResult.getSagaId());
        }

        @Test
        @DisplayName("should check if saga exists")
        void shouldCheckIfSagaExists() {
            // Given
            List<SagaStep<Map<String, Object>>> steps = List.of(
                    SagaStep.of("step1", data -> StepResult.success(data))
            );
            orchestrator.init("ExistenceSaga", steps);
            SagaResult<Map<String, Object>> result = orchestrator.execute(new HashMap<>());

            // When & Then
            assertThat(sagaRepository.existsBySagaId(result.getSagaId())).isTrue();
            assertThat(sagaRepository.existsBySagaId("non-existent-id")).isFalse();
        }
    }

    @Nested
    @DisplayName("Advanced Saga Scenarios")
    class AdvancedSagaScenariosTests {

        @Test
        @DisplayName("should skip step when precondition is not met")
        void shouldSkipStepWhenPreconditionNotMet() {
            // Given
            AtomicBoolean skippedStepExecuted = new AtomicBoolean(false);
            List<SagaStep<Map<String, Object>>> steps = List.of(
                    SagaStep.of("checkEligibility", data -> {
                        data.put("eligible", false);
                        return StepResult.success(data);
                    }),
                    SagaStep.<Map<String, Object>>builder()
                            .name("applyDiscount")
                            .action(data -> {
                                skippedStepExecuted.set(true);
                                data.put("discountApplied", true);
                                return StepResult.success(data);
                            })
                            .precondition(data -> Boolean.TRUE.equals(data.get("eligible")))
                            .build(),
                    SagaStep.of("finalize", data -> {
                        data.put("finalized", true);
                        return StepResult.success(data);
                    })
            );
            orchestrator.init("ConditionalSaga", steps);

            // When
            SagaResult<Map<String, Object>> result = orchestrator.execute(new HashMap<>());

            // Then
            assertThat(result.isSuccess()).isTrue();
            assertThat(skippedStepExecuted.get()).isFalse();
            assertThat(result.getData()).containsEntry("eligible", false);
            assertThat(result.getData()).containsEntry("finalized", true);
            assertThat(result.getData()).doesNotContainKey("discountApplied");
        }

        @Test
        @DisplayName("should continue on non-critical failure")
        void shouldContinueOnNonCriticalFailure() {
            // Given
            AtomicInteger step3ExecutionCount = new AtomicInteger(0);
            List<SagaStep<Map<String, Object>>> steps = List.of(
                    SagaStep.of("step1", data -> StepResult.success(data)),
                    SagaStep.<Map<String, Object>>builder()
                            .name("optionalStep")
                            .action(data -> StepResult.failure(data, "Non-critical error"))
                            .continueOnFailure(true)
                            .build(),
                    SagaStep.of("step3", data -> {
                        step3ExecutionCount.incrementAndGet();
                        return StepResult.success(data);
                    })
            );
            orchestrator.init("ResilientSaga", steps);

            // When
            SagaResult<Map<String, Object>> result = orchestrator.execute(new HashMap<>());

            // Then
            assertThat(result.isSuccess()).isTrue();
            assertThat(step3ExecutionCount.get()).isEqualTo(1);
        }

        @Test
        @DisplayName("should retry failed step before giving up")
        void shouldRetryFailedStep() {
            // Given
            AtomicInteger attemptCount = new AtomicInteger(0);
            List<SagaStep<Map<String, Object>>> steps = List.of(
                    SagaStep.<Map<String, Object>>builder()
                            .name("flakyStep")
                            .action(data -> {
                                int attempt = attemptCount.incrementAndGet();
                                if (attempt < 3) {
                                    throw new RuntimeException("Transient error #" + attempt);
                                }
                                data.put("successAfterRetries", true);
                                return StepResult.success(data);
                            })
                            .maxRetries(3)
                            .retryDelay(Duration.ofMillis(10))
                            .build()
            );
            orchestrator.init("RetrySaga", steps);

            // When
            SagaResult<Map<String, Object>> result = orchestrator.execute(new HashMap<>());

            // Then
            assertThat(result.isSuccess()).isTrue();
            assertThat(attemptCount.get()).isEqualTo(3);
            assertThat(result.getData()).containsEntry("successAfterRetries", true);
        }

        @Test
        @DisplayName("should execute saga asynchronously")
        void shouldExecuteSagaAsync() throws Exception {
            // Given
            List<SagaStep<Map<String, Object>>> steps = List.of(
                    SagaStep.of("step1", data -> {
                        data.put("async", true);
                        return StepResult.success(data);
                    })
            );
            orchestrator.init("AsyncSaga", steps);

            // When
            CompletableFuture<SagaResult<Map<String, Object>>> future = orchestrator.executeAsync(new HashMap<>());
            SagaResult<Map<String, Object>> result = future.get(5, TimeUnit.SECONDS);

            // Then
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData()).containsEntry("async", true);
        }

        @Test
        @DisplayName("should handle saga with no steps")
        void shouldHandleEmptySaga() {
            // Given
            List<SagaStep<Map<String, Object>>> steps = List.of();
            orchestrator.init("EmptySaga", steps);

            // When
            SagaResult<Map<String, Object>> result = orchestrator.execute(new HashMap<>());

            // Then
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getFinalState()).isEqualTo(SagaState.COMPLETED);
        }

        @Test
        @DisplayName("should handle complex saga with mixed outcomes")
        void shouldHandleComplexSaga() {
            // Given
            List<String> executionLog = new ArrayList<>();

            List<SagaStep<Map<String, Object>>> steps = List.of(
                    SagaStep.of("validate", data -> {
                        executionLog.add("validate");
                        data.put("valid", true);
                        return StepResult.success(data);
                    }),
                    SagaStep.withCompensation("createOrder",
                            data -> {
                                executionLog.add("createOrder");
                                data.put("orderId", "ORD-001");
                                return StepResult.success(data);
                            },
                            data -> {
                                executionLog.add("compensate-createOrder");
                                return StepResult.success(data);
                            }
                    ),
                    SagaStep.<Map<String, Object>>builder()
                            .name("notifyOptional")
                            .action(data -> {
                                executionLog.add("notifyOptional");
                                return StepResult.failure(data, "Notification failed");
                            })
                            .continueOnFailure(true)
                            .build(),
                    SagaStep.withCompensation("chargePayment",
                            data -> {
                                executionLog.add("chargePayment");
                                data.put("charged", true);
                                return StepResult.success(data);
                            },
                            data -> {
                                executionLog.add("compensate-chargePayment");
                                data.put("refunded", true);
                                return StepResult.success(data);
                            }
                    ),
                    SagaStep.of("finalize",
                            data -> StepResult.failure(data, "Finalization failed"))
            );
            orchestrator.init("ComplexSaga", steps);

            // When
            SagaResult<Map<String, Object>> result = orchestrator.execute(new HashMap<>());

            // Then
            assertThat(result.isCompensated()).isTrue();
            assertThat(executionLog).contains("validate", "createOrder", "notifyOptional", "chargePayment");
            assertThat(executionLog).contains("compensate-chargePayment", "compensate-createOrder");

            // Verify order: compensation should be in reverse (LIFO)
            int chargePaymentIndex = executionLog.indexOf("chargePayment");
            int createOrderIndex = executionLog.indexOf("createOrder");
            int compensateChargeIndex = executionLog.indexOf("compensate-chargePayment");
            int compensateCreateIndex = executionLog.indexOf("compensate-createOrder");

            assertThat(compensateChargeIndex).isLessThan(compensateCreateIndex);
            assertThat(chargePaymentIndex).isGreaterThan(createOrderIndex);
        }
    }

    @Nested
    @DisplayName("Saga Persistence Verification")
    class SagaPersistenceTests {

        @Test
        @DisplayName("should persist saga state transitions")
        void shouldPersistStateTransitions() {
            // Given
            List<SagaStep<Map<String, Object>>> steps = List.of(
                    SagaStep.of("step1", data -> StepResult.success(data))
            );
            orchestrator.init("TransitionSaga", steps);

            // When
            SagaResult<Map<String, Object>> result = orchestrator.execute(new HashMap<>());

            // Then
            Optional<SagaInstance> savedInstance = sagaRepository.findBySagaId(result.getSagaId());
            assertThat(savedInstance).isPresent();

            SagaInstance instance = savedInstance.get();
            assertThat(instance.getCurrentState()).isEqualTo("COMPLETED");
            assertThat(instance.getPreviousState()).isNotNull();
            assertThat(instance.getStartedAt()).isNotNull();
            assertThat(instance.getCompletedAt()).isNotNull();
            assertThat(instance.getLastUpdatedAt()).isNotNull();
            assertThat(instance.getVersion()).isGreaterThanOrEqualTo(0);
        }

        @Test
        @DisplayName("should persist step context")
        void shouldPersistStepContext() {
            // Given
            List<SagaStep<Map<String, Object>>> steps = List.of(
                    SagaStep.of("step1", data -> {
                        Map<String, Object> metadata = new HashMap<>();
                        metadata.put("step1Data", "value1");
                        return StepResult.success(data, metadata);
                    }),
                    SagaStep.of("step2", data -> {
                        Map<String, Object> metadata = new HashMap<>();
                        metadata.put("step2Data", "value2");
                        return StepResult.success(data, metadata);
                    })
            );
            orchestrator.init("ContextSaga", steps);

            // When
            SagaResult<Map<String, Object>> result = orchestrator.execute(new HashMap<>());

            // Then
            Optional<SagaInstance> savedInstance = sagaRepository.findBySagaId(result.getSagaId());
            assertThat(savedInstance).isPresent();
            assertThat(savedInstance.get().getStepContext()).containsKeys("step1", "step2");
            assertThat(savedInstance.get().getCompletedSteps()).containsExactly("step1", "step2");
        }

        @Test
        @DisplayName("should persist error information on failure")
        void shouldPersistErrorInformation() {
            // Given
            List<SagaStep<Map<String, Object>>> steps = List.of(
                    SagaStep.of("step1", data -> StepResult.success(data)),
                    SagaStep.of("failingStep", data -> StepResult.failure(data, "Specific error message"))
            );
            orchestrator.init("ErrorSaga", steps);

            // When
            SagaResult<Map<String, Object>> result = orchestrator.execute(new HashMap<>());

            // Then - Since step1 succeeded, compensation is triggered
            assertThat(result.isCompensated()).isTrue();

            Optional<SagaInstance> savedInstance = sagaRepository.findBySagaId(result.getSagaId());
            assertThat(savedInstance).isPresent();
            assertThat(savedInstance.get().getCurrentState()).isEqualTo("COMPENSATED");
            assertThat(savedInstance.get().getErrorStep()).isEqualTo("failingStep");
            assertThat(savedInstance.get().getErrorMessage()).contains("Specific error message");
        }
    }
}
