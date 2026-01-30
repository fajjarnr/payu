package id.payu.saga.annotation;

import java.lang.annotation.*;

/**
 * Annotation to mark a method as a saga step.
 * Used for automatic saga orchestration and monitoring.
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface SagaStep {

    /**
     * The name of this saga step.
     * Should be unique within the saga.
     */
    String name();

    /**
     * Description of what this step does.
     */
    String description() default "";

    /**
     * The saga type this step belongs to.
     */
    String sagaType();

    /**
     * Order of execution within the saga.
     * Lower values are executed first.
     */
    int order() default 0;

    /**
     * Whether this step has a compensation action.
     */
    boolean compensable() default true;

    /**
     * The name of the compensation method.
     * If empty, assumes method name + "Compensate" pattern.
     */
    String compensationMethod() default "";

    /**
     * Whether this step is critical (cannot be skipped).
     */
    boolean critical() default false;

    /**
     * Maximum number of retries for this step.
     */
    int maxRetries() default 0;

    /**
     * Retry delay in milliseconds.
     */
    long retryDelayMs() default 1000;

    /**
     * Timeout for this step in milliseconds.
     */
    long timeoutMs() default 30000;

    /**
     * Whether to continue saga execution on failure.
     */
    boolean continueOnFailure() default false;

    /**
     * Event topic to publish step completion to.
     */
    String publishEvent() default "";

    /**
     * Whether to persist step result to saga instance.
     */
    boolean persistResult() default true;
}
