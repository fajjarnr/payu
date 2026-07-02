package id.payu.wallet.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.*;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

@DisplayName("Architecture Rules")
class ArchitectureTest {

    private static JavaClasses importedClasses;

    @BeforeAll
    static void setUp() {
        importedClasses = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("id.payu.wallet");
    }

    @Nested
    @DisplayName("Hexagonal Architecture")
    class HexagonalArchitectureRules {

        @Test
        @DisplayName("should follow hexagonal architecture layers")
        void shouldFollowHexagonalArchitecture() {
            layeredArchitecture()
                    .consideringAllDependencies()
                    .withOptionalLayers(true)
                    .layer("Adapter.Web").definedBy("..adapter.web..")
                    .layer("Adapter.Persistence").definedBy("..adapter.persistence..")
                    .layer("Adapter.Client").definedBy("..adapter.client..")
                    .layer("Adapter.Messaging").definedBy("..adapter.messaging..")
                    .layer("Adapter.Grpc").definedBy("..adapter.grpc..")
                    .layer("Application").definedBy("..application..")
                    .layer("Domain").definedBy("..domain..")
                    .layer("Config").definedBy("..config..")
                    .layer("Dto").definedBy("..dto..")
                    .whereLayer("Adapter.Web").mayNotBeAccessedByAnyLayer()
                    .whereLayer("Domain").mayOnlyBeAccessedByLayers(
                            "Application", "Adapter.Web", "Adapter.Persistence",
                            "Adapter.Client", "Adapter.Messaging", "Adapter.Grpc", "Config", "Dto")
                    .allowEmptyShould(true)
                    .check(importedClasses);
        }
    }

    @Nested
    @DisplayName("Domain Isolation Rules")
    class DomainIsolationRules {

        @Test
        @DisplayName("domain should not depend on infrastructure")
        void domainShouldNotDependOnInfrastructure() {
            ArchRule rule = noClasses()
                    .that().resideInAPackage("..domain..")
                    .and().resideOutsideOfPackage("..domain.port..")
                    .should().dependOnClassesThat()
                    .resideInAnyPackage(
                            "..adapter..",
                            "..application.."
                    )
                    .because("Domain layer should be independent of infrastructure and application");
            rule.check(importedClasses);
        }

        @Test
        @DisplayName("domain should not depend on Spring framework")
        void domainShouldNotDependOnSpring() {
            ArchRule rule = noClasses()
                    .that().resideInAPackage("..domain..")
                    .should().dependOnClassesThat()
                    .resideInAnyPackage(
                            "org.springframework..",
                            "org.springframework.web.."
                    )
                    .because("Domain layer should not depend on Spring Framework");
            rule.check(importedClasses);
        }
    }
}
