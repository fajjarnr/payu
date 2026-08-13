package id.payu.partner.integration;

import id.payu.outbox.repository.OutboxRepository;
import id.payu.outbox.service.OutboxService;
import id.payu.partner.adapter.persistence.entity.SnapBiRefundEntity;
import id.payu.partner.adapter.persistence.repository.SnapBiRefundRepository;
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
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * QAMVP-013 (partner): transactional outbox atomicity against real PostgreSQL
 * (Testcontainers). Business row uses {@code EntityManager.persist} because
 * partner entities carry {@code @Version} and {@code save()} (merge) treats a
 * zero-version row as detached.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {"spring.jpa.hibernate.ddl-auto=none", "partner.jwt.secret=test-jwt-secret"})
@Import({OutboxAtomicityIntegrationTest.PartnerTestcontainersConfig.class,
        OutboxAtomicityIntegrationTest.TxProbeConfig.class})
@DisplayName("QAMVP-013 — partner outbox atomicity (real PostgreSQL)")
class OutboxAtomicityIntegrationTest {

    @Autowired
    private TxProbe txProbe;

    @Autowired
    private OutboxRepository outboxRepository;

    @Test
    @DisplayName("commit persists business row + outbox row together")
    void commitPersistsBusinessRowAndOutboxRow() {
        String refundNo = "REF-" + UUID.randomUUID().toString().substring(0, 12);

        txProbe.commitBoth(refundNo);

        assertThat(outboxRepository.findByAggregateId(refundNo)).hasSize(1);
        assertThat(txProbe.countByPayuRefundNo(refundNo)).isEqualTo(1);
    }

    @Test
    @DisplayName("rollback leaves neither business row nor outbox row")
    void rollbackLeavesNoRows() {
        String refundNo = "REF-" + UUID.randomUUID().toString().substring(0, 12);

        assertThatThrownBy(() -> txProbe.rollbackBoth(refundNo))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("rollback");

        assertThat(outboxRepository.findByAggregateId(refundNo))
                .as("rolled-back transaction must not leave an outbox row")
                .isEmpty();
        assertThat(txProbe.countByPayuRefundNo(refundNo))
                .as("rolled-back transaction must not leave a business row")
                .isZero();
    }

    @org.springframework.boot.test.context.TestConfiguration
    static class PartnerTestcontainersConfig {
        @Bean
        @ServiceConnection
        PostgreSQLContainer<?> postgreSQLContainer() {
            return new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("partner_test")
                    .withUsername("test")
                    .withPassword("test")
                    .withReuse(true);
        }
    }

    @TestConfiguration
    static class TxProbeConfig {
        @Bean
        TxProbe txProbe(SnapBiRefundRepository refundRepository, OutboxService outboxService) {
            return new TxProbe(refundRepository, outboxService);
        }
    }

    public static class TxProbe {

        private final SnapBiRefundRepository refundRepository;
        private final OutboxService outboxService;

        @PersistenceContext
        private EntityManager entityManager;

        TxProbe(SnapBiRefundRepository refundRepository, OutboxService outboxService) {
            this.refundRepository = refundRepository;
            this.outboxService = outboxService;
        }

        @Transactional
        public void commitBoth(String refundNo) {
            insert(refundNo);
        }

        @Transactional(propagation = Propagation.REQUIRES_NEW)
        public void rollbackBoth(String refundNo) {
            insert(refundNo);
            throw new IllegalStateException("trigger rollback");
        }

        private void insert(String refundNo) {
            SnapBiRefundEntity entity = new SnapBiRefundEntity();
            entity.setPayuRefundNo(refundNo);
            entity.setPartnerId("partner-1");
            entity.setPayuReferenceNo("PAY-" + UUID.randomUUID());
            entity.setPartnerReferenceNo("PREF-" + UUID.randomUUID());
            entity.setAmount(new BigDecimal("10000.0000"));
            entity.setCurrency("IDR");
            entity.setReason("atomicity probe");
            entity.setStatus("COMPLETED");
            entity.setCreatedAt(Instant.now());
            entityManager.persist(entity);

            outboxService.createEvent(
                    "SnapBiRefund",
                    refundNo,
                    "RefundCompleted",
                    Map.of("refundNo", refundNo),
                    null,
                    "payu.partner.refund-completed.v1");
        }

        public long countByPayuRefundNo(String refundNo) {
            return refundRepository.countByPayuRefundNo(refundNo);
        }
    }
}
