package id.payu.transaction.application.cqrs.command;

import id.payu.transaction.adapter.persistence.entity.TransactionEntity;
import id.payu.transaction.application.service.AuthorizationService;
import id.payu.transaction.domain.model.Money;
import id.payu.transaction.domain.model.TransactionStatus;
import id.payu.transaction.domain.model.TransactionType;
import id.payu.transaction.domain.port.out.QrisServicePort;
import id.payu.transaction.domain.port.out.TransactionEventPublisherPort;
import id.payu.transaction.domain.port.out.TransactionPersistencePort;
import id.payu.transaction.domain.port.out.WalletServicePort;
import id.payu.transaction.dto.QrisPaymentResponse;
import id.payu.transaction.dto.ReserveBalanceResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProcessQrisPaymentCommandHandlerTest {

    @Mock
    private TransactionPersistencePort transactionPersistencePort;
    @Mock
    private QrisServicePort qrisServicePort;
    @Mock
    private WalletServicePort walletServicePort;
    @Mock
    private TransactionEventPublisherPort eventPublisherPort;
    @Mock
    private AuthorizationService authorizationService;

    private ProcessQrisPaymentCommandHandler handler;

    @BeforeEach
    void setUp() {
        handler = new ProcessQrisPaymentCommandHandler(
                transactionPersistencePort, qrisServicePort, walletServicePort,
                eventPublisherPort, authorizationService);
    }

    private ProcessQrisPaymentCommand command(String idempotencyKey) {
        return new ProcessQrisPaymentCommand(
                "12345678", Money.idr("50000"), "user-001", UUID.randomUUID(), idempotencyKey);
    }

    @Test
    void rejectsReplayWithSameIdempotencyKeyWithoutReservingBalance() {
        ProcessQrisPaymentCommand command = command("qris-replay-key-001");
        TransactionEntity existing = TransactionEntity.builder()
                .id(UUID.randomUUID())
                .status(TransactionStatus.COMPLETED)
                .build();
        when(transactionPersistencePort.findByIdempotencyKey("qris-replay-key-001"))
                .thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> handler.handle(command))
                .isInstanceOf(id.payu.api.common.exception.BusinessException.class)
                .hasMessageContaining("Idempotency");
        verify(walletServicePort, never()).reserveBalance(any(), any(), any());
        verify(walletServicePort, never()).commitBalance(any(), any(), any(), any());
    }

    @Test
    void persistsIdempotencyKeyOnTransactionForDbFallbackDedupe() {
        ProcessQrisPaymentCommand command = command("qris-db-key-002");
        UUID transactionId = UUID.randomUUID();
        when(transactionPersistencePort.findByIdempotencyKey("qris-db-key-002"))
                .thenReturn(Optional.empty());
        when(transactionPersistencePort.save(any(TransactionEntity.class)))
                .thenAnswer(inv -> {
                    TransactionEntity t = inv.getArgument(0);
                    if (t.getId() == null) {
                        t.setId(transactionId);
                    }
                    return t;
                });
        when(walletServicePort.reserveBalance(any(), any(), any()))
                .thenReturn(ReserveBalanceResponse.builder().reservationId("r-1").status("RESERVED").build());
        when(qrisServicePort.processPayment(any()))
                .thenReturn(QrisPaymentResponse.builder().status("SUCCESS").build());

        handler.handle(command);

        ArgumentCaptor<TransactionEntity> captor = ArgumentCaptor.forClass(TransactionEntity.class);
        verify(transactionPersistencePort, org.mockito.Mockito.atLeastOnce()).save(captor.capture());
        assertThat(captor.getAllValues())
                .anyMatch(t -> "qris-db-key-002".equals(t.getIdempotencyKey()));
    }
}
