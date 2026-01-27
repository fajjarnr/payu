package id.payu.transaction.unit;

import id.payu.transaction.application.service.TransactionService;
import id.payu.transaction.domain.model.Transaction;
import id.payu.transaction.domain.port.out.*;
import id.payu.transaction.dto.InitiateTransferRequest;
import id.payu.transaction.dto.InitiateTransferResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IdempotencyTest {

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
    private QrisServicePort qrisServicePort;
    @Mock
    private TransactionEventPublisherPort eventPublisherPort;

    @InjectMocks
    private TransactionService transactionService;

    @Test
    @DisplayName("should return existing transaction when idempotency key exists")
    void shouldReturnExistingTransaction_WhenIdempotencyKeyExists() {
        // Given
        String idempotencyKey = "key-123";
        InitiateTransferRequest request = InitiateTransferRequest.builder()
                .senderAccountId(UUID.randomUUID())
                .recipientAccountNumber("123456")
                .amount(new BigDecimal("50000"))
                .description("Test")
                .type(InitiateTransferRequest.TransactionType.INTERNAL_TRANSFER)
                .idempotencyKey(idempotencyKey)
                .build();

        Transaction existingTransaction = Transaction.builder()
                .id(UUID.randomUUID())
                .referenceNumber("TXN-EXISTING")
                .idempotencyKey(idempotencyKey)
                .status(Transaction.TransactionStatus.PENDING)
                .type(Transaction.TransactionType.INTERNAL_TRANSFER)
                .amount(new BigDecimal("50000"))
                .currency("IDR")
                .build();

        when(transactionPersistencePort.findByIdempotencyKey(idempotencyKey))
                .thenReturn(Optional.of(existingTransaction));

        // When
        InitiateTransferResponse response = transactionService.initiateTransfer(request);

        // Then
        assertThat(response.getReferenceNumber()).isEqualTo("TXN-EXISTING");
        verify(walletServicePort, never()).reserveBalance(any(), any(), any());
        verify(transactionPersistencePort, never()).save(any());
    }
}
