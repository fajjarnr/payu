package id.payu.wallet.application.service;

import id.payu.wallet.domain.model.EntryType;
import id.payu.wallet.domain.model.SplitExecutionStatus;
import id.payu.wallet.domain.model.SplitPaymentExecution;
import id.payu.wallet.domain.model.SplitPaymentLeg;
import id.payu.wallet.domain.model.SplitRecipient;
import id.payu.wallet.domain.model.RecipientType;
import id.payu.wallet.domain.port.in.JournalUseCase;
import id.payu.wallet.domain.port.in.WalletUseCase;
import id.payu.wallet.domain.port.out.SplitPaymentPersistencePort;
import id.payu.wallet.domain.port.out.JournalPersistencePort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SplitPaymentServiceTest {

    @Mock
    private SplitPaymentPersistencePort persistencePort;
    @Mock
    private WalletUseCase walletUseCase;
    @Mock
    private JournalUseCase journalUseCase;

    private SplitPaymentService service;

    @BeforeEach
    void setUp() {
        service = new SplitPaymentService(persistencePort, walletUseCase, journalUseCase);
        when(persistencePort.saveExecution(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void failedLegIsPersistedForReconciliationAndEachLegUsesUniqueTransferReference() {
        when(persistencePort.findExecutionByIdempotencyKey("split-1")).thenReturn(Optional.empty());
        when(walletUseCase.transfer(anyString(), anyString(), any(BigDecimal.class), anyString(), anyString(), anyString()))
                .thenAnswer(invocation -> {
                    if ("recipient-2".equals(invocation.getArgument(1))) {
                        throw new IllegalStateException("recipient unavailable");
                    }
                    return "transfer-1";
                });

        SplitPaymentExecution execution = service.executeAdHocSplit(
                "payer", "partner", new BigDecimal("100.00"), "IDR", recipients(),
                "external-1", "split", "split-1");

        assertThat(execution.getStatus()).isEqualTo(SplitExecutionStatus.RECONCILIATION_REQUIRED);
        assertThat(execution.getLegs()).extracting(SplitPaymentLeg::getStatus)
                .containsExactly(
                        id.payu.wallet.domain.model.LegStatus.CREDITED,
                        id.payu.wallet.domain.model.LegStatus.FAILED);

        ArgumentCaptor<String> references = ArgumentCaptor.forClass(String.class);
        verify(walletUseCase, times(2)).transfer(
                eq("payer"), anyString(), any(BigDecimal.class), eq("IDR"), references.capture(), anyString());
        assertThat(references.getAllValues()).doesNotHaveDuplicates();
        verify(walletUseCase, never()).commitReservation(anyString());
        verify(walletUseCase, never()).releaseReservation(anyString());
    }

    @Test
    void reconciliationRetriesFailedLegAndCompletesJournal() {
        UUID executionId = UUID.randomUUID();
        SplitPaymentLeg credited = leg("recipient-1", "60.00", id.payu.wallet.domain.model.LegStatus.CREDITED);
        SplitPaymentLeg failed = leg("recipient-2", "40.00", id.payu.wallet.domain.model.LegStatus.FAILED);
        SplitPaymentExecution execution = SplitPaymentExecution.builder()
                .id(executionId)
                .payerAccountId("payer")
                .partnerId("partner")
                .totalAmount(new BigDecimal("100.00"))
                .currency("IDR")
                .status(SplitExecutionStatus.RECONCILIATION_REQUIRED)
                .description("split")
                .legs(new ArrayList<>(List.of(credited, failed)))
                .build();
        when(persistencePort.findExecutionsByStatusIn(anyCollection())).thenReturn(List.of(execution));
        when(walletUseCase.transfer(anyString(), eq("recipient-2"), any(BigDecimal.class), anyString(), anyString(), anyString()))
                .thenReturn("transfer-2");

        service.reconcile();

        assertThat(execution.getStatus()).isEqualTo(SplitExecutionStatus.COMPLETED);
        assertThat(execution.getLegs()).allMatch(leg -> leg.getStatus() == id.payu.wallet.domain.model.LegStatus.CREDITED);
        verify(journalUseCase).createAndPostJournal(anyString(), anyString(), eq(executionId.toString()), anyList(), anyString());
    }

    private static List<SplitRecipient> recipients() {
        return List.of(
                SplitRecipient.builder().recipientAccountId("recipient-1").recipientLabel("one")
                        .type(RecipientType.MERCHANT).fixedAmount(new BigDecimal("60.00")).build(),
                SplitRecipient.builder().recipientAccountId("recipient-2").recipientLabel("two")
                        .type(RecipientType.MERCHANT).fixedAmount(new BigDecimal("40.00")).build());
    }

    private static SplitPaymentLeg leg(String accountId, String amount, id.payu.wallet.domain.model.LegStatus status) {
        return SplitPaymentLeg.builder()
                .id(UUID.randomUUID())
                .executionId(UUID.randomUUID())
                .recipientAccountId(accountId)
                .amount(new BigDecimal(amount))
                .status(status)
                .build();
    }
}
