package id.payu.transaction.adapter.grpc;

import id.payu.transaction.adapter.persistence.entity.TransactionEntity;
import id.payu.transaction.domain.model.TransactionStatus;
import id.payu.transaction.domain.model.TransactionType;
import id.payu.transaction.domain.port.out.TransactionPersistencePort;
import id.payu.transaction.grpc.ExistsByReferenceRequest;
import id.payu.transaction.grpc.GetByReferenceRequest;
import id.payu.transaction.grpc.GetHistoryRequest;
import id.payu.transaction.grpc.GetTransactionRequest;
import id.payu.transaction.grpc.TransactionResponse;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * GRPC-002: TransactionService gRPC server read paths must work — the proto
 * previously had zero implementations (phantom contract).
 */
class TransactionGrpcServiceTest {

    private TransactionPersistencePort persistencePort;
    private TransactionGrpcService service;

    @BeforeEach
    void setUp() {
        persistencePort = mock(TransactionPersistencePort.class);
        service = new TransactionGrpcService(persistencePort);
    }

    private TransactionEntity tx(UUID id, String reference) {
        return TransactionEntity.builder()
                .id(id)
                .referenceNumber(reference)
                .senderAccountId(UUID.randomUUID())
                .recipientAccountId(UUID.randomUUID())
                .type(TransactionType.INTERNAL_TRANSFER)
                .status(TransactionStatus.COMPLETED)
                .amountValue(new BigDecimal("25000.0000"))
                .currencyCode("IDR")
                .description("test transfer")
                .createdAt(Instant.now())
                .build();
    }

    private static class RecordingObserver<T> implements StreamObserver<T> {
        T value;
        Throwable error;
        boolean completed;

        @Override
        public void onNext(T value) {
            this.value = value;
        }

        @Override
        public void onError(Throwable t) {
            this.error = t;
        }

        @Override
        public void onCompleted() {
            this.completed = true;
        }
    }

    @Test
    void getTransactionReturnsMappedTransaction() {
        UUID id = UUID.randomUUID();
        when(persistencePort.findById(id)).thenReturn(Optional.of(tx(id, "REF-1")));

        RecordingObserver<TransactionResponse> observer = new RecordingObserver<>();
        service.getTransaction(GetTransactionRequest.newBuilder().setTransactionId(id.toString()).build(), observer);

        assertThat(observer.error).isNull();
        assertThat(observer.value.getTransactionId()).isEqualTo(id.toString());
        assertThat(observer.value.getReferenceId()).isEqualTo("REF-1");
        assertThat(observer.value.getStatus().name()).isEqualTo("COMPLETED");
        assertThat(observer.value.getAmount().getAmount()).isEqualTo("25000.0000");
    }

    @Test
    void getTransactionUnknownReturnsNotFound() {
        UUID id = UUID.randomUUID();
        when(persistencePort.findById(id)).thenReturn(Optional.empty());

        RecordingObserver<TransactionResponse> observer = new RecordingObserver<>();
        service.getTransaction(GetTransactionRequest.newBuilder().setTransactionId(id.toString()).build(), observer);

        assertThat(observer.error).isInstanceOf(StatusRuntimeException.class);
        assertThat(((StatusRuntimeException) observer.error).getStatus().getCode())
                .isEqualTo(Status.Code.NOT_FOUND);
    }

    @Test
    void getByReferenceReturnsFirstMatch() {
        when(persistencePort.findByReferenceNumber("REF-1"))
                .thenReturn(List.of(tx(UUID.randomUUID(), "REF-1")));

        RecordingObserver<TransactionResponse> observer = new RecordingObserver<>();
        service.getByReference(GetByReferenceRequest.newBuilder().setReferenceId("REF-1").build(), observer);

        assertThat(observer.error).isNull();
        assertThat(observer.value.getReferenceId()).isEqualTo("REF-1");
    }

    @Test
    void getHistoryStreamsOnlyRequestedPage() {
        UUID accountId = UUID.randomUUID();
        when(persistencePort.findByAccountId(accountId, 1, 10))
                .thenReturn(List.of(tx(UUID.randomUUID(), "REF-2")));

        RecordingObserver<TransactionResponse> observer = new RecordingObserver<>();
        service.getHistory(GetHistoryRequest.newBuilder()
                .setAccountId(accountId.toString())
                .setPage(id.payu.grpc.common.PageRequest.newBuilder().setPage(1).setSize(10).build())
                .build(), observer);

        assertThat(observer.error).isNull();
        assertThat(observer.value.getReferenceId()).isEqualTo("REF-2");
        assertThat(observer.completed).isTrue();
    }

    @Test
    void existsByReferenceReportsPresence() {
        when(persistencePort.findByReferenceNumber("REF-1"))
                .thenReturn(List.of(tx(UUID.randomUUID(), "REF-1")));
        when(persistencePort.findByReferenceNumber("REF-MISSING")).thenReturn(List.of());

        RecordingObserver<id.payu.transaction.grpc.ExistsByReferenceResponse> found = new RecordingObserver<>();
        service.existsByReference(ExistsByReferenceRequest.newBuilder().setReferenceId("REF-1").build(), found);
        assertThat(found.value.getExists()).isTrue();

        RecordingObserver<id.payu.transaction.grpc.ExistsByReferenceResponse> missing = new RecordingObserver<>();
        service.existsByReference(ExistsByReferenceRequest.newBuilder().setReferenceId("REF-MISSING").build(), missing);
        assertThat(missing.value.getExists()).isFalse();
    }

    @Test
    void moneyWritesFailClosedWithUnimplemented() {
        RecordingObserver<TransactionResponse> observer = new RecordingObserver<>();
        service.createTransaction(
                id.payu.transaction.grpc.CreateTransactionRequest.getDefaultInstance(), observer);

        assertThat(observer.error).isInstanceOf(StatusRuntimeException.class);
        assertThat(((StatusRuntimeException) observer.error).getStatus().getCode())
                .isEqualTo(Status.Code.UNIMPLEMENTED);
    }
}
