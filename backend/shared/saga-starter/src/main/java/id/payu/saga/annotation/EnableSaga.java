package id.payu.saga.annotation;

import id.payu.saga.config.SagaAutoConfiguration;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

/**
 * Annotation to enable saga pattern support in a Spring Boot application.
 * Add this to your main application class or configuration.
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(SagaAutoConfiguration.class)
public @interface EnableSaga {

    /**
     * Base packages to scan for saga orchestrators and steps.
     */
    String[] basePackages() default {};

    /**
     * Whether to enable saga persistence.
     */
    boolean enablePersistence() default true;

    /**
     * Whether to enable saga monitoring and metrics.
     */
    boolean enableMonitoring() default true;

    /**
     * Whether to enable automatic compensation.
     */
    boolean enableCompensation() default true;
}
