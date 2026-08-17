package id.payu.billing.integration;

import id.payu.billing.infrastructure.persistence.entity.SubscriptionPlanEntity;
import id.payu.billing.adapter.persistence.repository.SubscriptionPlanRepository;
import id.payu.outbox.repository.OutboxRepository;
import id.payu.outbox.service.OutboxService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;

import java.math.BigDecimal;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * QAMVP-013 (billing): transactional outbox atomicity against real PostgreSQL
 * (Testcontainers). Business row uses {@code EntityManager.persist} because the
 * billing entities carry {@code @Version} and {@code save()} (merge) treats a
 * zero-version row as detached.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = "spring.jpa.hibernate.ddl-auto=none")
@Import({OutboxAtomicityIntegrationTest.BillingTestcontainersConfig.class,
        OutboxAtomicityIntegrationTest.TxProbeConfig.class})
@DisplayName("QAMVP-013 — billing outbox atomicity (real PostgreSQL)")
class OutboxAtomicityIntegrationTest {

    @Autowired
    private TxProbe txProbe;

    @Autowired
    private OutboxRepository outboxRepository;

    @Test
    @DisplayName("commit persists business row + outbox row together")
    void commitPersistsBusinessRowAndOutboxRow() {
        String planName = "PLAN-" + java.util.UUID.randomUUID().toString().substring(0, 8);

        txProbe.commitBoth(planName);

        assertThat(outboxRepository.findByAggregateId(planName)).hasSize(1);
        assertThat(txProbe.countByPlanName(planName)).isEqualTo(1);
    }

    @Test
    @DisplayName("rollback leaves neither business row nor outbox row")
    void rollbackLeavesNoRows() {
        String planName = "PLAN-" + java.util.UUID.randomUUID().toString().substring(0, 8);

        assertThatThrownBy(() -> txProbe.rollbackBoth(planName))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("rollback");

        assertThat(outboxRepository.findByAggregateId(planName))
                .as("rolled-back transaction must not leave an outbox row")
                .isEmpty();
        assertThat(txProbe.countByPlanName(planName))
                .as("rolled-back transaction must not leave a business row")
                .isZero();
    }

    @org.springframework.boot.test.context.TestConfiguration
    static class BillingTestcontainersConfig {
        @Bean
        @ServiceConnection
        PostgreSQLContainer<?> postgreSQLContainer() {
            return new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("billing_test")
                    .withUsername("test")
                    .withPassword("test");
        }
    }

    @TestConfiguration
    static class TxProbeConfig {
        @Bean
        TxProbe txProbe(SubscriptionPlanRepository planRepository, OutboxService outboxService) {
            return new TxProbe(planRepository, outboxService);
        }
    }

    public static class TxProbe {

        private final SubscriptionPlanRepository planRepository;
        private final OutboxService outboxService;

        @PersistenceContext
        private EntityManager entityManager;

        TxProbe(SubscriptionPlanRepository planRepository, OutboxService outboxService) {
            this.planRepository = planRepository;
            this.outboxService = outboxService;
        }

        @Transactional
        public void commitBoth(String planName) {
            insert(planName);
        }

        @Transactional(propagation = Propagation.REQUIRES_NEW)
        public void rollbackBoth(String planName) {
            insert(planName);
            throw new IllegalStateException("trigger rollback");
        }

        private void insert(String planName) {
            SubscriptionPlanEntity entity = new SubscriptionPlanEntity();
            entity.setPartnerId("partner-1");
            entity.setPlanName(planName);
            entity.setDescription("atomicity probe");
            entity.setBillingInterval("MONTHLY");
            entity.setPrice(new BigDecimal("10000.0000"));
            entity.setCurrency("IDR");
            entity.setActive(true);
            entityManager.persist(entity);

            outboxService.createEvent(
                    "SubscriptionPlan",
                    planName,
                    "PlanCreated",
                    Map.of("planName", planName),
                    null,
                    "payu.billing.plan-created.v1");
        }

        public long countByPlanName(String planName) {
            return planRepository.countByPlanName(planName);
        }
    }
}
