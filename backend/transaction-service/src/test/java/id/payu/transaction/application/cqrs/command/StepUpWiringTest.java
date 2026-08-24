package id.payu.transaction.application.cqrs.command;

import id.payu.transaction.adapter.persistence.entity.TransactionEntity;
import id.payu.transaction.application.service.AuthorizationService;
import id.payu.transaction.application.service.VelocityGuard;
import id.payu.transaction.domain.model.Money;
import id.payu.transaction.domain.model.TransactionStatus;
import id.payu.transaction.domain.port.out.*;
import id.payu.transaction.exception.TransactionDomainException.StepUpRequiredException;
import id.payu.transaction.exception.TransactionDomainException.StepUpVerificationFailedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class StepUpWiringTest {

    @Mock TransactionPersistencePort transactionPersistencePort;
    @Mock WalletServicePort walletServicePort;
    @Mock BifastServicePort bifastServicePort;
    @Mock SknServicePort sknServicePort;
    @Mock RgsServicePort rgsServicePort;
    @Mock TransactionEventPublisherPort eventPublisherPort;
    @Mock AuthorizationService authorizationService;
    @Mock VelocityGuard velocityGuard;
    @Mock RiskEvaluationPort riskEvaluationPort;
    @Mock StepUpVerificationPort stepUpVerificationPort;
    @Mock id.payu.transaction.application.service.InboxService inboxService;
    @Mock id.payu.transaction.application.service.AggregateResultService aggregateResultService;

    InitiateTransferCommandHandler handler;

    @BeforeEach
    void setUp() {
        handler = new InitiateTransferCommandHandler(
                transactionPersistencePort, walletServicePort, bifastServicePort,
                sknServicePort, rgsServicePort, eventPublisherPort,
                authorizationService, velocityGuard, riskEvaluationPort, stepUpVerificationPort, inboxService, aggregateResultService);
        ReflectionTestUtils.setField(handler, "stepUpAmountThreshold", new BigDecimal("10000000"));
        lenient().when(velocityGuard.isAllowed(anyString(), any(BigDecimal.class))).thenReturn(true);
        lenient().when(riskEvaluationPort.score(anyString(), any(BigDecimal.class), any())).thenReturn(0);
        lenient().when(transactionPersistencePort.save(any(TransactionEntity.class)))
                .thenAnswer(inv -> {
                    TransactionEntity e = inv.getArgument(0);
                    if (e.getId() == null) e.setId(UUID.randomUUID());
                    return e;
                });
    }

    // (a) transfer requiring step-up but no proof → 403 STEP_UP_REQUIRED
    @Test
    void requiresStepUpWhenRiskInStepUpBandWithoutProofThrows() {
        when(riskEvaluationPort.score(anyString(), any(BigDecimal.class), any())).thenReturn(55);

        InitiateTransferCommand cmd = new InitiateTransferCommand(
                UUID.randomUUID(), "1234567890", Money.idr("100000"),
                "test transfer", id.payu.transaction.interfaces.dto.TransactionType.BIFAST_TRANSFER,
                null, null, "idem-stepup-req-001", "user-001", null, null);

        assertThatThrownBy(() -> handler.handle(cmd))
                .isInstanceOf(StepUpRequiredException.class)
                .satisfies(e -> assertThat(((StepUpRequiredException) e).getCode()).isEqualTo("STEP_UP_REQUIRED"));
        verify(stepUpVerificationPort, never()).verify(anyString(), anyString(), anyString(), any(), anyString(), any(), anyString());
        verify(transactionPersistencePort, never()).save(any(TransactionEntity.class));
    }

    // (b) with valid dynamic linking proof → allowed
    @Test
    void allowsTransferWhenStepUpProofValidWithDynamicLinking() {
        when(riskEvaluationPort.score(anyString(), any(BigDecimal.class), any())).thenReturn(55);
        doNothing().when(stepUpVerificationPort).verify(anyString(), anyString(), anyString(), any(), anyString(), any(), anyString());
        when(walletServicePort.reserveBalance(any(UUID.class), anyString(), any(BigDecimal.class)))
                .thenReturn(id.payu.transaction.interfaces.dto.ReserveBalanceResponse.builder()
                        .reservationId("reservation-ok").status("RESERVED").build());

        InitiateTransferCommand cmd = new InitiateTransferCommand(
                UUID.randomUUID(), "1234567890", Money.idr("100000"),
                "test transfer", id.payu.transaction.interfaces.dto.TransactionType.BIFAST_TRANSFER,
                "123456", null, "idem-stepup-valid-001", "user-001", "014", "challenge-uuid-1");

        InitiateTransferCommandResult result = handler.handle(cmd);

        assertThat(result.status()).isEqualTo(TransactionStatus.PENDING.name());
        verify(stepUpVerificationPort).verify(eq("user-001"), eq("challenge-uuid-1"), eq("123456"),
                any(UUID.class), eq("1234567890"), argThat(a -> a != null && a.compareTo(new BigDecimal("100000")) == 0), eq("IDR"));
        verify(walletServicePort).reserveBalance(any(UUID.class), anyString(), argThat(a -> a != null && a.compareTo(new BigDecimal("100000")) == 0));
    }

    // (c) with invalid payee/amount mismatch → blocked (AUTH_CHALLENGE_TAMPERED)
    @Test
    void blocksTransferWhenDynamicLinkingMismatchTampered() {
        when(riskEvaluationPort.score(anyString(), any(BigDecimal.class), any())).thenReturn(60);
        doThrow(new StepUpVerificationFailedException("AUTH_CHALLENGE_TAMPERED", "payload digest mismatch"))
                .when(stepUpVerificationPort).verify(anyString(), anyString(), anyString(), any(), anyString(), any(), anyString());

        InitiateTransferCommand cmd = new InitiateTransferCommand(
                UUID.randomUUID(), "1234567890", Money.idr("100000"),
                "test transfer", id.payu.transaction.interfaces.dto.TransactionType.BIFAST_TRANSFER,
                "123456", null, "idem-stepup-tamper-001", "user-001", null, "challenge-tamper");

        assertThatThrownBy(() -> handler.handle(cmd))
                .isInstanceOf(StepUpVerificationFailedException.class)
                .satisfies(e -> assertThat(((StepUpVerificationFailedException) e).getCode()).isEqualTo("AUTH_CHALLENGE_TAMPERED"));
        verify(walletServicePort, never()).reserveBalance(any(UUID.class), anyString(), any(BigDecimal.class));
    }

    // bypass when low risk and low amount
    @Test
    void bypassesStepUpWhenLowRiskAndLowAmount() {
        when(riskEvaluationPort.score(anyString(), any(BigDecimal.class), any())).thenReturn(20);
        when(walletServicePort.reserveBalance(any(UUID.class), anyString(), any(BigDecimal.class)))
                .thenReturn(id.payu.transaction.interfaces.dto.ReserveBalanceResponse.builder()
                        .reservationId("res-bypass").status("RESERVED").build());

        InitiateTransferCommand cmd = bifastLowAmount("idem-bypass-001");

        InitiateTransferCommandResult result = handler.handle(cmd);

        assertThat(result.status()).isEqualTo(TransactionStatus.PENDING.name());
        verify(stepUpVerificationPort, never()).verify(anyString(), anyString(), anyString(), any(), anyString(), any(), anyString());
    }

    // amount threshold overrides low risk
    @Test
    void requiresStepUpWhenAmountExceedsThresholdEvenLowRisk() {
        when(riskEvaluationPort.score(anyString(), any(BigDecimal.class), any())).thenReturn(10);

        InitiateTransferCommand cmd = new InitiateTransferCommand(
                UUID.randomUUID(), "1234567890", Money.idr("50000000"),
                "large transfer", id.payu.transaction.interfaces.dto.TransactionType.BIFAST_TRANSFER,
                null, null, "idem-large-001", "user-001", null, null);

        assertThatThrownBy(() -> handler.handle(cmd))
                .isInstanceOf(StepUpRequiredException.class);
    }

    private InitiateTransferCommand bifastLowAmount(String idempotencyKey) {
        return new InitiateTransferCommand(
                UUID.randomUUID(), "1234567890", Money.idr("100000"),
                "test transfer", id.payu.transaction.interfaces.dto.TransactionType.BIFAST_TRANSFER,
                null, null, idempotencyKey, "user-001", null);
    }
}
