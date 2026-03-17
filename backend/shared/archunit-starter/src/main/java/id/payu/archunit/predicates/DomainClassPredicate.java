package id.payu.archunit.predicates;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;

/**
 * Predicates for identifying domain layer classes.
 *
 * @author PayU Architecture Team
 * @version 1.0.0
 */
public final class DomainClassPredicate {

    private DomainClassPredicate() {
        // Utility class
    }

    /**
     * Predicate that matches domain entity classes (not JPA entities).
     *
     * @return predicate matching domain entities
     */
    public static DescribedPredicate<JavaClass> areDomainEntities() {
        return new DescribedPredicate<>("are domain entities") {
            @Override
            public boolean test(JavaClass javaClass) {
                return javaClass.getPackageName().contains(".domain.model")
                        && !javaClass.isAnnotatedWith("jakarta.persistence.Entity")
                        && !javaClass.getSimpleName().endsWith("Entity");
            }
        };
    }

    /**
     * Predicate that matches domain value objects.
     *
     * @return predicate matching value objects
     */
    public static DescribedPredicate<JavaClass> areValueObjects() {
        return new DescribedPredicate<>("are value objects") {
            @Override
            public boolean test(JavaClass javaClass) {
                String name = javaClass.getSimpleName();
                return name.endsWith("Id")
                        || name.endsWith("Code")
                        || name.endsWith("Number")
                        || name.endsWith("Amount")
                        || name.endsWith("Date")
                        || name.endsWith("Status");
            }
        };
    }

    /**
     * Predicate that matches domain aggregate roots.
     *
     * @return predicate matching aggregate roots
     */
    public static DescribedPredicate<JavaClass> areAggregateRoots() {
        return new DescribedPredicate<>("are aggregate roots") {
            @Override
            public boolean test(JavaClass javaClass) {
                // Aggregate roots typically have methods like add, remove, getId
                return javaClass.getPackageName().contains(".domain.model")
                        && javaClass.getMethods().stream()
                                .anyMatch(method -> method.getName().equals("getId"));
            }
        };
    }

    /**
     * Predicate that matches domain services.
     *
     * @return predicate matching domain services
     */
    public static DescribedPredicate<JavaClass> areDomainServices() {
        return new DescribedPredicate<>("are domain services") {
            @Override
            public boolean test(JavaClass javaClass) {
                return javaClass.getPackageName().contains(".domain.service")
                        && javaClass.getSimpleName().endsWith("Service");
            }
        };
    }

    /**
     * Predicate that matches port interfaces.
     *
     * @return predicate matching ports
     */
    public static DescribedPredicate<JavaClass> arePorts() {
        return new DescribedPredicate<>("are ports") {
            @Override
            public boolean test(JavaClass javaClass) {
                return javaClass.getPackageName().contains(".port")
                        && javaClass.isInterface();
            }
        };
    }

    /**
     * Predicate that matches input ports (use case interfaces).
     *
     * @return predicate matching input ports
     */
    public static DescribedPredicate<JavaClass> areInputPorts() {
        return new DescribedPredicate<>("are input ports") {
            @Override
            public boolean test(JavaClass javaClass) {
                return (javaClass.getPackageName().contains(".port.in")
                        || javaClass.getPackageName().contains(".port.input"))
                        && javaClass.isInterface();
            }
        };
    }

    /**
     * Predicate that matches output ports (repository/spi interfaces).
     *
     * @return predicate matching output ports
     */
    public static DescribedPredicate<JavaClass> areOutputPorts() {
        return new DescribedPredicate<>("are output ports") {
            @Override
            public boolean test(JavaClass javaClass) {
                return (javaClass.getPackageName().contains(".port.out")
                        || javaClass.getPackageName().contains(".port.output"))
                        && javaClass.isInterface();
            }
        };
    }
}
