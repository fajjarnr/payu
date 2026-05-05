package id.payu.dispute;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

/**
 * ArchUnit tests to verify hexagonal architecture compliance.
 *
 * <p>These tests ensure that the dispute service follows the hexagonal architecture
 * pattern with proper layer separation and dependency direction.</p>
 */
class ArchitectureTest {

    private static JavaClasses classes;

    @BeforeAll
    static void setUp() {
        classes = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("id.payu.dispute");
    }

    @BeforeEach
    void skipIfNoClasses() {
        Assumptions.assumeFalse(classes.isEmpty(),
                "Skipping architecture tests: no classes imported (likely Java 25 / ASM incompatibility)");
    }

    @Test
    void shouldFollowHexagonalArchitecture() {
        layeredArchitecture()
                .consideringOnlyDependenciesInAnyPackage("id.payu.dispute..")
                .layer("Adapter.Web").definedBy("..adapter.web..")
                .layer("Adapter.Persistence").definedBy("..adapter.persistence..")
                .layer("Application").definedBy("..application..")
                .layer("Domain").definedBy("..domain..")
                .layer("Config").definedBy("..config..")
                .layer("Dto").definedBy("..dto..")
                .whereLayer("Adapter.Web").mayNotBeAccessedByAnyLayer()
                .whereLayer("Adapter.Persistence").mayOnlyBeAccessedByLayers("Application")
                .whereLayer("Application").mayOnlyBeAccessedByLayers("Adapter.Web")
                .whereLayer("Domain").mayOnlyBeAccessedByLayers("Adapter.Web", "Adapter.Persistence", "Application", "Dto")
                .whereLayer("Dto").mayOnlyBeAccessedByLayers("Adapter.Web", "Application")
                .check(classes);
    }
}
