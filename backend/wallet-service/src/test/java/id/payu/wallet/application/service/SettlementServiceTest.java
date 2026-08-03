package id.payu.wallet.application.service;

import id.payu.wallet.domain.model.RevenueSplit;
import id.payu.wallet.domain.model.SettlementBatch;
import id.payu.wallet.domain.model.SettlementStatus;
import id.payu.wallet.domain.model.SplitPaymentExecution;
import id.payu.wallet.domain.model.SplitType;
import id.payu.wallet.domain.model.Stakeholder;
import id.payu.wallet.domain.port.out.JournalPersistencePort;
import id.payu.wallet.domain.port.out.SettlementPersistencePort;
import id.payu.wallet.domain.port.in.SplitPaymentUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SettlementServiceTest {

    @Mock
    private SettlementPersistencePort settlementPersistencePort;

    @Mock
    private JournalPersistencePort journalPersistencePort;

    @Mock
    private SplitPaymentUseCase splitPaymentUseCase;

    private SettlementService settlementService;

    @BeforeEach
    void setUp() {
        settlementService = new SettlementService(
                settlementPersistencePort,
                journalPersistencePort,
                splitPaymentUseCase,
                Clock.fixed(Instant.parse("2026-08-03T00:00:00Z"), ZoneOffset.UTC)
        );
    }

    @Test
    void generateRoyaltyStatementAggregatesCompletedSettlementForAccountAndPeriod() {
        LocalDate settlementDate = LocalDate.of(2026, 7, 15);
        SettlementBatch completed = settlementBatch(settlementDate, SettlementStatus.COMPLETED, "10000.00");
        SettlementBatch pending = settlementBatch(LocalDate.of(2026, 7, 20), SettlementStatus.PENDING, "4000.00");

        Stakeholder stakeholder = Stakeholder.builder()
                .id(UUID.randomUUID())
                .accountId("account-1")
                .name("Partner royalty")
                .percentage(new BigDecimal("12.5"))
                .fixedAmount(BigDecimal.ZERO)
                .priority(1)
                .build();
        RevenueSplit split = RevenueSplit.builder()
                .partnerId("partner-1")
                .name("Default split")
                .splitType(SplitType.PERCENTAGE)
                .stakeholders(new ArrayList<>(List.of(stakeholder)))
                .active(true)
                .effectiveFrom(LocalDateTime.of(2026, 1, 1, 0, 0))
                .build();

        when(settlementPersistencePort.findSettlementBatchesByPartner(
                "partner-1", LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31)))
                .thenReturn(List.of(completed, pending));
        when(settlementPersistencePort.findRevenueSplitsByPartner("partner-1"))
                .thenReturn(List.of(split));

        String statement = settlementService.generateRoyaltyStatement("partner-1", "account-1", 2026, 7);

        assertThat(statement)
                .contains("Revenue Split: Default split")
                .contains("Amount: 1250.00")
                .contains("Total Royalties: 1250.00")
                .doesNotContain("4000.00");
    }

    @Test
    void applyRevenueSplitDelegatesToDurableSplitExecution() {
        UUID batchId = UUID.randomUUID();
        UUID splitId = UUID.randomUUID();
        SettlementBatch batch = settlementBatch(LocalDate.of(2026, 8, 3), SettlementStatus.PROCESSING, "100.00");
        batch.setId(batchId);
        RevenueSplit split = RevenueSplit.builder()
                .id(splitId)
                .partnerId("partner-1")
                .name("Fixed split")
                .splitType(SplitType.FIXED)
                .stakeholders(new ArrayList<>(List.of(Stakeholder.builder()
                        .accountId("recipient-1")
                        .name("Recipient")
                        .fixedAmount(new BigDecimal("100.00"))
                        .priority(1)
                        .build())))
                .active(true)
                .build();
        when(settlementPersistencePort.findSettlementBatchById(batchId)).thenReturn(java.util.Optional.of(batch));
        when(settlementPersistencePort.findRevenueSplitById(splitId)).thenReturn(java.util.Optional.of(split));
        when(splitPaymentUseCase.executeAdHocSplit(
                eq("partner-1"), eq("partner-1"), eq(new BigDecimal("100.00")), eq("IDR"),
                any(), eq(batchId.toString()), eq("Revenue split"), eq("SETTLEMENT:" + batchId)))
                .thenReturn(SplitPaymentExecution.builder()
                        .status(id.payu.wallet.domain.model.SplitExecutionStatus.COMPLETED).build());

        settlementService.applyRevenueSplit(batchId, splitId);

        verify(splitPaymentUseCase).executeAdHocSplit(
                eq("partner-1"), eq("partner-1"), eq(new BigDecimal("100.00")), eq("IDR"),
                any(), eq(batchId.toString()), eq("Revenue split"), eq("SETTLEMENT:" + batchId));
    }

    private static SettlementBatch settlementBatch(LocalDate date, SettlementStatus status, String netAmount) {
        return SettlementBatch.builder()
                .id(UUID.randomUUID())
                .partnerId("partner-1")
                .settlementDate(date)
                .currency("IDR")
                .totalAmount(new BigDecimal(netAmount))
                .feeAmount(BigDecimal.ZERO)
                .netAmount(new BigDecimal(netAmount))
                .status(status)
                .entries(List.of())
                .build();
    }
}
