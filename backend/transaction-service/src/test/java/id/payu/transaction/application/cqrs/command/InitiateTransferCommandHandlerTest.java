package id.payu.transaction.application.cqrs.command;

import id.payu.transaction.adapter.persistence.entity.TransactionEntity;
import id.payu.transaction.domain.model.Money;
import id.payu.transaction.domain.model.TransactionStatus;
import id.payu.transaction.domain.model.TransactionType;
import id.payu.transaction.domain.port.out.BifastServicePort;
import id.payu.transaction.domain.port.out.RgsServicePort;
import id.payu.transaction.domain.port.out.SknServicePort;
import id.payu.transaction.domain.port.out.TransactionEventPublisherPort;
import id.payu.transaction.domain.port.out.TransactionPersistencePort;
import id.payu.transaction.domain.port.out.WalletServicePort;
import id.payu.transaction.application.service.AuthorizationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InitiateTransferCommandHandlerTest {

    @Mock
    private TransactionPersistencePort transactionPersistencePort;
    @Mock
    private WalletServicePort walletServicePort;
    @Mock
    private BifastServicePort bifastServicePort;
    @Mock
    private SknServicePort sknServicePort;
    @Mock
    private RgsServicePort rgsServicePort;
    @Mock
    private TransactionEventPublisherPort eventPublisherPort;
    @Mock
    private AuthorizationService authorizationService;

    @InjectMocks
    private InitiateTransferCommandHandler handler;

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
        when(transactionPersistencePort.findByReferenceNumber("TXN-CALLBACK-001"))
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
        when(transactionPersistencePort.findByReferenceNumber("TXN-CALLBACK-002"))
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
    void submitsSknTransferToClearingAdapter() {
        UUID transactionId = UUID.randomUUID();
        InitiateTransferCommand command = new InitiateTransferCommand(
                UUID.randomUUID(),
                "1234567890",
                Money.idr("100000"),
                "test transfer",
                id.payu.transaction.dto.TransactionType.SKN_TRANSFER,
                null,
                null,
                "idem-skn-001",
                "user-001");
        when(walletServicePort.reserveBalance(
                eq(command.senderAccountId()), eq(transactionId.toString()), eq(command.amount().getAmount())))
                .thenReturn(id.payu.transaction.dto.ReserveBalanceResponse.builder()
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
}
