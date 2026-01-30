package id.payu.archunit.conditions;

import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;

import java.util.Arrays;
import java.util.List;

/**
 * ArchUnit condition to check if a method is annotated with @Transactional.
 *
 * <p>Supports both Spring's @Transactional and Jakarta's @Transactional.
 *
 * @author PayU Architecture Team
 * @version 1.0.0
 */
public class TransactionalMethodCondition extends ArchCondition<JavaMethod> {

    private static final List<String> TRANSACTIONAL_ANNOTATIONS = Arrays.asList(
            "org.springframework.transaction.annotation.Transactional",
            "jakarta.transaction.Transactional"
    );

    private final boolean allowReadOnly;

    /**
     * Creates a condition that checks for @Transactional annotation.
     */
    public TransactionalMethodCondition() {
        this(false);
    }

    /**
     * Creates a condition that checks for @Transactional annotation.
     *
     * @param allowReadOnly if true, readOnly=true is acceptable; if false, requires readOnly=false or not set
     */
    public TransactionalMethodCondition(boolean allowReadOnly) {
        super("be annotated with @Transactional" + (allowReadOnly ? " (read-only allowed)" : ""));
        this.allowReadOnly = allowReadOnly;
    }

    @Override
    public void check(JavaMethod method, ConditionEvents events) {
        boolean hasTransactional = TRANSACTIONAL_ANNOTATIONS.stream()
                .anyMatch(annotation -> method.isAnnotatedWith(annotation));

        if (!hasTransactional) {
            events.add(SimpleConditionEvent.violated(method,
                    String.format("Method %s.%s is not annotated with @Transactional",
                            method.getOwner().getName(), method.getName())));
            return;
        }

        // Check readOnly attribute if required
        if (!allowReadOnly) {
            // Note: Full attribute checking would require more complex annotation processing
            // This is a simplified check
            boolean isReadOnly = method.getAnnotations().stream()
                    .anyMatch(annotation -> {
                        String annotationType = annotation.getRawType().getName();
                        return TRANSACTIONAL_ANNOTATIONS.contains(annotationType)
                                && annotation.toString().contains("readOnly=true");
                    });

            if (isReadOnly) {
                events.add(SimpleConditionEvent.violated(method,
                        String.format("Method %s.%s has @Transactional(readOnly=true) but write operation detected",
                                method.getOwner().getName(), method.getName())));
            }
        }
    }
}
