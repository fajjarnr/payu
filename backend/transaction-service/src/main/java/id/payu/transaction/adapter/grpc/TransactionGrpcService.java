package id.payu.transaction.adapter.grpc;

import id.payu.grpc.common.ErrorDetail;
import id.payu.grpc.common.Money;
import id.payu.grpc.common.Timestamp;
import id.payu.transaction.adapter.persistence.entity.TransactionEntity;
import id.payu.transaction.domain.port.out.TransactionPersistencePort;
import id.payu.transaction.grpc.ExistsByReferenceRequest;
import id.payu.transaction.grpc.ExistsByReferenceResponse;
import id.payu.transaction.grpc.GetByAccountRequest;
import id.payu.transaction.grpc.GetByReferenceRequest;
import id.payu.transaction.grpc.GetHistoryRequest;
import id.payu.transaction.grpc.GetTransactionRequest;
import id.payu.transaction.grpc.TransactionResponse;
import id.payu.transaction.grpc.TransactionServiceGrpc;
import id.payu.transaction.grpc.TransactionStatus;
import id.payu.transaction.grpc.TransactionType;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import org.springframework.grpc.server.service.GrpcService;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * GRPC-002: TransactionService gRPC server — the proto was a phantom contract
 * (0 implementations) while statement-service called the REST API instead.
 * Read paths are fully implemented; money writes (CreateTransaction,
 * UpdateStatus) are UNIMPLEMENTED until they carry idempotency semantics —
 * fail-closed rather than inventing non-idempotent writes.
 */
@GrpcService
@Component
public class TransactionGrpcService extends TransactionServiceGrpc.TransactionServiceImplBase {

    private final TransactionPersistencePort persistencePort;

    public TransactionGrpcService(TransactionPersistencePort persistencePort) {
        this.persistencePort = persistencePort;
    }

    @Override
    public void getTransaction(GetTransactionRequest request,
                               StreamObserver<TransactionResponse> responseObserver) {
        try {
            persistencePort.findById(UUID.fromString(request.getTransactionId()))
                    .map(this::toResponse)
                    .ifPresentOrElse(
                            responseObserver::onNext,
                            () -> responseObserver.onError(Status.NOT_FOUND
                                    .withDescription("Transaction not found: " + request.getTransactionId())
                                    .asRuntimeException()));
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
        }
    }

    @Override
    public void getByReference(GetByReferenceRequest request,
                               StreamObserver<TransactionResponse> responseObserver) {
        try {
            persistencePort.findByReferenceNumber(request.getReferenceId())
                    .stream().findFirst()
                    .map(this::toResponse)
                    .ifPresentOrElse(
                            responseObserver::onNext,
                            () -> responseObserver.onError(Status.NOT_FOUND
                                    .withDescription("Transaction not found: " + request.getReferenceId())
                                    .asRuntimeException()));
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
        }
    }

    @Override
    public void getHistory(GetHistoryRequest request,
                           StreamObserver<TransactionResponse> responseObserver) {
        try {
            int page = request.hasPage() ? request.getPage().getPage() : 0;
            int size = request.hasPage() && request.getPage().getSize() > 0
                    ? request.getPage().getSize() : 100;
            persistencePort.findByAccountId(UUID.fromString(request.getAccountId()), page, size)
                    .forEach(tx -> responseObserver.onNext(toResponse(tx)));
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
        }
    }

    @Override
    public void getByAccount(GetByAccountRequest request,
                             StreamObserver<TransactionResponse> responseObserver) {
        try {
            int page = request.hasPage() ? request.getPage().getPage() : 0;
            int size = request.hasPage() && request.getPage().getSize() > 0
                    ? request.getPage().getSize() : 100;
            persistencePort.findByAccountId(UUID.fromString(request.getAccountId()), page, size)
                    .forEach(tx -> responseObserver.onNext(toResponse(tx)));
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
        }
    }

    @Override
    public void existsByReference(ExistsByReferenceRequest request,
                                  StreamObserver<ExistsByReferenceResponse> responseObserver) {
        try {
            boolean exists = persistencePort.findByReferenceNumber(request.getReferenceId())
                    .stream().findFirst().isPresent();
            responseObserver.onNext(ExistsByReferenceResponse.newBuilder()
                    .setExists(exists)
                    .build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
        }
    }

    @Override
    public void createTransaction(id.payu.transaction.grpc.CreateTransactionRequest request,
                                  StreamObserver<TransactionResponse> responseObserver) {
        // ponytail: money writes need idempotency semantics before they can be
        // exposed over gRPC — fail closed until the use case exists.
        responseObserver.onError(Status.UNIMPLEMENTED
                .withDescription("CreateTransaction is not exposed over gRPC")
                .asRuntimeException());
    }

    @Override
    public void updateStatus(id.payu.transaction.grpc.UpdateStatusRequest request,
                             StreamObserver<TransactionResponse> responseObserver) {
        responseObserver.onError(Status.UNIMPLEMENTED
                .withDescription("UpdateStatus is not exposed over gRPC")
                .asRuntimeException());
    }

    private TransactionResponse toResponse(TransactionEntity tx) {
        TransactionResponse.Builder builder = TransactionResponse.newBuilder()
                .setTransactionId(tx.getId().toString())
                .setReferenceId(tx.getReferenceNumber())
                .setType(mapType(tx.getType() == null ? null : tx.getType().name()))
                .setStatus(mapStatus(tx.getStatus() == null ? null : tx.getStatus().name()))
                .setAmount(Money.newBuilder()
                        .setCurrency(tx.getCurrencyCode() == null ? "IDR" : tx.getCurrencyCode())
                        .setAmount(tx.getAmountValue() == null ? "0" : tx.getAmountValue().toPlainString())
                        .build())
                .setDescription(tx.getDescription() == null ? "" : tx.getDescription())
                .setAccountId(tx.getSenderAccountId() == null ? "" : tx.getSenderAccountId().toString())
                .setDestinationAccountId(tx.getRecipientAccountId() == null ? "" : tx.getRecipientAccountId().toString());
        if (tx.getCreatedAt() != null) {
            builder.setCreatedAt(Timestamp.newBuilder()
                    .setSeconds(tx.getCreatedAt().getEpochSecond())
                    .setNanos(tx.getCreatedAt().getNano())
                    .build());
        }
        if (tx.getFailureReason() != null) {
            builder.setError(ErrorDetail.newBuilder()
                    .setMessage(tx.getFailureReason())
                    .build());
        }
        return builder.build();
    }

    private TransactionType mapType(String type) {
        if (type == null) {
            return TransactionType.TRANSACTION_TYPE_UNSPECIFIED;
        }
        return switch (type) {
            case "CREDIT" -> TransactionType.CREDIT;
            case "DEBIT" -> TransactionType.DEBIT;
            case "TRANSFER" -> TransactionType.TRANSFER;
            case "PAYMENT" -> TransactionType.PAYMENT;
            case "REFUND" -> TransactionType.REFUND;
            case "FEE" -> TransactionType.FEE;
            case "INTEREST" -> TransactionType.INTEREST;
            default -> TransactionType.TRANSACTION_TYPE_UNSPECIFIED;
        };
    }

    private TransactionStatus mapStatus(String status) {
        if (status == null) {
            return TransactionStatus.TRANSACTION_STATUS_UNSPECIFIED;
        }
        return switch (status) {
            case "PENDING" -> TransactionStatus.PENDING;
            case "PROCESSING" -> TransactionStatus.PROCESSING;
            case "COMPLETED" -> TransactionStatus.COMPLETED;
            case "FAILED" -> TransactionStatus.FAILED;
            case "CANCELLED" -> TransactionStatus.CANCELLED;
            case "REVERSED" -> TransactionStatus.REVERSED;
            default -> TransactionStatus.TRANSACTION_STATUS_UNSPECIFIED;
        };
    }
}
