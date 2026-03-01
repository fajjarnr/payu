package id.payu.saga.orchestrator;

import id.payu.saga.entity.SagaInstance;
import id.payu.saga.model.SagaResult;
import id.payu.saga.model.SagaState;
import id.payu.saga.model.SagaStep;
import id.payu.saga.model.StepResult;
import id.payu.saga.repository.SagaRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Abstract base class for saga orchestrators.
 * Provides the core orchestration logic for executing saga steps with compensation support.
 *
 * @param <T> The saga context/data type
 */
@Slf4j
public abstract class SagaOrchestrator<T> {

    protected final SagaRepository sagaRepository;
    protected final TaskExecutor sagaTaskExecutor;
    protected final ScheduledExecutorService sagaRetryScheduler;

    @PersistenceContext
    private EntityManager entityManager;

    private final List<SagaStep<T>> steps = new ArrayList<>();
    private String sagaType;

    /**
     * Constructor with Spring-managed executors.
     *
     * @param sagaRepository the saga repository
     * @param sagaTaskExecutor the Spring-managed task executor for async operations
     * @param sagaRetryScheduler the Spring-managed scheduled executor for retries
     */
    protected SagaOrchestrator(SagaRepository sagaRepository,
                               @Qualifier("sagaTaskExecutor") TaskExecutor sagaTaskExecutor,
                               @Qualifier("sagaRetryScheduler") ScheduledExecutorService sagaRetryScheduler) {
        this.sagaRepository = sagaRepository;
        this.sagaTaskExecutor = sagaTaskExecutor;
        this.sagaRetryScheduler = sagaRetryScheduler;
    }

    /**
     * Initialize the orchestrator with saga type and steps.
     */
    protected void initialize(String sagaType, List<SagaStep<T>> steps) {
        this.sagaType = sagaType;
        this.steps.addAll(steps);
        log.info("Initialized saga orchestrator for type: {} with {} steps", sagaType, steps.size());
    }

    /**
     * Execute the saga with the given initial data.
     *
     * @param initialData The initial saga data
     * @return The saga execution result
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public SagaResult<T> execute(T initialData) {
        String sagaId = UUID.randomUUID().toString();
        return executeWithId(sagaId, initialData);
    }

    /**
     * Execute the saga with a specific ID.
     *
     * @param sagaId The saga instance ID
     * @param initialData The initial saga data
     * @return The saga execution result
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public SagaResult<T> executeWithId(String sagaId, T initialData) {
        log.info("Starting saga execution: {} - type: {}", sagaId, sagaType);

        // Create and persist saga instance
        SagaInstance instance = createSagaInstance(sagaId, initialData);
        instance = sagaRepository.save(instance);

        T currentData = initialData;
        List<String> executedSteps = new ArrayList<>();

        try {
            // Execute each step in order
            for (SagaStep<T> step : steps) {
                log.debug("Executing step: {} for saga: {}", step.getName(), sagaId);

                // Check preconditions
                if (!step.canExecute(currentData)) {
                    log.warn("Precondition not met for step: {} in saga: {}", step.getName(), sagaId);
                    continue;
                }

                // Update state - re-fetch to get current version
                instance = sagaRepository.findById(sagaId).orElse(instance);
                instance.transitionTo("EXECUTING_" + step.getName());
                instance = sagaRepository.save(instance);

                // Execute step with retry logic
                StepResult<T> result = executeStepWithRetry(step, currentData);

                if (result.isSuccess()) {
                    // Record successful step - re-fetch to get current version
                    instance = sagaRepository.findById(sagaId).orElse(instance);
                    currentData = result.getContext();
                    executedSteps.add(step.getName());
                    instance.recordStepCompletion(step.getName(), result.getMetadata());
                    instance = sagaRepository.save(instance);
                    log.debug("Step completed successfully: {} for saga: {}", step.getName(), sagaId);
                } else {
                    // Step failed
                    log.error("Step failed: {} for saga: {} - {}", step.getName(), sagaId, result.getMessage());

                    if (step.isContinueOnFailure()) {
                        // Non-critical failure, continue
                        log.warn("Continuing after non-critical failure in step: {}", step.getName());
                        continue;
                    }

                    // Record failure and trigger compensation - re-fetch to get current version
                    instance = sagaRepository.findById(sagaId).orElse(instance);
                    instance.recordError(step.getName(), result.getMessage());
                    instance.transitionTo(SagaState.FAILED.name());
                    instance = sagaRepository.save(instance);

                    if (result.isTriggerCompensation() && !executedSteps.isEmpty()) {
                        return compensate(sagaId, executedSteps, currentData, result.getError());
                    }

                    return SagaResult.failure(sagaId, sagaType, result.getMessage(), step.getName());
                }
            }

            // All steps completed successfully - re-fetch to get current version
            instance = sagaRepository.findById(sagaId).orElse(instance);
            instance.transitionTo(SagaState.COMPLETED.name());
            instance.complete();
            sagaRepository.save(instance);

            log.info("Saga completed successfully: {}", sagaId);
            return SagaResult.success(sagaId, sagaType, currentData);

        } catch (Exception e) {
            log.error("Unexpected error in saga: {}", sagaId, e);
            instance = sagaRepository.findById(sagaId).orElse(instance);
            instance.recordError("UNKNOWN", e.getMessage());
            instance.transitionTo(SagaState.FAILED.name());
            sagaRepository.save(instance);

            return compensate(sagaId, executedSteps, currentData, e);
        }
    }

    /**
     * Execute saga asynchronously.
     */
    public CompletableFuture<SagaResult<T>> executeAsync(T initialData) {
        return CompletableFuture.supplyAsync(() -> execute(initialData), sagaTaskExecutor::execute);
    }

    /**
     * Execute saga asynchronously with specific ID.
     */
    public CompletableFuture<SagaResult<T>> executeAsyncWithId(String sagaId, T initialData) {
        return CompletableFuture.supplyAsync(() -> executeWithId(sagaId, initialData), sagaTaskExecutor::execute);
    }

    /**
     * Compensate completed steps in reverse order.
     */
    protected SagaResult<T> compensate(String sagaId, List<String> executedSteps,
                                        T currentData, Throwable error) {
        log.info("Starting compensation for saga: {} - executed steps: {}",
                sagaId, executedSteps);

        Optional<SagaInstance> instanceOpt = sagaRepository.findBySagaId(sagaId);
        if (instanceOpt.isEmpty()) {
            log.error("Saga instance not found for compensation: {}", sagaId);
            return SagaResult.failure(sagaId, sagaType, "Saga instance not found", "COMPENSATION");
        }

        SagaInstance instance = instanceOpt.get();
        instance.transitionTo(SagaState.COMPENSATING.name());
        instance = sagaRepository.save(instance);

        // Reverse the executed steps for compensation
        List<String> stepsToCompensate = new ArrayList<>(executedSteps);
        Collections.reverse(stepsToCompensate);

        List<String> compensatedSteps = new ArrayList<>();
        Throwable compensationError = null;

        for (String stepName : stepsToCompensate) {
            Optional<SagaStep<T>> stepOpt = steps.stream()
                    .filter(s -> s.getName().equals(stepName))
                    .findFirst();

            if (stepOpt.isPresent() && stepOpt.get().hasCompensation()) {
                SagaStep<T> step = stepOpt.get();
                try {
                    log.debug("Compensating step: {} for saga: {}", stepName, sagaId);

                    Object rawContext = instance.getStepContext().get(stepName);
                    Map<String, Object> stepContext = null;
                    if (rawContext instanceof Map) {
                        try {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> ctx = (Map<String, Object>) rawContext;
                            stepContext = ctx;
                        } catch (ClassCastException e) {
                            // If cast fails, use null context
                            stepContext = null;
                        }
                    }
                    T compensationContext = prepareCompensationContext(currentData, stepContext);

                    StepResult<T> result = step.getCompensation().apply(compensationContext);

                    if (result.isSuccess()) {
                        compensatedSteps.add(stepName);
                        log.debug("Step compensated successfully: {} for saga: {}", stepName, sagaId);
                    } else {
                        log.error("Compensation failed for step: {} in saga: {}", stepName, sagaId);
                        compensationError = new RuntimeException(
                                "Compensation failed for step: " + stepName + " - " + result.getMessage());
                        break;
                    }
                } catch (Exception e) {
                    log.error("Exception during compensation for step: {} in saga: {}", stepName, sagaId, e);
                    compensationError = e;
                    break;
                }
            }
        }

        // Update final state - re-fetch to get current version
        instance = sagaRepository.findById(sagaId).orElse(instance);
        if (compensationError == null) {
            instance.transitionTo(SagaState.COMPENSATED.name());
            instance.complete();
            sagaRepository.save(instance);
            log.info("Compensation completed successfully for saga: {}", sagaId);
            return SagaResult.compensated(sagaId, sagaType);
        } else {
            instance.transitionTo(SagaState.COMPENSATION_FAILED.name());
            instance.recordError("COMPENSATION", compensationError.getMessage());
            sagaRepository.save(instance);
            log.error("Compensation failed for saga: {}", sagaId, compensationError);
            return SagaResult.<T>builder()
                    .sagaId(sagaId)
                    .sagaType(sagaType)
                    .finalState(SagaState.COMPENSATION_FAILED)
                    .errorMessage(compensationError.getMessage())
                    .errorStep("COMPENSATION")
                    .completedAt(Instant.now())
                    .build();
        }
    }

    /**
     * Execute a step with retry logic.
     */
    protected StepResult<T> executeStepWithRetry(SagaStep<T> step, T data) {
        int attempts = 0;
        Duration delay = step.getRetryDelay();

        while (attempts <= step.getMaxRetries()) {
            try {
                return step.getAction().apply(data);
            } catch (Exception e) {
                attempts++;
                log.warn("Step {} failed (attempt {}/{}): {}",
                        step.getName(), attempts, step.getMaxRetries() + 1, e.getMessage());

                if (attempts > step.getMaxRetries()) {
                    return StepResult.failure(data, "Max retries exceeded: " + e.getMessage(), e);
                }

                // IMP-068: Non-blocking wait using Spring-managed ScheduledExecutorService
                try {
                    CompletableFuture<Void> waitFuture = new CompletableFuture<>();
                    sagaRetryScheduler.schedule(() -> waitFuture.complete(null), delay.toMillis(), TimeUnit.MILLISECONDS);
                    waitFuture.get(); // blocks this saga worker thread, NOT Tomcat threads
                    delay = delay.multipliedBy(2); // Exponential backoff
                } catch (Exception ie) {
                    Thread.currentThread().interrupt();
                    return StepResult.failure(data, "Retry interrupted", ie);
                }
            }
        }

        return StepResult.failure(data, "Unexpected retry loop exit");
    }

    /**
     * Create a new saga instance.
     */
    protected SagaInstance createSagaInstance(String sagaId, T initialData) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("initialData", initialData);
        payload.put("correlationId", sagaId);

        return SagaInstance.builder()
                .sagaId(sagaId)
                .sagaType(sagaType)
                .currentState(SagaState.STARTED.name())
                .payload(payload)
                .build();
    }

    /**
     * Prepare context for compensation.
     * Override this method to customize compensation context preparation.
     */
    protected T prepareCompensationContext(T currentData, Map<String, Object> stepContext) {
        return currentData;
    }

    /**
     * Get the saga type.
     */
    public String getSagaType() {
        return sagaType;
    }

    /**
     * Get the list of steps.
     */
    protected List<SagaStep<T>> getSteps() {
        return Collections.unmodifiableList(steps);
    }

    /**
     * Save saga instance and refresh to get updated version.
     */
    protected SagaInstance saveAndRefresh(SagaInstance instance) {
        SagaInstance saved = sagaRepository.save(instance);
        if (entityManager != null && entityManager.contains(saved)) {
            entityManager.refresh(saved);
        }
        return saved;
    }
}
