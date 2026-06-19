package id.payu.statement.architecture;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.annotation.Transactional;

import static com.tngtech.archunit.library.Architectures.layeredArchitecture;
import static org.junit.jupiter.api.Assertions.assertTrue;

@AnalyzeClasses(packages = "id.payu.statement")
public class ArchitectureTest {

    @ArchTest
    static final ArchRule hexagonal_architecture = layeredArchitecture()
            .consideringOnlyDependenciesInAnyPackage("id.payu.statement..")
            .layer("Adapter.Web").definedBy("..adapter.web..")
            .layer("Adapter.Persistence").definedBy("..adapter.persistence..")
            .layer("Application").definedBy("..application..")
            .layer("Domain").definedBy("..domain..")
            .layer("Config").definedBy("..config..")

            .whereLayer("Adapter.Web").mayNotBeAccessedByAnyLayer()
            .whereLayer("Adapter.Persistence").mayOnlyBeAccessedByLayers("Application")
            .whereLayer("Application").mayOnlyBeAccessedByLayers("Adapter.Web")
            .whereLayer("Domain").mayOnlyBeAccessedByLayers("Adapter.Web", "Adapter.Persistence", "Application")
            .allowEmptyShould(true);

    // Epic E-19: Receipt Domain Architecture Tests
    @ArchTest
    static final ArchRule receipt_domain_classes_should_be_in_domain_model_package =
            ArchRuleDefinition.classes()
                    .that().haveSimpleNameContaining("Receipt")
                    .and().haveSimpleNameNotContaining("Test")
                    .and().haveSimpleNameNotContaining("Entity")
                    .and().haveSimpleNameNotContaining("Repository")
                    .and().haveSimpleNameNotContaining("Service")
                    .and().haveSimpleNameNotContaining("Controller")
                    .and().haveSimpleNameNotContaining("Response")
                    .and().haveSimpleNameNotContaining("Request")
                    .should().resideInAPackage("..domain.model..")
                    .allowEmptyShould(true);

    @ArchTest
    static final ArchRule receipt_repository_port_should_be_in_application_port_package =
            ArchRuleDefinition.classes()
                    .that().haveSimpleNameContaining("ReceiptRepositoryPort")
                    .should().resideInAPackage("..application.port.output..")
                    .allowEmptyShould(true);

    @ArchTest
    static final ArchRule receipt_service_should_be_in_application_service_package =
            ArchRuleDefinition.classes()
                    .that().haveSimpleName("ReceiptService")
                    .should().resideInAPackage("..application.service..")
                    .allowEmptyShould(true);

    @ArchTest
    static final ArchRule receipt_entity_should_be_in_adapter_persistence_package =
            ArchRuleDefinition.classes()
                    .that().haveSimpleName("ReceiptEntity")
                    .should().resideInAPackage("..adapter.persistence.entity..")
                    .allowEmptyShould(true);

    @ArchTest
    static final ArchRule receipt_repository_adapter_should_be_in_adapter_persistence_package =
            ArchRuleDefinition.classes()
                    .that().haveSimpleName("ReceiptRepositoryAdapter")
                    .should().resideInAPackage("..adapter.persistence..")
                    .allowEmptyShould(true);

    @ArchTest
    static final ArchRule domain_should_not_depend_on_spring_framework =
            ArchRuleDefinition.noClasses()
                    .that().resideInAPackage("..domain.model..")
                    .should().dependOnClassesThat()
                    .resideInAPackage("org.springframework..")
                    .allowEmptyShould(true);

    @ArchTest
    static final ArchRule value_objects_should_be_immutable =
            ArchRuleDefinition.classes()
                    .that().haveSimpleNameContaining("Info")
                    .and().resideInAPackage("..domain.model..")
                    .should().haveOnlyFinalFields()
                    .allowEmptyShould(true);

    @Test
    @DisplayName("BUG-STMT-ASYNC-001: @Async methods should not be @Transactional")
    void asyncMethodsShouldNotBeTransactional() {
        // BUG-BE-049 lesson: @Transactional on @Async method is a no-op.
        // @Async runs in a different thread; Spring's @Transactional proxy
        // is only applied at the call site, not on the async thread.
        // The transaction context is not propagated. Each repository.save()
        // call runs in its own implicit transaction.
        //
        // CALIBRATION 2026-06-19: ArchUnit 1.2.1 can't parse Java 25 bytecode
        // (ASM incompatibility — @AnalyzeClasses returns empty). Use Java
        // reflection on explicitly-known class instead.
        java.util.List<String> violations = new java.util.ArrayList<>();
        String[] targetClasses = {
            "id.payu.statement.application.service.StatementService",
        };
        for (String className : targetClasses) {
            try {
                Class<?> clazz = Class.forName(className);
                for (java.lang.reflect.Method method : clazz.getDeclaredMethods()) {
                    boolean hasAsync = method.isAnnotationPresent(Async.class);
                    boolean hasTx = method.isAnnotationPresent(Transactional.class);
                    if (hasAsync && hasTx) {
                        violations.add(String.format(
                            "Method %s.%s is @Async AND @Transactional (BUG-BE-049 no-op)",
                            className, method.getName()));
                    }
                }
            } catch (ClassNotFoundException e) {
                violations.add("Class not found: " + className);
            }
        }
        assertTrue(violations.isEmpty(),
            "Found @Async + @Transactional methods (BUG-BE-049 no-op):\n  " + String.join("\n  ", violations));
    }
}
