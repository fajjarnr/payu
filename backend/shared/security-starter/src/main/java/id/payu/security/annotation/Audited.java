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
    Operation operation() default Operation.OTHER;

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
    enum Operation {
        CREATE,
        READ,
        UPDATE,
        DELETE,
        TRANSFER,
        LOGIN,
        LOGOUT,
        KYC_APPROVE,
        KYC_REJECT,
        PASSWORD_CHANGE,
        PASSWORD_RESET,
        ACCOUNT_BLOCK,
        ACCOUNT_UNBLOCK,
        OTHER
    }

    /**
     * Audit levels
     */
    enum AuditLevel {
        DEBUG,
        INFO,
        WARN,
        ERROR
    }
}
