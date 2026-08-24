package id.payu.transaction.application.cqrs.command;

import id.payu.api.common.exception.RateLimitExceededException;
import id.payu.transaction.adapter.persistence.entity.TransactionEntity;
import id.payu.transaction.application.service.VelocityGuard;
import id.payu.transaction.domain.model.Money;
import id.payu.transaction.domain.model.TransactionStatus;
import id.payu.transaction.domain.model.TransactionType;
import id.payu.transaction.domain.port.out.BifastServicePort;
import id.payu.transaction.domain.port.out.RiskEvaluationPort;
import id.payu.transaction.domain.port.out.RgsServicePort;
import id.payu.transaction.domain.port.out.SknServicePort;
import id.payu.transaction.domain.port.out.TransactionEventPublisherPort;
import id.payu.transaction.domain.port.out.TransactionPersistencePort;
import id.payu.transaction.domain.port.out.WalletServicePort;
import id.payu.transaction.application.service.AuthorizationService;
import id.payu.transaction.exception.TransactionDomainException;
import id.payu.transaction.interfaces.dto.ReserveBalanceResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class InitiateTransferCommandHandlerTest {

    @Mock
    private TransactionPersistencePort transactionPersistencePort;
    @Mock
    private BifastServicePort bifastServicePort;
    @Mock
    private WalletServicePort walletServicePort;
    @Mock
    private SknServicePort sknServicePort;
    @Mock
    private RgsServicePort rgsServicePort;
    @Mock
    private TransactionEventPublisherPort eventPublisherPort;
    @Mock
    private AuthorizationService authorizationService;

    // ADR-0030 enforcement collaborators — lenient: not every test exercises the guard path
    @Mock(strictness = org.mockito.Mock.Strictness.LENIENT)
    private VelocityGuard velocityGuard;
    @Mock(strictness = org.mockito.Mock.Strictness.LENIENT)
    private RiskEvaluationPort riskEvaluationPort;

    @InjectMocks
    private InitiateTransferCommandHandler handler;

    @BeforeEach
    void allowRiskPathByDefault() {
        when(velocityGuard.isAllowed(anyString(), any(java.math.BigDecimal.class))).thenReturn(true);
        when(riskEvaluationPort.score(anyString(), any(java.math.BigDecimal.class), any())).thenReturn(0);
    }

    @Test
    void completesInterbankTransferFromProviderCallback() {

        UUID transactionId = UUID.randomUUID();
        UUID senderAccountId = UUID.randomUUID();
        TransactionEntity transaction = TransactionEntity.builder()
                .id(transactionId)
                .referenceNumber("TXN-CALLBACK-001")
                .senderAccountId(senderAccountId)
                .amount(Money.idr("100000"))
                .type(TransactionType.BIFAST_TRANSFER)
                .status(TransactionStatus.PENDING)
                .reservationId("reservation-001")
                .build();
        when(transactionPersistencePort.findByReferenceNumberForUpdate("TXN-CALLBACK-001"))
                .thenReturn(List.of(transaction));
        when(transactionPersistencePort.save(transaction)).thenReturn(transaction);

        TransactionEntity result = handler.settleInterbankTransfer(
                "TXN-CALLBACK-001", "COMPLETED", null);

        assertThat(result.getStatus()).isEqualTo(TransactionStatus.COMPLETED);
        assertThat(result.getCompletedAt()).isNotNull();
        verify(walletServicePort).commitBalance(
                eq(senderAccountId), eq(transactionId.toString()), eq("reservation-001"), eq(Money.idr("100000").getAmount()));
        verify(eventPublisherPort).publishTransactionCompleted(transaction);
    }

    @Test
    void releasesReservationFromFailedProviderCallback() {
        UUID transactionId = UUID.randomUUID();
        UUID senderAccountId = UUID.randomUUID();
        TransactionEntity transaction = TransactionEntity.builder()
                .id(transactionId)
                .referenceNumber("TXN-CALLBACK-002")
                .senderAccountId(senderAccountId)
                .amount(Money.idr("100000"))
                .type(TransactionType.SKN_TRANSFER)
                .status(TransactionStatus.PENDING)
                .reservationId("reservation-002")
                .build();
        when(transactionPersistencePort.findByReferenceNumberForUpdate("TXN-CALLBACK-002"))
                .thenReturn(List.of(transaction));
        when(transactionPersistencePort.save(transaction)).thenReturn(transaction);

        TransactionEntity result = handler.settleInterbankTransfer(
                "TXN-CALLBACK-002", "FAILED", "Beneficiary account rejected");

        assertThat(result.getStatus()).isEqualTo(TransactionStatus.FAILED);
        assertThat(result.getFailureReason()).isEqualTo("Beneficiary account rejected");
        verify(walletServicePort).releaseBalance(
                eq(senderAccountId), eq(transactionId.toString()), eq("reservation-002"), eq(Money.idr("100000").getAmount()));
        verify(eventPublisherPort).publishTransactionFailed(transaction, "Beneficiary account rejected");
    }

    @Test
    void doubleCompletedCallbackCommitsAndPublishesOnce() {
        UUID transactionId = UUID.randomUUID();
        UUID senderAccountId = UUID.randomUUID();
        TransactionEntity transaction = TransactionEntity.builder()
                .id(transactionId)
                .referenceNumber("TXN-CALLBACK-003")
                .senderAccountId(senderAccountId)
                .amount(Money.idr("100000"))
                .type(TransactionType.BIFAST_TRANSFER)
                .status(TransactionStatus.PENDING)
                .reservationId("reservation-003")
                .build();
        when(transactionPersistencePort.findByReferenceNumberForUpdate("TXN-CALLBACK-003"))
                .thenReturn(List.of(transaction));
        when(transactionPersistencePort.save(transaction)).thenReturn(transaction);

        handler.settleInterbankTransfer("TXN-CALLBACK-003", "COMPLETED", null);
        TransactionEntity second = handler.settleInterbankTransfer("TXN-CALLBACK-003", "COMPLETED", null);

        assertThat(second.getStatus()).isEqualTo(TransactionStatus.COMPLETED);
        verify(walletServicePort, times(1)).commitBalance(
                eq(senderAccountId), eq(transactionId.toString()), eq("reservation-003"), eq(Money.idr("100000").getAmount()));
        verify(eventPublisherPort, times(1)).publishTransactionCompleted(transaction);
    }

    @Test
    void submitsSknTransferToClearingAdapter() {
        UUID transactionId = UUID.randomUUID();
        InitiateTransferCommand command = new InitiateTransferCommand(
                UUID.randomUUID(),
                "1234567890",
                Money.idr("100000"),
                "test transfer",
                id.payu.transaction.interfaces.dto.TransactionType.SKN_TRANSFER,
                null,
                null,
                "idem-skn-001",
                "user-001",
                null);
        when(walletServicePort.reserveBalance(
                eq(command.senderAccountId()), eq(transactionId.toString()), eq(command.amount().getAmount())))
                .thenReturn(id.payu.transaction.interfaces.dto.ReserveBalanceResponse.builder()
                        .reservationId("reservation-skn-001")
                        .status("RESERVED")
                        .build());
        when(transactionPersistencePort.save(org.mockito.ArgumentMatchers.any(TransactionEntity.class)))
                .thenAnswer(invocation -> {
                    TransactionEntity transaction = invocation.getArgument(0);
                    if (transaction.getId() == null) {
                        transaction.setId(transactionId);
                    }
                    return transaction;
                });

        InitiateTransferCommandResult result = handler.handle(command);

        assertThat(result.status()).isEqualTo(TransactionStatus.PENDING.name());
        verify(sknServicePort).initiateTransfer(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void usesBankCodeFromRequestForBiFastTransfer() throws Exception {
        UUID transactionId = UUID.randomUUID();
        UUID senderAccountId = UUID.randomUUID();
        InitiateTransferCommand command = new InitiateTransferCommand(
                senderAccountId,
                "1234567890",
                Money.idr("100000"),
                "test transfer",
                id.payu.transaction.interfaces.dto.TransactionType.BIFAST_TRANSFER,
                null,
                null,
                "idem-bifast-bankcode-001",
                "user-001",
                "002");
        when(walletServicePort.reserveBalance(
                eq(senderAccountId), eq(transactionId.toString()), eq(Money.idr("100000").getAmount())))
                .thenReturn(id.payu.transaction.interfaces.dto.ReserveBalanceResponse.builder()
                        .reservationId("reservation-bifast-002")
                        .status("RESERVED")
                        .build());
        when(transactionPersistencePort.save(any(TransactionEntity.class)))
                .thenAnswer(invocation -> {
                    TransactionEntity transaction = invocation.getArgument(0);
                    if (transaction.getId() == null) {
                        transaction.setId(transactionId);
                    }
                    return transaction;
                });

        handler.handle(command);

        var captured = org.mockito.ArgumentCaptor.forClass(id.payu.transaction.interfaces.dto.BifastTransferRequest.class);
        verify(bifastServicePort).initiateTransfer(captured.capture());
        assertThat(captured.getValue().getBeneficiaryBankCode()).isEqualTo("002");
    }

    @Test
    void defaultsToBank014WhenBankCodeAbsent() throws Exception {
        UUID transactionId = UUID.randomUUID();
        UUID senderAccountId = UUID.randomUUID();
        InitiateTransferCommand command = new InitiateTransferCommand(
                senderAccountId,
                "1234567890",
                Money.idr("100000"),
                "test transfer",
                id.payu.transaction.interfaces.dto.TransactionType.BIFAST_TRANSFER,
                null,
                null,
                "idem-bifast-default-001",
                "user-001",
                null);
        when(walletServicePort.reserveBalance(
                eq(senderAccountId), eq(transactionId.toString()), eq(Money.idr("100000").getAmount())))
                .thenReturn(id.payu.transaction.interfaces.dto.ReserveBalanceResponse.builder()
                        .reservationId("reservation-bifast-014")
                        .status("RESERVED")
                        .build());
        when(transactionPersistencePort.save(any(TransactionEntity.class)))
                .thenAnswer(invocation -> {
                    TransactionEntity transaction = invocation.getArgument(0);
                    if (transaction.getId() == null) {
                        transaction.setId(transactionId);
                    }
                    return transaction;
                });

        handler.handle(command);

        var captured = org.mockito.ArgumentCaptor.forClass(id.payu.transaction.interfaces.dto.BifastTransferRequest.class);
        verify(bifastServicePort).initiateTransfer(captured.capture());
        assertThat(captured.getValue().getBeneficiaryBankCode()).isEqualTo("014");
    }

    @Test
    void reportsZeroFeeWhenFeeNotCollected() {
        UUID transactionId = UUID.randomUUID();
        UUID senderAccountId = UUID.randomUUID();
        InitiateTransferCommand command = new InitiateTransferCommand(
                senderAccountId,
                "1234567890",
                Money.idr("100000"),
                "test transfer",
                id.payu.transaction.interfaces.dto.TransactionType.BIFAST_TRANSFER,
                null,
                null,
                "idem-bifast-fee-001",
                "user-001",
                "014");
        when(walletServicePort.reserveBalance(
                eq(senderAccountId), eq(transactionId.toString()), eq(Money.idr("100000").getAmount())))
                .thenReturn(id.payu.transaction.interfaces.dto.ReserveBalanceResponse.builder()
                        .reservationId("reservation-bifast-fee-001")
                        .status("RESERVED")
                        .build());
        when(transactionPersistencePort.save(any(TransactionEntity.class)))
                .thenAnswer(invocation -> {
                    TransactionEntity transaction = invocation.getArgument(0);
                    if (transaction.getId() == null) {
                        transaction.setId(transactionId);
                    }
                    return transaction;
                });

        InitiateTransferCommandResult result = handler.handle(command);

        assertThat(result.fee()).isEqualTo(java.math.BigDecimal.ZERO);
    }

    @Test
    void usesAtomicTransferForInternalTransfer() {
        UUID transactionId = UUID.randomUUID();
        UUID senderAccountId = UUID.randomUUID();
        String recipientAccountNumber = "1234567890";
        InitiateTransferCommand command = new InitiateTransferCommand(
                senderAccountId,
                recipientAccountNumber,
                Money.idr("100"),
                "internal transfer",
                id.payu.transaction.interfaces.dto.TransactionType.INTERNAL_TRANSFER,
                null,
                null,
                "idem-internal-atomic-001",
                "user-001",
                null);
        when(transactionPersistencePort.save(any(TransactionEntity.class)))
                .thenAnswer(invocation -> {
                    TransactionEntity transaction = invocation.getArgument(0);
                    if (transaction.getId() == null) {
                        transaction.setId(transactionId);
                    }
                    return transaction;
                });

        InitiateTransferCommandResult result = handler.handle(command);

        assertThat(result.status()).isEqualTo(TransactionStatus.COMPLETED.name());
        verify(walletServicePort).transferBalance(
                eq(senderAccountId.toString()), eq(recipientAccountNumber),
                eq(Money.idr("100").getAmount()), eq(transactionId.toString()));
        verify(walletServicePort, never()).reserveBalance(any(UUID.class), anyString(), any(java.math.BigDecimal.class));
        verify(walletServicePort, never()).commitBalance(any(UUID.class), anyString(), anyString(), any(java.math.BigDecimal.class));
        verify(walletServicePort, never()).creditBalance(anyString(), anyString(), any(java.math.BigDecimal.class));
        verify(walletServicePort, never()).releaseBalance(any(UUID.class), anyString(), anyString(), any(java.math.BigDecimal.class));
    }

    @Test
    void marksInternalTransferFailedWhenAtomicTransferFails() {
        UUID transactionId = UUID.randomUUID();
        UUID senderAccountId = UUID.randomUUID();
        InitiateTransferCommand command = new InitiateTransferCommand(
                senderAccountId,
                "1234567890",
                Money.idr("100"),
                "internal transfer",
                id.payu.transaction.interfaces.dto.TransactionType.INTERNAL_TRANSFER,
                null,
                null,
                "idem-internal-atomic-fail-001",
                "user-001",
                null);
        when(transactionPersistencePort.save(any(TransactionEntity.class)))
                .thenAnswer(invocation -> {
                    TransactionEntity transaction = invocation.getArgument(0);
                    if (transaction.getId() == null) {
                        transaction.setId(transactionId);
                    }
                    return transaction;
                });
        doThrow(new RuntimeException("Insufficient available balance"))
                .when(walletServicePort).transferBalance(
                        eq(senderAccountId.toString()), eq("1234567890"),
                        eq(Money.idr("100").getAmount()), eq(transactionId.toString()));

        InitiateTransferCommandResult result = handler.handle(command);

        assertThat(result.status()).isEqualTo(TransactionStatus.FAILED.name());
        verify(walletServicePort, never()).releaseBalance(any(UUID.class), anyString(), anyString(), any(java.math.BigDecimal.class));
        verify(walletServicePort, never()).creditBalance(anyString(), anyString(), any(java.math.BigDecimal.class));
        verify(walletServicePort, never()).commitBalance(any(UUID.class), anyString(), anyString(), any(java.math.BigDecimal.class));
    }

    @Test
    void persistsRecipientAccountNumberForInternalTransferRefunds() {
        UUID transactionId = UUID.randomUUID();
        String recipientAccountNumber = "1234567890";
        InitiateTransferCommand command = new InitiateTransferCommand(
                UUID.randomUUID(),
                recipientAccountNumber,
                Money.idr("100"),
                "internal transfer",
                id.payu.transaction.interfaces.dto.TransactionType.INTERNAL_TRANSFER,
                null,
                null,
                "idem-internal-refund-001",
                "user-001",
                null);
        when(walletServicePort.transferBalance(
                eq(command.senderAccountId().toString()), eq("1234567890"),
                eq(command.amount().getAmount()), eq(transactionId.toString())))
                .thenReturn("ledger-tx-id");
        when(transactionPersistencePort.save(any(TransactionEntity.class)))
                .thenAnswer(invocation -> {
                    TransactionEntity transaction = invocation.getArgument(0);
                    if (transaction.getId() == null) {
                        transaction.setId(transactionId);
                    }
                    return transaction;
                });

        handler.handle(command);

        var savedTransactions = org.mockito.ArgumentCaptor.forClass(TransactionEntity.class);
        verify(transactionPersistencePort, atLeastOnce()).save(savedTransactions.capture());
        assertThat(savedTransactions.getAllValues())
                .anyMatch(transaction -> transaction.getMetadata() != null
                        && transaction.getMetadata().contains("\"recipientAccountNumber\":\"1234567890\""));
    }

    // === ADR-0030: real-time velocity & AML risk enforcement (B1.3) ===

    @Test
    void rejectsTransferWhenVelocityLimitExceeded() {
        InitiateTransferCommand command = bifastCommand("idem-vel-001");
        when(velocityGuard.isAllowed(eq("user-001"), eq(Money.idr("100000").getAmount()))).thenReturn(false);

        assertThatThrownBy(() -> handler.handle(command))
                .isInstanceOf(RateLimitExceededException.class)
                .hasMessageContaining("AML_VELOCITY");
        verify(walletServicePort, never()).reserveBalance(any(UUID.class), anyString(), any(java.math.BigDecimal.class));
        verify(transactionPersistencePort, never()).save(any(TransactionEntity.class));
    }

    @Test
    void blocksTransferWhenFraudScoreCritical() {
        InitiateTransferCommand command = bifastCommand("idem-risk-block-001");
        when(riskEvaluationPort.score(eq("user-001"), any(java.math.BigDecimal.class), any())).thenReturn(90);

        assertThatThrownBy(() -> handler.handle(command))
                .isInstanceOf(TransactionDomainException.AmlHighRiskBlockedException.class);
        verify(transactionPersistencePort, never()).save(any(TransactionEntity.class));
        verify(walletServicePort, never()).reserveBalance(any(UUID.class), anyString(), any(java.math.BigDecimal.class));
    }

    @Test
    void holdsTransferForComplianceReviewWhenScoreHigh() {
        UUID transactionId = UUID.randomUUID();
        InitiateTransferCommand command = bifastCommand("idem-risk-hold-001");
        when(riskEvaluationPort.score(eq("user-001"), any(java.math.BigDecimal.class), any())).thenReturn(78);
        when(transactionPersistencePort.save(any(TransactionEntity.class)))
                .thenAnswer(invocation -> {
                    TransactionEntity transaction = invocation.getArgument(0);
                    if (transaction.getId() == null) {
                        transaction.setId(transactionId);
                    }
                    return transaction;
                });

        InitiateTransferCommandResult result = handler.handle(command);

        assertThat(result.status()).isEqualTo(TransactionStatus.PENDING_COMPLIANCE_REVIEW.name());
        verify(walletServicePort, never()).reserveBalance(any(UUID.class), anyString(), any(java.math.BigDecimal.class));
        var saved = org.mockito.ArgumentCaptor.forClass(TransactionEntity.class);
        verify(transactionPersistencePort).save(saved.capture());
        assertThat(saved.getValue().getStatus()).isEqualTo(TransactionStatus.PENDING_COMPLIANCE_REVIEW);
    }

    @Test
    void failsClosedHoldingTransferWhenAnalyticsUnavailable() {
        // ADR-0030: analytics timeout is fail-safe to STEP_UP; with step-up unwired the transfer is held, never silently allowed
        UUID transactionId = UUID.randomUUID();
        InitiateTransferCommand command = bifastCommand("idem-risk-down-001");
        when(riskEvaluationPort.score(anyString(), any(java.math.BigDecimal.class), any()))
                .thenThrow(new TransactionDomainException.RiskEvaluationUnavailableException(
                        new java.net.ConnectException("analytics down")));
        when(transactionPersistencePort.save(any(TransactionEntity.class)))
                .thenAnswer(invocation -> {
                    TransactionEntity transaction = invocation.getArgument(0);
                    if (transaction.getId() == null) {
                        transaction.setId(transactionId);
                    }
                    return transaction;
                });

        InitiateTransferCommandResult result = handler.handle(command);

        assertThat(result.status()).isEqualTo(TransactionStatus.PENDING_COMPLIANCE_REVIEW.name());
        verify(walletServicePort, never()).reserveBalance(any(UUID.class), anyString(), any(java.math.BigDecimal.class));
    }

    private InitiateTransferCommand bifastCommand(String idempotencyKey) {
        return new InitiateTransferCommand(
                UUID.randomUUID(),
                "1234567890",
                Money.idr("100000"),
                "test transfer",
                id.payu.transaction.interfaces.dto.TransactionType.BIFAST_TRANSFER,
                null,
                null,
                idempotencyKey,
                "user-001",
                null);
    }
}
