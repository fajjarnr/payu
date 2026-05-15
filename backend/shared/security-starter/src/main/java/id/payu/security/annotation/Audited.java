package id.payu.security.annotation;

import java.lang.annotation.*;

/**
 * Annotation for auditing sensitive operations
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Audited {

    /**
     * Operation type
     */
    AuditOperation operation() default AuditOperation.OTHER;

    /**
     * Entity type being operated on
     */
    String entityType() default "";

    /**
     * Whether to mask sensitive data in audit logs
     */
    boolean maskData() default true;

    /**
     * Audit log level
     */
    AuditLevel level() default AuditLevel.INFO;

    /**
     * Operation types
     */

    /**
     * Audit levels
     */
}
