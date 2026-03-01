import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition;
import com.tngtech.archunit.library.dependencies.SlicesRuleDefinition;
import org.junit.jupiter.api.Test;

/**
 * Architecture tests to enforce hexagonal architecture principles.
 * These tests ensure the service maintains proper layering and dependencies.
 */
class ArchitectureTest {

    private static final String BASE_PACKAGE = "${{ values.java_package }}";

    private final JavaClasses importedClasses = new ClassFileImporter()
            .importPackages(BASE_PACKAGE);

    /**
     * Domain layer should not depend on infrastructure (adapter) layer.
     * This is the core rule of hexagonal architecture.
     */
    @Test
    void domainShouldNotDependOnAdapter() {
        ArchRule rule = ArchRuleDefinition.noClasses()
                .that()
                .resideInAPackage("..domain..")
                .should()
                .dependOnClassesThat()
                .resideInAPackage("..adapter..");

        rule.check(importedClasses);
    }

    /**
     * Domain layer should not depend on application layer.
     * Domain is the innermost layer and should have no inward dependencies.
     */
    @Test
    void domainShouldNotDependOnApplication() {
        ArchRule rule = ArchRuleDefinition.noClasses()
                .that()
                .resideInAPackage("..domain..")
                .should()
                .dependOnClassesThat()
                .resideInAPackage("..application..");

        rule.check(importedClasses);
    }

    /**
     * Domain layer should not depend on Spring framework.
     * Domain should be pure Java with no framework dependencies.
     */
    @Test
    void domainShouldNotDependOnSpring() {
        ArchRule rule = ArchRuleDefinition.noClasses()
                .that()
                .resideInAPackage("..domain..")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage("org.springframework..");

        rule.check(importedClasses);
    }

    /**
     * Application layer should not depend on adapter (infrastructure) layer.
     * Application layer orchestrates domain logic but doesn't depend on infrastructure.
     */
    @Test
    void applicationShouldNotDependOnAdapter() {
        ArchRule rule = ArchRuleDefinition.noClasses()
                .that()
                .resideInAPackage("..application..")
                .should()
                .dependOnClassesThat()
                .resideInAPackage("..adapter..");

        rule.check(importedClasses);
    }

    /**
     * Adapters should not depend on each other.
     * Each adapter is independent and communicates only through domain/application.
     */
    @Test
    void adaptersShouldNotDependOnEachOther() {
        SlicesRuleDefinition.slices()
                .matching("..adapter.(*)..")
                .should()
                .notDependOnEachOther()
                .check(importedClasses);
    }

    /**
     * All domain models should be in the domain package.
     */
    @Test
    void domainModelsShouldBeInDomainPackage() {
        ArchRule rule = ArchRuleDefinition.classes()
                .that()
                .haveSimpleNameEndingWith("Entity")
                .or()
                .haveSimpleNameEndingWith("Aggregate")
                .or()
                .haveSimpleNameEndingWith("ValueObject")
                .should()
                .resideInAPackage("..domain.model..");

        rule.check(importedClasses);
    }

    /**
     * Repository interfaces (ports) should be in domain.port package.
     */
    @Test
    void repositoryInterfacesShouldBeInDomainPortPackage() {
        ArchRule rule = ArchRuleDefinition.classes()
                .that()
                .haveSimpleNameEndingWith("Repository")
                .and()
                .areInterfaces()
                .should()
                .resideInAPackage("..domain.port..");

        rule.check(importedClasses);
    }

    /**
     * Use case interfaces should be in application.port package.
     */
    @Test
    void useCasesShouldBeInApplicationPortPackage() {
        ArchRule rule = ArchRuleDefinition.classes()
                .that()
                .haveSimpleNameEndingWith("UseCase")
                .and()
                .areInterfaces()
                .should()
                .resideInAPackage("..application.port..");

        rule.check(importedClasses);
    }

    /**
     * Controllers should be in adapter.web package.
     */
    @Test
    void controllersShouldBeInAdapterWebPackage() {
        ArchRule rule = ArchRuleDefinition.classes()
                .that()
                .haveSimpleNameEndingWith("Controller")
                .should()
                .resideInAPackage("..adapter.web..");

        rule.check(importedClasses);
    }

    /**
     * Repository implementations should be in adapter.persistence package.
     */
    @Test
    void repositoryImplementationsShouldBeInAdapterPersistencePackage() {
        ArchRule rule = ArchRuleDefinition.classes()
                .that()
                .haveSimpleNameEndingWith("RepositoryImpl")
                .or()
                .haveSimpleNameEndingWith("JpaRepository")
                .should()
                .resideInAPackage("..adapter.persistence..");

        rule.check(importedClasses);
    }
}
