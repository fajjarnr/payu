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
    }

    /**
     * Predicate that matches domain entity classes (not JPA entities).
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

    public static DescribedPredicate<JavaClass> areDomainServices() {
        return new DescribedPredicate<>("are domain services") {
            @Override
            public boolean test(JavaClass javaClass) {
                return javaClass.getPackageName().contains(".domain.service")
                        && javaClass.getSimpleName().endsWith("Service");
            }
        };
    }

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
