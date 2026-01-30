package id.payu.saga.annotation;

import java.lang.annotation.*;

/**
 * Annotation to mark a class as a saga orchestrator.
 * Enables automatic saga management and monitoring.
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface SagaOrchestration {

    /**
     * The type/name of this saga.
     * Should be unique across the system.
     */
    String value();

    /**
     * Description of the saga.
     */
    String description() default "";

    /**
     * Whether to enable automatic persistence of saga state.
     */
    boolean persistent() default true;

    /**
     * Whether to enable compensation on failure.
     */
    boolean compensating() default true;

    /**
     * Maximum time for saga completion.
     * Saga will be marked as TIMED_OUT after this duration.
     */
    long timeoutMs() default 300000; // 5 minutes

    /**
     * Whether to publish saga lifecycle events.
     */
    boolean publishEvents() default true;

    /**
     * Event topic for saga lifecycle events.
     */
    String eventTopic() default "saga.events";
}
