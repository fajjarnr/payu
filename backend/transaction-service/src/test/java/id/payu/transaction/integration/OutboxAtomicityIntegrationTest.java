package id.payu.transaction.integration;

import id.payu.outbox.repository.OutboxRepository;
import id.payu.outbox.service.OutboxService;
import id.payu.transaction.adapter.persistence.entity.VaPaymentRecordEntity;
import id.payu.transaction.adapter.persistence.repository.VaPaymentRecordRepository;
import id.payu.transaction.config.TestcontainersConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * QAMVP-013 (transaction): transactional outbox atomicity against real
 * PostgreSQL (Testcontainers) — business row (transaction_archives) and outbox
 * row commit together and roll back together. No mocks.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {"payu.grpc.server.port=0", "spring.jpa.hibernate.ddl-auto=none"})
@Import({TestcontainersConfig.class, OutboxAtomicityIntegrationTest.TxProbeConfig.class})
@DisplayName("QAMVP-013 — transaction outbox atomicity (real PostgreSQL)")
class OutboxAtomicityIntegrationTest {

    @Autowired
    private TxProbe txProbe;

    @Autowired
    private OutboxRepository outboxRepository;

    @Test
    @DisplayName("commit persists business row + outbox row together")
    void commitPersistsBusinessRowAndOutboxRow() {
        String referenceNumber = "ARCH-" + UUID.randomUUID().toString().substring(0, 8);

        txProbe.commitBoth(referenceNumber);

        assertThat(outboxRepository.findByAggregateId(referenceNumber)).hasSize(1);
        assertThat(txProbe.countByReferenceNumber(referenceNumber)).isEqualTo(1);
    }

    @Test
    @DisplayName("rollback leaves neither business row nor outbox row")
    void rollbackLeavesNoRows() {
        String referenceNumber = "ARCH-" + UUID.randomUUID().toString().substring(0, 8);

        assertThatThrownBy(() -> txProbe.rollbackBoth(referenceNumber))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("rollback");

        assertThat(outboxRepository.findByAggregateId(referenceNumber))
                .as("rolled-back transaction must not leave an outbox row")
                .isEmpty();
        assertThat(txProbe.countByReferenceNumber(referenceNumber))
                .as("rolled-back transaction must not leave a business row")
                .isZero();
    }

    @TestConfiguration
    static class TxProbeConfig {
        @Bean
        TxProbe txProbe(VaPaymentRecordRepository vaPaymentRecordRepository,
                        OutboxService outboxService) {
            return new TxProbe(vaPaymentRecordRepository, outboxService);
        }
    }

    /**
     * Inserts a transaction_archives business row and an outbox event inside one
     * transaction, exposing commitBoth/rollbackBoth for atomicity assertions.
     */
    public static class TxProbe {

        private final VaPaymentRecordRepository vaPaymentRecordRepository;
        private final OutboxService outboxService;

        TxProbe(VaPaymentRecordRepository vaPaymentRecordRepository, OutboxService outboxService) {
            this.vaPaymentRecordRepository = vaPaymentRecordRepository;
            this.outboxService = outboxService;
        }

        @Transactional
        public void commitBoth(String referenceNumber) {
            insert(referenceNumber);
        }

        @Transactional(propagation = Propagation.REQUIRES_NEW)
        public void rollbackBoth(String referenceNumber) {
            insert(referenceNumber);
            throw new IllegalStateException("trigger rollback");
        }

        private void insert(String referenceNumber) {
            VaPaymentRecordEntity entity = VaPaymentRecordEntity.of(
                    UUID.randomUUID(),
                    referenceNumber,
                    new BigDecimal("10000.0000"),
                    referenceNumber,
                    Instant.now());
            vaPaymentRecordRepository.save(entity);

            outboxService.createEvent(
                    "TransactionArchive",
                    referenceNumber,
                    "TransferArchived",
                    Map.of("referenceNumber", referenceNumber),
                    null,
                    "payu.transaction.transfer-archived.v1");
        }

        public long countByReferenceNumber(String referenceNumber) {
            return vaPaymentRecordRepository.countByPaymentReference(referenceNumber);
        }
    }
}
