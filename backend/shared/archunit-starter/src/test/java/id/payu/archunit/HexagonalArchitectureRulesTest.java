package id.payu.archunit;

import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link HexagonalArchitectureRules} to ensure all rules are properly defined.
 *
 * @author PayU Architecture Team
 */
@DisplayName("HexagonalArchitectureRules Unit Tests")
class HexagonalArchitectureRulesTest {

    @Test
    @DisplayName("Should provide all core rules")
    void shouldProvideAllCoreRules() {
        List<ArchRule> allRules = HexagonalArchitectureRules.allRules();

        assertThat(allRules)
                .isNotEmpty()
                .hasSizeGreaterThanOrEqualTo(5);
    }

    @Test
    @DisplayName("Domain should not depend on infrastructure rule should be defined")
    void domainShouldNotDependOnInfrastructureRuleShouldBeDefined() {
        ArchRule rule = HexagonalArchitectureRules.domainShouldNotDependOnInfrastructure();

        assertThat(rule).isNotNull();
        assertThat(rule.getDescription())
                .containsIgnoringCase("domain")
                .containsIgnoringCase("infrastructure");
    }

    @Test
    @DisplayName("Domain should not depend on application rule should be defined")
    void domainShouldNotDependOnApplicationRuleShouldBeDefined() {
        ArchRule rule = HexagonalArchitectureRules.domainShouldNotDependOnApplication();

        assertThat(rule).isNotNull();
        assertThat(rule.getDescription())
                .containsIgnoringCase("domain")
                .containsIgnoringCase("application");
    }

    @Test
    @DisplayName("Application should only access repositories through ports rule should be defined")
    void applicationShouldOnlyAccessRepositoriesThroughPortsRuleShouldBeDefined() {
        ArchRule rule = HexagonalArchitectureRules.applicationShouldOnlyAccessRepositoriesThroughPorts();

        assertThat(rule).isNotNull();
        assertThat(rule.getDescription())
                .containsIgnoringCase("application")
                .containsIgnoringCase("port");
    }

    @Test
    @DisplayName("Adapters should not leak into domain rule should be defined")
    void adaptersShouldNotLeakIntoDomainRuleShouldBeDefined() {
        ArchRule rule = HexagonalArchitectureRules.adaptersShouldNotLeakIntoDomain();

        assertThat(rule).isNotNull();
        assertThat(rule.getDescription())
                .containsIgnoringCase("adapter")
                .containsIgnoringCase("domain");
    }

    @Test
    @DisplayName("Use cases should be transactional rule should be defined")
    void useCasesShouldBeTransactionalRuleShouldBeDefined() {
        ArchRule rule = HexagonalArchitectureRules.useCasesShouldBeTransactional();

        assertThat(rule).isNotNull();
        assertThat(rule.getDescription())
                .containsIgnoringCase("transactional");
    }

    @Test
    @DisplayName("State modifying use cases should be transactional rule should be defined")
    void stateModifyingUseCasesShouldBeTransactionalRuleShouldBeDefined() {
        ArchRule rule = HexagonalArchitectureRules.stateModifyingUseCasesShouldBeTransactional();

        assertThat(rule).isNotNull();
        assertThat(rule.getDescription())
                .containsIgnoringCase("transactional");
    }

    @Test
    @DisplayName("Repositories should not return entities rule should be defined")
    void repositoriesShouldNotReturnEntitiesRuleShouldBeDefined() {
        ArchRule rule = HexagonalArchitectureRules.repositoriesShouldNotReturnEntities();

        assertThat(rule).isNotNull();
        assertThat(rule.getDescription())
                .containsIgnoringCase("domain")
                .containsIgnoringCase("entities");
    }

    @Test
    @DisplayName("Ports should be interfaces rule should be defined")
    void portsShouldBeInterfacesRuleShouldBeDefined() {
        ArchRule rule = HexagonalArchitectureRules.portsShouldBeInterfaces();

        assertThat(rule).isNotNull();
        assertThat(rule.getDescription())
                .containsIgnoringCase("port")
                .containsIgnoringCase("interface");
    }

    @Test
    @DisplayName("JPA entities should be in infrastructure rule should be defined")
    void jpaEntitiesShouldBeInInfrastructureRuleShouldBeDefined() {
        ArchRule rule = HexagonalArchitectureRules.jpaEntitiesShouldBeInInfrastructure();

        assertThat(rule).isNotNull();
        assertThat(rule.getDescription())
                .containsIgnoringCase("entity")
                .containsIgnoringCase("infrastructure");
    }
}
