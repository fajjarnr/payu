package id.payu.saga.orchestrator;

import id.payu.saga.entity.SagaInstance;
import id.payu.saga.model.SagaResult;
import id.payu.saga.model.SagaState;
import id.payu.saga.model.SagaStep;
import id.payu.saga.model.StepResult;
import id.payu.saga.repository.SagaRepository;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Reactive saga orchestrator for non-blocking saga execution.
 * Uses Project Reactor for reactive programming.
 *
 * @param <T> The saga context/data type
 */
@Slf4j
public abstract class ReactiveSagaOrchestrator<T> {

    protected final SagaRepository sagaRepository;
    private final List<SagaStep<T>> steps = new ArrayList<>();
    private String sagaType;

    public ReactiveSagaOrchestrator(SagaRepository sagaRepository) {
        this.sagaRepository = sagaRepository;
    }

    /**
     * Initialize the orchestrator.
     */
    protected void initialize(String sagaType, List<SagaStep<T>> steps) {
        this.sagaType = sagaType;
        this.steps.addAll(steps);
        log.info("Initialized reactive saga orchestrator for type: {} with {} steps", sagaType, steps.size());
    }

    /**
     * Execute saga reactively.
     */
    public Mono<SagaResult<T>> execute(T initialData) {
        String sagaId = UUID.randomUUID().toString();
        return executeWithId(sagaId, initialData);
    }
    /**
     * Execute saga with specific ID reactively.
     */
    public Mono<SagaResult<T>> executeWithId(String sagaId, T initialData) {
        log.info("Starting reactive saga execution: {} - type: {}", sagaId, sagaType);

        return createSagaInstanceMono(sagaId, initialData)
                .flatMap(instance -> executeSteps(sagaId, instance, initialData))
                .onErrorResume(e -> handleExecutionError(sagaId, e));
    }

    /**
     * Create saga instance reactively.
     */
    protected Mono<SagaInstance> createSagaInstanceMono(String sagaId, T initialData) {
        return Mono.fromCallable(() -> {
            Map<String, Object> payload = new HashMap<>();
            payload.put("initialData", initialData);
            payload.put("correlationId", sagaId);

            SagaInstance instance = SagaInstance.create(sagaType, SagaState.STARTED.name(), payload);
            instance.setSagaId(sagaId);
            return sagaRepository.save(instance);
        }).subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * Execute all saga steps reactively.
     */
    protected Mono<SagaResult<T>> executeSteps(String sagaId, SagaInstance instance, T initialData) {
        // Use a concurrent map to track state across reactive operations
        final Map<String, Object> executionState = new ConcurrentHashMap<>();
        executionState.put("data", initialData);
        executionState.put("executedSteps", new ArrayList<String>());

        return Flux.fromIterable(steps)
                .concatMap(step -> executeSingleStep(sagaId, instance, step, executionState))
                .takeWhile(result -> result.isSuccess() || shouldContinueOnFailure(result))
                .collectList()
                .flatMap(results -> {
                    // Check if any result failed
                    Optional<StepResult<T>> failure = results.stream()
                            .filter(r -> !r.isSuccess() && !shouldContinueOnFailure(r))
                            .findFirst();

                    if (failure.isPresent()) {
                        StepResult<T> failedResult = failure.get();
                        @SuppressWarnings("unchecked")
                        T currentData = (T) executionState.get("data");
                        @SuppressWarnings("unchecked")
                        List<String> executedSteps = (List<String>) executionState.get("executedSteps");

                        return compensateReactive(sagaId, executedSteps, currentData, failedResult.getError())
                                .flatMap(compResult -> {
                                    if (compResult.getFinalState() == SagaState.COMPENSATED) {
                                        return Mono.just(SagaResult.compensated(sagaId, sagaType));
                                    }
                                    return Mono.just(SagaResult.failure(sagaId, sagaType,
                                            failedResult.getMessage(), "STEP_FAILURE"));
                                });
                    }

                    // All steps succeeded
                    @SuppressWarnings("unchecked")
                    T finalData = (T) executionState.get("data");
                    return completeSaga(sagaId, instance, finalData);
                });
    }

    /**
     * Execute a single step with reactive retry logic.
     */
    protected Mono<StepResult<T>> executeSingleStep(String sagaId, SagaInstance instance,
                                                     SagaStep<T> step, Map<String, Object> executionState) {
        log.debug("Executing reactive step: {} for saga: {}", step.getName(), sagaId);

        @SuppressWarnings("unchecked")
        T currentData = (T) executionState.get("data");

        // Check preconditions
        if (!step.canExecute(currentData)) {
            log.warn("Precondition not met for step: {} in saga: {}", step.getName(), sagaId);
            return Mono.just(StepResult.success(currentData, "Precondition not met, skipped"));
        }

        // Update state
        return updateSagaState(sagaId, "EXECUTING_" + step.getName())
                .then(executeStepWithRetryReactive(step, currentData)
                        .flatMap(result -> {
                            if (result.isSuccess()) {
                                executionState.put("data", result.getContext());
                                @SuppressWarnings("unchecked")
                                List<String> executedSteps = (List<String>) executionState.get("executedSteps");
                                executedSteps.add(step.getName());

                                return recordStepCompletion(sagaId, step.getName(), result.getMetadata())
                                        .thenReturn(result);
                            }
                            return Mono.just(result);
                        })
                );
    }

    /**
     * Execute step with reactive retry.
     */
    protected Mono<StepResult<T>> executeStepWithRetryReactive(SagaStep<T> step, T data) {
        return Mono.defer(() -> {
            try {
                return Mono.just(step.getAction().apply(data));
            } catch (Exception e) {
                return Mono.error(e);
            }
        })
        .retryWhen(reactor.util.retry.Retry.backoff(step.getMaxRetries(), step.getRetryDelay())
                .filter(e -> !(e instanceof InterruptedException))
                .doBeforeRetry(retrySignal ->
                        log.warn("Retrying step {} (attempt {})", step.getName(), retrySignal.totalRetries() + 1)))
        .onErrorResume(e -> {
            log.error("Step {} failed after retries: {}", step.getName(), e.getMessage());
            return Mono.just(StepResult.failure(data, "Max retries exceeded: " + e.getMessage(), e));
        });
    }

    /**
     * Reactive compensation.
     */
    protected Mono<SagaResult<T>> compensateReactive(String sagaId, List<String> executedSteps,
                                                      T currentData, Throwable error) {
        log.info("Starting reactive compensation for saga: {}", sagaId);

        return Mono.fromCallable(() -> sagaRepository.findBySagaId(sagaId))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(optional -> {
                    if (optional.isEmpty()) {
                        return Mono.just(SagaResult.<T>failure(sagaId, sagaType, "Saga instance not found", "COMPENSATION"));
                    }

                    SagaInstance instance = optional.get();
                    instance.transitionTo(SagaState.COMPENSATING.name());

                    return Mono.fromCallable(() -> sagaRepository.save(instance))
                            .subscribeOn(Schedulers.boundedElastic())
                            .flatMap(saved -> executeCompensationSteps(sagaId, executedSteps, currentData));
                });
    }

    /**
     * Execute compensation steps reactively.
     */
    protected Mono<SagaResult<T>> executeCompensationSteps(String sagaId, List<String> executedSteps, T currentData) {
        List<String> stepsToCompensate = new ArrayList<>(executedSteps);
        Collections.reverse(stepsToCompensate);

        return Flux.fromIterable(stepsToCompensate)
                .concatMap(stepName -> compensateSingleStep(sagaId, stepName, currentData))
                .collectList()
                .flatMap(compensated -> {
                    return updateSagaState(sagaId, SagaState.COMPENSATED.name())
                            .then(Mono.fromCallable(() -> {
                                Optional<SagaInstance> instanceOpt = sagaRepository.findBySagaId(sagaId);
                                if (instanceOpt.isPresent()) {
                                    SagaInstance instance = instanceOpt.get();
                                    instance.complete();
                                    sagaRepository.save(instance);
                                }
                                return SagaResult.<T>compensated(sagaId, sagaType);
                            }).subscribeOn(Schedulers.boundedElastic()));
                })
                .onErrorResume(e -> {
                    log.error("Reactive compensation failed for saga: {}", sagaId, e);
                    return updateSagaState(sagaId, SagaState.COMPENSATION_FAILED.name())
                            .then(Mono.just(SagaResult.<T>builder()
                                    .sagaId(sagaId)
                                    .sagaType(sagaType)
                                    .finalState(SagaState.COMPENSATION_FAILED)
                                    .errorMessage(e.getMessage())
                                    .errorStep("COMPENSATION")
                                    .completedAt(Instant.now())
                                    .build()));
                });
    }

    /**
     * Compensate a single step.
     */
    protected Mono<Boolean> compensateSingleStep(String sagaId, String stepName, T currentData) {
        Optional<SagaStep<T>> stepOpt = steps.stream()
                .filter(s -> s.getName().equals(stepName) && s.hasCompensation())
                .findFirst();

        if (stepOpt.isEmpty()) {
            return Mono.just(true);
        }

        SagaStep<T> step = stepOpt.get();
        return Mono.fromCallable(() -> {
            try {
                log.debug("Reactively compensating step: {} for saga: {}", stepName, sagaId);
                StepResult<T> result = step.getCompensation().apply(currentData);
                return result.isSuccess();
            } catch (Exception e) {
                log.error("Compensation failed for step: {} in saga: {}", stepName, sagaId, e);
                throw e;
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * Complete the saga successfully.
     */
    protected Mono<SagaResult<T>> completeSaga(String sagaId, SagaInstance instance, T finalData) {
        return updateSagaState(sagaId, SagaState.COMPLETED.name())
                .then(Mono.fromCallable(() -> {
                    Optional<SagaInstance> instanceOpt = sagaRepository.findBySagaId(sagaId);
                    if (instanceOpt.isPresent()) {
                        SagaInstance saved = instanceOpt.get();
                        saved.complete();
                        sagaRepository.save(saved);
                    }
                    log.info("Reactive saga completed successfully: {}", sagaId);
                    return SagaResult.success(sagaId, sagaType, finalData);
                }).subscribeOn(Schedulers.boundedElastic()));
    }

    /**
     * Update saga state reactively.
     */
    protected Mono<Void> updateSagaState(String sagaId, String newState) {
        return Mono.fromCallable(() -> {
            Optional<SagaInstance> instanceOpt = sagaRepository.findBySagaId(sagaId);
            if (instanceOpt.isPresent()) {
                SagaInstance instance = instanceOpt.get();
                instance.transitionTo(newState);
                sagaRepository.save(instance);
            }
            return null;
        }).subscribeOn(Schedulers.boundedElastic()).then();
    }

    /**
     * Record step completion.
     */
    protected Mono<Void> recordStepCompletion(String sagaId, String stepName, Map<String, Object> metadata) {
        return Mono.fromCallable(() -> {
            Optional<SagaInstance> instanceOpt = sagaRepository.findBySagaId(sagaId);
            if (instanceOpt.isPresent()) {
                SagaInstance instance = instanceOpt.get();
                instance.recordStepCompletion(stepName, metadata);
                sagaRepository.save(instance);
            }
            return null;
        }).subscribeOn(Schedulers.boundedElastic()).then();
    }

    /**
     * Handle execution errors.
     */
    protected Mono<SagaResult<T>> handleExecutionError(String sagaId, Throwable error) {
        log.error("Reactive saga execution error: {}", sagaId, error);
        return updateSagaState(sagaId, SagaState.FAILED.name())
                .then(Mono.just(SagaResult.failure(sagaId, sagaType, error.getMessage(), "EXECUTION")));
    }

    /**
     * Check if saga should continue on step failure.
     */
    protected boolean shouldContinueOnFailure(StepResult<T> result) {
        return !result.isTriggerCompensation();
    }

    /**
     * Get saga type.
     */
    public String getSagaType() {
        return sagaType;
    }
}
