package id.payu.transaction.integration;

import id.payu.transaction.adapter.persistence.entity.DisbursementEntity;
import id.payu.transaction.adapter.persistence.repository.DisbursementJpaRepository;
import id.payu.transaction.application.service.DisbursementService;
import id.payu.transaction.config.TestcontainersConfig;
import id.payu.transaction.domain.model.DisbursementStatus;
import id.payu.transaction.domain.model.Money;
import id.payu.transaction.domain.port.out.BifastServicePort;
import id.payu.transaction.domain.port.out.WalletServicePort;
import id.payu.transaction.interfaces.dto.BifastTransferResponse;
import id.payu.transaction.interfaces.dto.ReserveBalanceResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * QAMVP-010: disbursement journey against real PostgreSQL (Testcontainers).
 * Wallet/bifast ports mocked; persistence real.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {"payu.grpc.server.port=0", "spring.jpa.hibernate.ddl-auto=none"})
@Import(TestcontainersConfig.class)
@DisplayName("QAMVP-010 — disbursement journey (real PostgreSQL)")
class DisbursementServiceIntegrationTest {

    @Autowired
    private DisbursementService disbursementService;

    @Autowired
    private DisbursementJpaRepository disbursementJpaRepository;

    @MockitoBean
    private WalletServicePort walletServicePort;

    @MockitoBean
    private BifastServicePort bifastServicePort;

    private void stubWallet() {
        when(walletServicePort.reserveBalance(any(), any(), any()))
                .thenReturn(ReserveBalanceResponse.builder()
                        .reservationId("res-disb").status("RESERVED").build());
    }

    @Test
    @DisplayName("create → process → complete persists disbursement state")
    void disbursementJourney() throws Exception {
        stubWallet();
        when(bifastServicePort.initiateTransfer(any()))
                .thenReturn(BifastTransferResponse.builder()
                        .referenceNumber("BIFAST-DISB").status("SUCCESS").build());

        UUID accountId = UUID.randomUUID();
        String idempotencyKey = UUID.randomUUID().toString();

        DisbursementEntity created = disbursementService.createDisbursement(
                accountId, Money.idr("50000"), "011", "0123456789", "Payee",
                "salary payout", idempotencyKey);
        assertThat(created.getStatus()).isEqualTo(DisbursementStatus.PENDING);
        assertThat(created.getReservationId()).isEqualTo("res-disb");

        DisbursementEntity processed = disbursementService.processDisbursement(created.getId());
        assertThat(processed.getStatus()).isEqualTo(DisbursementStatus.PROCESSING);

        DisbursementEntity completed = disbursementService.completeDisbursement(created.getId(), "BANK-REF-1");
        assertThat(completed.getStatus()).isEqualTo(DisbursementStatus.COMPLETED);
        assertThat(completed.getBankReference()).isEqualTo("BANK-REF-1");

        DisbursementEntity persisted = disbursementJpaRepository.findById(created.getId()).orElseThrow();
        assertThat(persisted.getStatus()).isEqualTo(DisbursementStatus.COMPLETED);
        assertThat(persisted.getIdempotencyKey()).isEqualTo(idempotencyKey);
    }

    @Test
    @DisplayName("duplicate idempotency key returns existing disbursement (no double mutation)")
    void idempotencyKeyDeduplicates() throws Exception {
        stubWallet();

        UUID accountId = UUID.randomUUID();
        String idempotencyKey = UUID.randomUUID().toString();

        DisbursementEntity first = disbursementService.createDisbursement(
                accountId, Money.idr("25000"), "011", "0987654321", "Payee",
                null, idempotencyKey);
        DisbursementEntity second = disbursementService.createDisbursement(
                accountId, Money.idr("25000"), "011", "0987654321", "Payee",
                null, idempotencyKey);

        assertThat(second.getId()).isEqualTo(first.getId());
        assertThat(disbursementJpaRepository.countByIdempotencyKey(idempotencyKey)).isEqualTo(1);
    }
}
