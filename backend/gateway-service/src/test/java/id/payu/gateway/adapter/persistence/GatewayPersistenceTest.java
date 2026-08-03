package id.payu.gateway.adapter.persistence;

import id.payu.gateway.domain.repository.PartnerRatePlanRepository;
import id.payu.gateway.domain.repository.RatePlanRepository;
import id.payu.gateway.domain.repository.TransformationRuleRepository;
import id.payu.gateway.domain.entity.PartnerRatePlan;
import id.payu.gateway.domain.entity.RatePlan;
import id.payu.gateway.domain.entity.TransformationRule;
import id.payu.gateway.domain.vo.RateLimit;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
class GatewayPersistenceTest {

    @Inject
    RatePlanRepository ratePlanRepository;

    @Inject
    PartnerRatePlanRepository partnerRatePlanRepository;

    @Inject
    TransformationRuleRepository transformationRuleRepository;

    @Inject
    DataSource dataSource;

    @Test
    void startsWithoutDemoRatePlansAssignmentsOrTransformationRules() {
        assertTrue(ratePlanRepository.findById("default").await().indefinitely().isEmpty());
        assertTrue(partnerRatePlanRepository.findEffectiveByPartnerId("tokobapak")
            .await().indefinitely().isEmpty());
        assertTrue(transformationRuleRepository.findById("security-headers")
            .await().indefinitely().isEmpty());
    }

    @Test
    void persistsRatePlansAssignmentsAndTransformationRules() {
        RatePlan plan = new RatePlan("test-plan", "Test Plan", "test", RateLimit.of(10, 100, 1000));
        plan.addEndpointOverride("/payments/*", RateLimit.of(2, 20, 200));
        ratePlanRepository.save(plan).await().indefinitely();

        RatePlan loadedPlan = ratePlanRepository.findById("test-plan").await().indefinitely().orElseThrow();
        assertEquals(2, loadedPlan.getEffectiveLimit("/payments/1").requestsPerMinute());

        PartnerRatePlan assignment = new PartnerRatePlan("assignment-1", "test-partner", "test-plan");
        partnerRatePlanRepository.save(assignment).await().indefinitely();
        assertEquals("test-plan", partnerRatePlanRepository.findEffectiveByPartnerId("test-partner")
            .await().indefinitely().orElseThrow().getRatePlanId());

        TransformationRule rule = new TransformationRule("test-rule", "Test Rule", "test", 10);
        transformationRuleRepository.save(rule).await().indefinitely();
        assertTrue(transformationRuleRepository.findById("test-rule").await().indefinitely().isPresent());
    }

    @Test
    void recordsConfigurationChangesInAnAuditTable() throws SQLException {
        ratePlanRepository.save(new RatePlan("audit-plan", "Audit Plan", "audit", RateLimit.of(1, 2, 3)))
            .await().indefinitely();

        try (var connection = dataSource.getConnection();
             var statement = connection.prepareStatement(
                 "SELECT COUNT(*) FROM gateway_configuration_audit WHERE entity_id = ?")) {
            statement.setString(1, "audit-plan");
            try (var resultSet = statement.executeQuery()) {
                resultSet.next();
                assertEquals(1, resultSet.getLong(1));
            }
        }
    }
}
