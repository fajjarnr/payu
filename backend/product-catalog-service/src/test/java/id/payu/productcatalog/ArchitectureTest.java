package id.payu.productcatalog;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

/**
 * ArchUnit tests to enforce Hexagonal Architecture compliance.
 *
 * Rules:
 * 1. Domain layer should not depend on infrastructure (adapter, config, Spring)
 * 2. Application layer should only access domain, not infrastructure directly
 * 3. No cyclic dependencies between packages
 */
@AnalyzeClasses(
        packages = "id.payu.productcatalog",
        importOptions = ImportOption.DoNotIncludeTests.class
)
public class ArchitectureTest {

    @ArchTest
    static final ArchRule domainShouldNotDependOnInfrastructure =
            noClasses()
                    .that().resideInAPackage("..domain..")
                    .should().dependOnClassesThat()
                    .resideInAnyPackage(
                            "..adapter..",
                            "..config..",
                            "org.springframework..",
                            "jakarta.persistence..",
                            "jakarta.validation.."
                    )
                    .because("Domain layer must be pure business logic without framework dependencies");

    @ArchTest
    static final ArchRule domainShouldNotDependOnApplication =
            noClasses()
                    .that().resideInAPackage("..domain..")
                    .should().dependOnClassesThat()
                    .resideInAPackage("..application..")
                    .because("Domain should not depend on application layer - dependency points inward");

    @ArchTest
    static final ArchRule applicationShouldNotDependOnAdapters =
            noClasses()
                    .that().resideInAPackage("..application..")
                    .should().dependOnClassesThat()
                    .resideInAnyPackage(
                            "..adapter.web..",
                            "..adapter.persistence..",
                            "..adapter.messaging.."
                    )
                    .because("Application layer should only depend on domain and ports, not concrete adapters");

    @ArchTest
    static final ArchRule noCyclicDependencies =
            slices()
                    .matching("id.payu.productcatalog.(*)..")
                    .should().beFreeOfCycles()
                    .because("Hexagonal architecture should not have cyclic dependencies between layers");

    @ArchTest
    static final ArchRule adaptersShouldDependOnApplication =
            noClasses()
                    .that().resideInAPackage("..adapter..")
                    .should().dependOnClassesThat()
                    .resideInAPackage("..dto..")
                    .because("DTOs should be defined in adapter layer, not depended on by it");

    @ArchTest
    static final ArchRule repositoriesShouldBeInPersistenceAdapter =
            noClasses()
                    .that().resideOutsideOfPackage("..adapter.persistence..")
                    .and().haveSimpleNameContaining("Repository")
                    .should().exist()
                    .because("Repositories should only exist in persistence adapter");
}
