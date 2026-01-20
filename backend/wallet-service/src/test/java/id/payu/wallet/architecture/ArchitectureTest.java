package id.payu.wallet.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.*;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

@DisplayName("Architecture Tests - Hexagonal Architecture Enforcement")
class ArchitectureTest {

    private static JavaClasses classes;

    @BeforeAll
    static void setup() {
        classes = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("id.payu.wallet");
    }

    @Test
    @DisplayName("Domain layer should not depend on adapters or application layer")
    void domainShouldNotDependOnOuterLayers() {
        noClasses()
                .that().resideInAPackage("..domain..")
                .should().dependOnClassesThat().resideInAPackage("..adapter..")
                .because("Domain layer must be independent of infrastructure")
                .check(classes);
    }

    @Test
    @DisplayName("Domain model should not depend on Spring Framework")
    void domainModelShouldNotDependOnSpring() {
        noClasses()
                .that().resideInAPackage("..domain.model..")
                .should().dependOnClassesThat().resideInAPackage("org.springframework..")
                .because("Domain model must be framework-agnostic")
                .check(classes);
    }

    @Test
    @DisplayName("Ports should be interfaces")
    void portsShouldBeInterfaces() {
        classes()
                .that().resideInAPackage("..domain.port..")
                .should().beInterfaces()
                .because("Ports define contracts and should be interfaces")
                .check(classes);
    }

    @Test
    @DisplayName("Card adapter should implement CardPersistencePort")
    void cardAdapterShouldImplementCardPersistencePort() {
        classes()
                .that().resideInAPackage("..adapter.persistence..")
                .and().haveSimpleName("CardPersistenceAdapter")
                .should().implement(id.payu.wallet.domain.port.out.CardPersistencePort.class)
                .because("CardPersistenceAdapter should implement CardPersistencePort")
                .check(classes);
    }

    @Test
    @DisplayName("Wallet adapter should implement WalletPersistencePort")
    void walletAdapterShouldImplementWalletPersistencePort() {
        classes()
                .that().resideInAPackage("..adapter.persistence..")
                .and().haveSimpleName("WalletPersistenceAdapter")
                .should().implement(id.payu.wallet.domain.port.out.WalletPersistencePort.class)
                .because("WalletPersistenceAdapter should implement WalletPersistencePort")
                .check(classes);
    }

    @Test
    @DisplayName("Controllers should be in adapter.web package")
    void controllersShouldBeInWebPackage() {
        classes()
                .that().haveSimpleNameEndingWith("Controller")
                .should().resideInAPackage("..adapter.web..")
                .because("Controllers are driving adapters and belong in adapter.web")
                .check(classes);
    }

    @Test
    @DisplayName("Services should be in application.service package")
    void servicesShouldBeInApplicationPackage() {
        classes()
                .that().haveSimpleNameEndingWith("Service")
                .and().areNotInterfaces()
                .should().resideInAPackage("..application.service..")
                .because("Service implementations belong in application layer")
                .check(classes);
    }

    @Test
    @DisplayName("JPA entities should only be in adapter.persistence.entity package")
    void jpaEntitiesShouldBeInAdapterPackage() {
        classes()
                .that().areAnnotatedWith(jakarta.persistence.Entity.class)
                .should().resideInAPackage("..adapter.persistence.entity..")
                .because("JPA entities are infrastructure concerns")
                .check(classes);
    }

    @Test
    @DisplayName("Layered architecture should be respected")
    void layeredArchitectureShouldBeRespected() {
        layeredArchitecture()
                .consideringOnlyDependenciesInLayers()
                .layer("Domain").definedBy("..domain..")
                .layer("Application").definedBy("..application..")
                .layer("Adapter").definedBy("..adapter..")
                .layer("Config").definedBy("..config..")
                .layer("DTO").definedBy("..dto..")
                .layer("Exception").definedBy("..exception..")
                .whereLayer("Domain").mayNotAccessAnyLayer()
                .whereLayer("Application").mayOnlyAccessLayers("Domain")
                .whereLayer("Adapter").mayOnlyAccessLayers("Domain", "Application", "DTO", "Exception")
                .because("Hexagonal architecture dependencies must flow inward")
                .check(classes);
    }
}
