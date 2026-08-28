package id.payu.partner.application.service;

import id.payu.partner.adapter.persistence.entity.SnapBiPaymentEntity;
import id.payu.partner.adapter.persistence.entity.SnapBiRefundEntity;
import id.payu.partner.adapter.persistence.entity.SnapReconciliationCaseEntity;
import id.payu.partner.adapter.persistence.repository.SnapBiPaymentRepository;
import id.payu.partner.adapter.persistence.repository.SnapBiRefundRepository;
import id.payu.partner.adapter.persistence.repository.SnapReconciliationCaseRepository;
import id.payu.partner.domain.port.out.WalletSettlementPort;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * PARTNER-PROD-005: SNAP payment/refund vs wallet-ledger reconciliation.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SnapBiReconciliationServiceTest {

    @Mock private SnapBiPaymentRepository paymentRepository;
    @Mock private SnapBiRefundRepository refundRepository;
    @Mock private SnapReconciliationCaseRepository caseRepository;
    @Mock private WalletSettlementPort walletSettlementPort;

    private SnapBiReconciliationService service;

    @BeforeEach
    void setUp() {
        service = new SnapBiReconciliationService(paymentRepository, refundRepository,
                caseRepository, walletSettlementPort);
        service.setWindowHours(24);
    }

    private SnapBiPaymentEntity payment(String ref, String status) {
        SnapBiPaymentEntity p = new SnapBiPaymentEntity(ref, "1", "PR-" + ref, new BigDecimal("100.00"),
                "IDR", "ACC-002", "002", "ACC-001", status);
        return p;
    }

    private SnapBiRefundEntity refund(String payuRefundNo, String status) {
        SnapBiRefundEntity r = new SnapBiRefundEntity(payuRefundNo, "1", "PAYU-P1", "PR1",
                "PRF-" + payuRefundNo, new BigDecimal("100.00"), "IDR", "test", status);
        return r;
    }

    private WalletSettlementPort.LedgerMovement movement(String ref, String refType, String entryType) {
        return new WalletSettlementPort.LedgerMovement("ACC-001", ref, refType, entryType,
                new BigDecimal("100.00"), new BigDecimal("500.0000"));
    }

    @Test
    @DisplayName("COMPLETED payment with both ledger legs creates no case")
    void matchedPaymentNoCase() {
        SnapBiPaymentEntity p = payment("PAYU-P1", "COMPLETED");
        when(paymentRepository.findByCreatedAtAfter(any(Instant.class)))
                .thenReturn(List.of(p));
        when(refundRepository.findByCreatedAtAfter(any(Instant.class)))
                .thenReturn(List.of());
        when(walletSettlementPort.ledgerMovementsByReferences(any()))
                .thenReturn(List.of(
                        movement("PAYU-P1", "RESERVATION", "DEBIT"),
                        movement("PAYU-P1", "CREDIT", "CREDIT")));

        service.reconcileSnapMovements();

        verify(caseRepository, never()).save(any());
    }

    @Test
    @DisplayName("COMPLETED payment missing the debit leg creates a PAYMENT case")
    void paymentMissingDebitCreatesCase() {
        SnapBiPaymentEntity p = payment("PAYU-P2", "COMPLETED");
        when(paymentRepository.findByCreatedAtAfter(any(Instant.class)))
                .thenReturn(List.of(p));
        when(refundRepository.findByCreatedAtAfter(any(Instant.class)))
                .thenReturn(List.of());
        when(walletSettlementPort.ledgerMovementsByReferences(any()))
                .thenReturn(List.of(movement("PAYU-P2", "CREDIT", "CREDIT")));

        service.reconcileSnapMovements();

        ArgumentCaptor<SnapReconciliationCaseEntity> captor = ArgumentCaptor.forClass(SnapReconciliationCaseEntity.class);
        verify(caseRepository).save(captor.capture());
        assertEquals("PAYMENT", captor.getValue().getReferenceType());
        assertEquals("PAYU-P2", captor.getValue().getReferenceId());
    }

    @Test
    @DisplayName("COMPLETED payment with no ledger movements creates a PAYMENT case")
    void paymentWithoutMovementsCreatesCase() {
        SnapBiPaymentEntity p = payment("PAYU-P3", "COMPLETED");
        when(paymentRepository.findByCreatedAtAfter(any(Instant.class)))
                .thenReturn(List.of(p));
        when(refundRepository.findByCreatedAtAfter(any(Instant.class)))
                .thenReturn(List.of());
        when(walletSettlementPort.ledgerMovementsByReferences(any())).thenReturn(List.of());

        service.reconcileSnapMovements();

        ArgumentCaptor<SnapReconciliationCaseEntity> captor = ArgumentCaptor.forClass(SnapReconciliationCaseEntity.class);
        verify(caseRepository).save(captor.capture());
        assertEquals("PAYMENT", captor.getValue().getReferenceType());
        assertEquals("PAYU-P3", captor.getValue().getReferenceId());
    }

    @Test
    @DisplayName("COMPLETED refund with REFUND_REVERSAL movement creates no case")
    void matchedRefundNoCase() {
        SnapBiRefundEntity r = refund("REFUND-11111111-2222-3333-4444-555555555555", "COMPLETED");
        when(paymentRepository.findByCreatedAtAfter(any(Instant.class)))
                .thenReturn(List.of());
        when(refundRepository.findByCreatedAtAfter(any(Instant.class)))
                .thenReturn(List.of(r));
        when(walletSettlementPort.ledgerMovementsByReferences(any()))
                .thenReturn(List.of(movement("11111111-2222-3333-4444-555555555555", "REFUND_REVERSAL", "DEBIT")));

        service.reconcileSnapMovements();

        verify(caseRepository, never()).save(any());
    }

    @Test
    @DisplayName("COMPLETED refund without REFUND_REVERSAL movement creates a REFUND case")
    void refundWithoutReversalCreatesCase() {
        SnapBiRefundEntity r = refund("REFUND-11111111-2222-3333-4444-555555555555", "COMPLETED");
        when(paymentRepository.findByCreatedAtAfter(any(Instant.class)))
                .thenReturn(List.of());
        when(refundRepository.findByCreatedAtAfter(any(Instant.class)))
                .thenReturn(List.of(r));
        when(walletSettlementPort.ledgerMovementsByReferences(any())).thenReturn(List.of());

        service.reconcileSnapMovements();

        ArgumentCaptor<SnapReconciliationCaseEntity> captor = ArgumentCaptor.forClass(SnapReconciliationCaseEntity.class);
        verify(caseRepository).save(captor.capture());
        assertEquals("REFUND", captor.getValue().getReferenceType());
        assertEquals("REFUND-11111111-2222-3333-4444-555555555555", captor.getValue().getReferenceId());
    }

    @Test
    @DisplayName("ledger movement without a COMPLETED partner record creates a WALLET_MOVEMENT case")
    void orphanWalletMovementCreatesCase() {
        SnapBiPaymentEntity pending = payment("PAYU-P4", "PENDING");
        when(paymentRepository.findByCreatedAtAfter(any(Instant.class)))
                .thenReturn(List.of(pending));
        when(refundRepository.findByCreatedAtAfter(any(Instant.class)))
                .thenReturn(List.of());
        when(walletSettlementPort.ledgerMovementsByReferences(any()))
                .thenReturn(List.of(
                        movement("PAYU-P4", "RESERVATION", "DEBIT"),
                        movement("PAYU-P4", "CREDIT", "CREDIT")));

        service.reconcileSnapMovements();

        ArgumentCaptor<SnapReconciliationCaseEntity> captor = ArgumentCaptor.forClass(SnapReconciliationCaseEntity.class);
        verify(caseRepository).save(captor.capture());
        assertEquals("WALLET_MOVEMENT", captor.getValue().getReferenceType());
        assertEquals("PAYU-P4", captor.getValue().getReferenceId());
    }

    @Test
    @DisplayName("already-open case is not duplicated")
    void existingCaseNotDuplicated() {
        SnapBiPaymentEntity p = payment("PAYU-P5", "COMPLETED");
        when(paymentRepository.findByCreatedAtAfter(any(Instant.class)))
                .thenReturn(List.of(p));
        when(refundRepository.findByCreatedAtAfter(any(Instant.class)))
                .thenReturn(List.of());
        when(walletSettlementPort.ledgerMovementsByReferences(any())).thenReturn(List.of());
        when(caseRepository.existsByReferenceTypeAndReferenceId("PAYMENT", "PAYU-P5")).thenReturn(true);

        service.reconcileSnapMovements();

        verify(caseRepository, never()).save(any());
    }

    @Test
    @DisplayName("non-SNAP references (INVESTMENT/other) do not create cases")
    void unrelatedMovementsIgnored() {
        when(paymentRepository.findByCreatedAtAfter(any())).thenReturn(List.of());
        when(refundRepository.findByCreatedAtAfter(any())).thenReturn(List.of());
        when(walletSettlementPort.ledgerMovementsByReferences(any())).thenReturn(List.of(
                movement("INVESTMENT-001", "COMMIT", "DEBIT")
        ));
        service.reconcileSnapMovements();
        verify(caseRepository, never()).save(any());
    }

    @Test
    @DisplayName("GLOBAL-RECON: auto-resolves PAYMENT case when ledger catch-up within 5m")
    void autoResolvesPaymentCaseWithin5m() {
        String ref = "PAYU-AUTO-001";
        SnapReconciliationCaseEntity existing = new SnapReconciliationCaseEntity(
                SnapReconciliationCaseEntity.TYPE_PAYMENT, ref, "missing legs");
        // existing is OPEN and detectedAt is now (within 5m)
        when(caseRepository.findByReferenceTypeAndReferenceId(
                SnapReconciliationCaseEntity.TYPE_PAYMENT, ref))
                .thenReturn(java.util.Optional.of(existing));
        SnapBiPaymentEntity payment = payment(ref, "COMPLETED");
        when(paymentRepository.findByCreatedAtAfter(any())).thenReturn(List.of(payment));
        when(refundRepository.findByCreatedAtAfter(any())).thenReturn(List.of());
        when(walletSettlementPort.ledgerMovementsByReferences(any())).thenReturn(List.of(
                movement(ref, "COMMIT", "DEBIT"),
                movement(ref, "CREDIT", "CREDIT")
        ));
        service.reconcileSnapMovements();
        assertEquals("RESOLVED", existing.getStatus());
        verify(caseRepository).save(existing);
    }
}
