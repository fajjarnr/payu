package id.payu.wallet.integration;

import id.payu.outbox.entity.OutboxEvent;
import id.payu.outbox.repository.OutboxRepository;
import id.payu.outbox.service.OutboxService;
import id.payu.wallet.adapter.persistence.entity.RefundReversalExecutionEntity;
import id.payu.wallet.domain.model.RefundReversalStatus;
import id.payu.wallet.adapter.persistence.repository.RefundReversalExecutionRepository;
import id.payu.wallet.config.TestcontainersConfig;
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
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * QAMVP-013: transactional outbox atomicity against real PostgreSQL (via
 * Testcontainers) — the business row (wallet_transactions) and the outbox row
 * must commit together and roll back together. No mocks.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = "payu.grpc.server.port=0")
@Import({TestcontainersConfig.class, OutboxAtomicityIntegrationTest.TxProbeConfig.class})
@DisplayName("QAMVP-013 — outbox atomicity with business row (real PostgreSQL)")
class OutboxAtomicityIntegrationTest {

    @Autowired
    private TxProbe txProbe;

    @Autowired
    private OutboxRepository outboxRepository;

    @Test
    @DisplayName("commit persists business row + outbox row together")
    void commitPersistsBusinessRowAndOutboxRow() {
        String referenceId = UUID.randomUUID().toString();

        txProbe.commitBoth(referenceId);

        assertThat(outboxRepository.findByAggregateId(referenceId)).hasSize(1);
        assertThat(txProbe.countByRefundId(referenceId)).isEqualTo(1);
    }

    @Test
    @DisplayName("rollback leaves neither business row nor outbox row")
    void rollbackLeavesNoRows() {
        String referenceId = UUID.randomUUID().toString();

        assertThatThrownBy(() -> txProbe.rollbackBoth(referenceId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("rollback");

        assertThat(outboxRepository.findByAggregateId(referenceId))
                .as("rolled-back transaction must not leave an outbox row")
                .isEmpty();
        assertThat(txProbe.countByRefundId(referenceId))
                .as("rolled-back transaction must not leave a business row")
                .isZero();
    }

    @TestConfiguration
    static class TxProbeConfig {
        @Bean
        TxProbe txProbe(RefundReversalExecutionRepository reversalRepository,
                        OutboxService outboxService) {
            return new TxProbe(reversalRepository, outboxService);
        }
    }

    /**
     * Inserts a wallet_transactions business row and an outbox event inside one
     * transaction, exposing commitBoth/rollbackBoth for atomicity assertions.
     */
    public static class TxProbe {

        private final RefundReversalExecutionRepository reversalRepository;
        private final OutboxService outboxService;

        TxProbe(RefundReversalExecutionRepository reversalRepository, OutboxService outboxService) {
            this.reversalRepository = reversalRepository;
            this.outboxService = outboxService;
        }

        @Transactional
        public void commitBoth(String referenceId) {
            insert(referenceId);
        }

        @Transactional(propagation = Propagation.REQUIRES_NEW)
        public void rollbackBoth(String referenceId) {
            insert(referenceId);
            throw new IllegalStateException("trigger rollback");
        }

        private void insert(String referenceId) {
            RefundReversalExecutionEntity entity = new RefundReversalExecutionEntity();
            entity.setRefundId(UUID.fromString(referenceId));
            entity.setTransactionId(UUID.randomUUID());
            entity.setAmount(new BigDecimal("10000.0000"));
            entity.setCurrency("IDR");
            entity.setStatus(RefundReversalStatus.COMPLETED);
            reversalRepository.save(entity);

            outboxService.createEvent(
                    "WalletTransaction",
                    referenceId,
                    "TopUpCompleted",
                    Map.of("referenceId", referenceId),
                    null,
                    "payu.wallet.topup-completed.v1");
        }

        public long countByRefundId(String referenceId) {
            return reversalRepository.countByRefundId(UUID.fromString(referenceId));
        }
    }
}
