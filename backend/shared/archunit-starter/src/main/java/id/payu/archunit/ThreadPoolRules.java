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

    /**
     * Rule: No direct usage of Executors factory methods in field initialization.
     *
     * <p>Direct usage of {@code Executors.newCachedThreadPool()}, {@code Executors.newFixedThreadPool()},
     * etc. in field initialization bypasses Spring's lifecycle management.</p>
     */
    public static final ArchRule NO_EXECUTORS_FACTORY_METHODS = noFields()
            .should(useExecutorsFactoryMethods())
            .because("Executors factory methods create unmanaged thread pools. " +
                    "Use Spring-managed ThreadPoolTaskExecutor instead (IMP-068).");

    /**
     * Condition that checks if a field uses Executors factory methods for initialization.
     */
    private static ArchCondition<JavaField> useExecutorsFactoryMethods() {
        return new ArchCondition<>("use Executors factory methods") {
            @Override
            public void check(JavaField field, ConditionEvents events) {
                // Check if the field type is an executor type
                JavaClass rawType = field.getRawType();
                boolean isExecutorType = rawType.isAssignableFrom(Executor.class)
                        || rawType.isAssignableFrom(ExecutorService.class)
                        || rawType.isAssignableFrom(ScheduledExecutorService.class);

                if (isExecutorType && field.getInitializer().isPresent()) {
                    String initializer = field.getInitializer().get().toString();
                    if (initializer.contains("Executors.new")) {
                        events.add(SimpleConditionEvent.violated(field,
                                String.format("Field %s uses Executors factory method: %s",
                                        field.getFullName(), initializer)));
                    }
                }
            }
        };
    }

    /**
     * Combined rule for thread pool best practices.
     *
     * <p>Enforces all thread pool related rules.</p>
     */
    public static final ArchRule THREAD_POOL_BEST_PRACTICES = NO_STATIC_UNMANAGED_EXECUTORS
            .and(NO_EXECUTORS_FACTORY_METHODS);
}
