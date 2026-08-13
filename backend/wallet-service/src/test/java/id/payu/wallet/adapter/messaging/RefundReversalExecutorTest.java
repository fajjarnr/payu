package id.payu.wallet.adapter.messaging;

import id.payu.wallet.domain.model.RefundReversalStatus;
import id.payu.wallet.domain.port.in.WalletUseCase;
import id.payu.wallet.adapter.persistence.entity.RefundReversalExecutionEntity;
import id.payu.wallet.adapter.persistence.repository.RefundReversalExecutionRepository;
import id.payu.wallet.interfaces.dto.RefundRequestedEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RefundReversalExecutorTest {

    private static final UUID REFUND_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440001");
    private static final UUID TRANSACTION_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440002");

    @Mock
    private RefundReversalExecutionRepository executionRepository;
    @Mock
    private WalletUseCase walletUseCase;

    @Test
    void execute_shouldPersistCompletedReversalAfterWalletTransfer() {
        RefundReversalExecutor executor = new RefundReversalExecutor(executionRepository, walletUseCase);
        when(executionRepository.findByRefundId(REFUND_ID)).thenReturn(Optional.empty());
        when(executionRepository.saveAndFlush(any(RefundReversalExecutionEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        RefundRequestedEvent event = new RefundRequestedEvent(
                REFUND_ID, TRANSACTION_ID, new BigDecimal("100.00"), "IDR", "customer refund", "sender", "recipient");

        executor.execute(event);

        verify(walletUseCase).reverseTransfer("sender", "recipient", new BigDecimal("100.00"), "IDR", REFUND_ID, "customer refund");
        ArgumentCaptor<RefundReversalExecutionEntity> saved = ArgumentCaptor.forClass(RefundReversalExecutionEntity.class);
        verify(executionRepository, times(2)).saveAndFlush(saved.capture());
        assertThat(saved.getAllValues().get(1).getStatus()).isEqualTo(RefundReversalStatus.COMPLETED);
    }

    @Test
    void execute_replayAfterCompletion_doesNotReverseMoneyAgain() {
        RefundReversalExecutor executor = new RefundReversalExecutor(executionRepository, walletUseCase);
        RefundReversalExecutionEntity completed = new RefundReversalExecutionEntity();
        completed.setRefundId(REFUND_ID);
        completed.setTransactionId(TRANSACTION_ID);
        completed.setStatus(RefundReversalStatus.COMPLETED);
        when(executionRepository.findByRefundId(REFUND_ID)).thenReturn(Optional.of(completed));

        RefundRequestedEvent event = new RefundRequestedEvent(
                REFUND_ID, TRANSACTION_ID, new BigDecimal("100.00"), "IDR", "customer refund", "sender", "recipient");

        executor.execute(event);

        verify(walletUseCase, never()).reverseTransfer(any(), any(), any(), any(), any(), any());
        verify(executionRepository, never()).saveAndFlush(any());
    }

    @Test
    void execute_invalidEvent_marksReconciliationRequired() {
        RefundReversalExecutor executor = new RefundReversalExecutor(executionRepository, walletUseCase);
        when(executionRepository.findByRefundId(REFUND_ID)).thenReturn(Optional.empty());
        when(executionRepository.saveAndFlush(any(RefundReversalExecutionEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        RefundRequestedEvent event = new RefundRequestedEvent(
                REFUND_ID, TRANSACTION_ID, null, "IDR", "reason", "sender", "recipient");

        executor.execute(event);

        verify(walletUseCase, never()).reverseTransfer(any(), any(), any(), any(), any(), any());
        ArgumentCaptor<RefundReversalExecutionEntity> saved = ArgumentCaptor.forClass(RefundReversalExecutionEntity.class);
        verify(executionRepository, atLeastOnce()).saveAndFlush(saved.capture());
        assertThat(saved.getAllValues().get(saved.getAllValues().size() - 1).getStatus())
                .isEqualTo(RefundReversalStatus.RECONCILIATION_REQUIRED);
    }
}
