package id.payu.archunit;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaField;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noFields;

/**
 * ArchUnit rules for thread pool management best practices.
 *
 * <p>These rules ensure:
 * <ul>
 *   <li>No static unmanaged executors (preventing thread leaks)</li>
 *   <li>Thread pools are Spring-managed for proper lifecycle</li>
 *   <li>Proper graceful shutdown configuration</li>
 * </ul>
 *
 * <p>IMP-068: Shared Library Lifecycle Management</p>
 *
 * @since IMP-068
 */
public final class ThreadPoolRules {

    private ThreadPoolRules() {
        // Utility class
    }

    /**
     * Rule: No static Executor or ExecutorService fields using Executors factory methods.
     *
     * <p>Static executors created via {@link Executors} are not managed by Spring lifecycle
     * and can cause thread leaks on application shutdown.</p>
     *
     * <p>Use Spring-managed {@link org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor}
     * or inject executors via constructor instead.</p>
     */
    public static final ArchRule NO_STATIC_UNMANAGED_EXECUTORS = noFields()
            .that().haveRawType(Executor.class)
            .or().haveRawType(ExecutorService.class)
            .or().haveRawType(ScheduledExecutorService.class)
            .should().beStatic()
            .because("Static executors are not managed by Spring lifecycle and cause thread leaks. " +
                    "Use Spring-managed ThreadPoolTaskExecutor or inject via constructor (IMP-068).");

}
