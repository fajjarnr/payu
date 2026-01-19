package id.payu.account.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

@DisplayName("Architecture Rules")
class ArchitectureTest {

    private static JavaClasses importedClasses;

    @BeforeAll
    static void setUp() {
        importedClasses = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("id.payu.account");
    }

    @Nested
    @DisplayName("Hexagonal Architecture")
    class HexagonalArchitectureRules {

        @Test
        @DisplayName("should follow hexagonal architecture layers")
        void shouldFollowHexagonalArchitecture() {
            layeredArchitecture()
                    .consideringAllDependencies()
                    .layer("Domain").definedBy("..domain..")
                    .layer("Application").definedBy("..application..")
                    .layer("Adapter").definedBy("..adapter..")
                    .layer("Config").definedBy("..config..")

                    // Domain should rely on nothing (pure java ideally, but pojos/dtos allowed)
                    // Application relies on Domain
                    // Adapter relies on Domain and Application (for Ports)
                    
                    .whereLayer("Domain").mayOnlyBeAccessedByLayers("Application", "Adapter", "Config")
                    .whereLayer("Application").mayOnlyBeAccessedByLayers("Adapter", "Config")
                    // .whereLayer("Adapter").mayNotBeAccessedByAnyLayer() // Ideally true but config accesses it
                    
                    .check(importedClasses);
        }
    }
}
