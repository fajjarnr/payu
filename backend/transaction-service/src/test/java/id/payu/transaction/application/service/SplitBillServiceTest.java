package id.payu.transaction.application.service;

import id.payu.transaction.adapter.persistence.entity.SplitBillEntity;
import id.payu.transaction.adapter.persistence.entity.SplitBillParticipantEntity;
import id.payu.transaction.domain.model.ParticipantStatus;
import id.payu.transaction.domain.model.SplitStatus;
import id.payu.transaction.domain.model.SplitType;
import id.payu.transaction.domain.port.in.SplitBillUseCase;
import id.payu.transaction.domain.port.out.SplitBillEventPublisherPort;
import id.payu.transaction.domain.port.out.SplitBillPersistencePort;
import id.payu.transaction.interfaces.dto.CreateSplitBillRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TXN-SPLIT-001 regression tests: a split bill must only be considered fully
 * paid when the total collected equals the total bill amount, and custom splits
 * must validate that the sum of participant obligations equals the total.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SplitBillService (TXN-SPLIT-001) Tests")
class SplitBillServiceTest {

    @Mock
    private SplitBillPersistencePort persistencePort;

    @Mock
    private SplitBillEventPublisherPort eventPublisher;

    @InjectMocks
    private SplitBillService splitBillService;

    @Test
    @DisplayName("isFullyPaid returns false when all participants paid their share but total collected < bill total")
    void isFullyPaidFalseWhenTotalCollectedBelowBillTotal() {
        // Given - total bill is 1,000,000 but custom shares only add up to 600,000
        // and every participant has paid their (too-small) share in full
        SplitBillEntity splitBill = SplitBillEntity.builder()
                .totalAmount(new BigDecimal("1000000.0000"))
                .splitType(SplitType.CUSTOM)
                .status(SplitStatus.ACTIVE)
                .build();

        SplitBillParticipantEntity a = participant(new BigDecimal("300000.0000"), new BigDecimal("300000.0000"));
        SplitBillParticipantEntity b = participant(new BigDecimal("300000.0000"), new BigDecimal("300000.0000"));
        splitBill.setParticipants(List.of(a, b));

        // When/Then - NOT fully paid: 600k collected, 400k still missing
        assertFalse(splitBill.isFullyPaid());
        assertEquals(0, new BigDecimal("400000.0000").compareTo(splitBill.getRemainingAmount()));
    }

    @Test
    @DisplayName("isFullyPaid returns true when sum of paid equals bill total")
    void isFullyPaidTrueWhenTotalCollectedEqualsBillTotal() {
        // Given - bill 1,000,000, shares sum to 1,000,000, all paid
        SplitBillEntity splitBill = SplitBillEntity.builder()
                .totalAmount(new BigDecimal("1000000.0000"))
                .splitType(SplitType.CUSTOM)
                .status(SplitStatus.ACTIVE)
                .build();

        SplitBillParticipantEntity a = participant(new BigDecimal("600000.0000"), new BigDecimal("600000.0000"));
        SplitBillParticipantEntity b = participant(new BigDecimal("400000.0000"), new BigDecimal("400000.0000"));
        splitBill.setParticipants(List.of(a, b));

        assertTrue(splitBill.isFullyPaid());
    }

    @Test
    @DisplayName("createSplitBill rejects CUSTOM split where participant amounts do not sum to total")
    void createSplitBillRejectsMismatchedCustomSplit() {
        CreateSplitBillRequest request = new CreateSplitBillRequest();
        request.setCreatorAccountId(UUID.randomUUID());
        request.setTotalAmount(new BigDecimal("1000000.0000"));
        request.setCurrency("IDR");
        request.setTitle("Custom lunch");
        request.setSplitType(SplitType.CUSTOM);
        request.setParticipants(List.of(
                CreateSplitBillRequest.ParticipantRequest.builder()
                        .accountId(UUID.randomUUID()).accountNumber("111").accountName("A")
                        .amountOwed(new BigDecimal("300000.0000")).build(),
                CreateSplitBillRequest.ParticipantRequest.builder()
                        .accountId(UUID.randomUUID()).accountNumber("222").accountName("B")
                        .amountOwed(new BigDecimal("200000.0000")).build()));

        assertThrows(IllegalArgumentException.class, () -> splitBillService.createSplitBill(request));
    }

    @Test
    @DisplayName("createSplitBill accepts CUSTOM split where participant amounts sum exactly to total")
    void createSplitBillAcceptsMatchingCustomSplit() {
        CreateSplitBillRequest request = new CreateSplitBillRequest();
        request.setCreatorAccountId(UUID.randomUUID());
        request.setTotalAmount(new BigDecimal("1000000.0000"));
        request.setCurrency("IDR");
        request.setTitle("Custom lunch");
        request.setSplitType(SplitType.CUSTOM);
        request.setParticipants(List.of(
                CreateSplitBillRequest.ParticipantRequest.builder()
                        .accountId(UUID.randomUUID()).accountNumber("111").accountName("A")
                        .amountOwed(new BigDecimal("600000.0000")).build(),
                CreateSplitBillRequest.ParticipantRequest.builder()
                        .accountId(UUID.randomUUID()).accountNumber("222").accountName("B")
                        .amountOwed(new BigDecimal("400000.0000")).build()));

        SplitBillEntity saved = SplitBillEntity.builder()
                .id(UUID.randomUUID())
                .creatorAccountId(request.getCreatorAccountId())
                .totalAmount(request.getTotalAmount())
                .currency("IDR")
                .title(request.getTitle())
                .splitType(SplitType.CUSTOM)
                .status(SplitStatus.DRAFT)
                .build();
        org.mockito.Mockito.when(persistencePort.save(org.mockito.ArgumentMatchers.any()))
                .thenReturn(saved);

        splitBillService.createSplitBill(request);
        org.mockito.Mockito.verify(persistencePort, org.mockito.Mockito.atLeastOnce()).save(org.mockito.ArgumentMatchers.any());
    }

    private SplitBillParticipantEntity participant(BigDecimal amountOwed, BigDecimal amountPaid) {
        return SplitBillParticipantEntity.builder()
                .id(UUID.randomUUID())
                .amountOwed(amountOwed)
                .amountPaid(amountPaid)
                .status(amountPaid.compareTo(amountOwed) >= 0 ? ParticipantStatus.SETTLED : ParticipantStatus.PARTIALLY_PAID)
                .build();
    }
}
