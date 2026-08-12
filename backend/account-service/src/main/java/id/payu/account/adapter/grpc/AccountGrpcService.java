package id.payu.account.adapter.grpc;

import id.payu.account.domain.model.Account;
import id.payu.account.domain.model.AccountStatus;
import id.payu.account.domain.port.out.AccountPersistencePort;
import id.payu.account.grpc.AccountExistsRequest;
import id.payu.account.grpc.AccountExistsResponse;
import id.payu.account.grpc.AccountResponse;
import id.payu.account.grpc.AccountServiceGrpc;
import id.payu.account.grpc.AccountVerificationResponse;
import id.payu.account.grpc.GetAccountByNumberRequest;
import id.payu.account.grpc.GetAccountRequest;
import id.payu.account.grpc.GetAccountsByUserRequest;
import id.payu.account.grpc.VerifyAccountRequest;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import org.springframework.grpc.server.service.GrpcService;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * GRPC-001: AccountService gRPC server — the proto was a phantom contract
 * (0 implementations) while consumers fell back to REST. Read paths are fully
 * implemented; money writes (CreateAccount, UpdateAccount) are UNIMPLEMENTED —
 * fail-closed rather than inventing non-idempotent writes.
 */
@GrpcService
@Component
public class AccountGrpcService extends AccountServiceGrpc.AccountServiceImplBase {

    private final AccountPersistencePort persistencePort;

    public AccountGrpcService(AccountPersistencePort persistencePort) {
        this.persistencePort = persistencePort;
    }

    @Override
    public void getAccount(GetAccountRequest request,
                           StreamObserver<AccountResponse> responseObserver) {
        try {
            persistencePort.findById(UUID.fromString(request.getAccountId()))
                    .map(this::toResponse)
                    .ifPresentOrElse(
                            responseObserver::onNext,
                            () -> responseObserver.onError(Status.NOT_FOUND
                                    .withDescription("Account not found: " + request.getAccountId())
                                    .asRuntimeException()));
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
        }
    }

    @Override
    public void getAccountsByUser(GetAccountsByUserRequest request,
                                  StreamObserver<AccountResponse> responseObserver) {
        try {
            persistencePort.findByUserId(UUID.fromString(request.getUserId()))
                    .forEach(account -> responseObserver.onNext(toResponse(account)));
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
        }
    }

    @Override
    public void verifyAccount(VerifyAccountRequest request,
                              StreamObserver<AccountVerificationResponse> responseObserver) {
        try {
            UUID accountId = request.getAccountId() == null || request.getAccountId().isBlank()
                    ? null : UUID.fromString(request.getAccountId());
            Account account = accountId != null
                    ? persistencePort.findById(accountId).orElse(null)
                    : null;
            if (account != null && request.getAccountNumber() != null
                    && !request.getAccountNumber().isBlank()
                    && !account.getAccountNumber().equals(request.getAccountNumber())) {
                account = null;
            }
            if (account == null || account.getStatus() != AccountStatus.ACTIVE) {
                responseObserver.onNext(AccountVerificationResponse.newBuilder()
                        .setValid(false)
                        .setAccountId(request.getAccountId())
                        .setAccountNumber(request.getAccountNumber())
                        .build());
            } else {
                responseObserver.onNext(AccountVerificationResponse.newBuilder()
                        .setValid(true)
                        .setAccountId(account.getId().toString())
                        .setAccountNumber(account.getAccountNumber())
                        .setStatus(toGrpcStatus(account.getStatus()))
                        .build());
            }
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
        }
    }

    @Override
    public void getAccountByNumber(GetAccountByNumberRequest request,
                                   StreamObserver<AccountResponse> responseObserver) {
        try {
            Account account = findByNumber(request.getAccountNumber());
            if (account == null) {
                responseObserver.onError(Status.NOT_FOUND
                        .withDescription("Account not found: " + request.getAccountNumber())
                        .asRuntimeException());
            } else {
                responseObserver.onNext(toResponse(account));
            }
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
        }
    }

    @Override
    public void accountExists(AccountExistsRequest request,
                              StreamObserver<AccountExistsResponse> responseObserver) {
        try {
            Account account = persistencePort.findById(UUID.fromString(request.getAccountId())).orElse(null);
            responseObserver.onNext(AccountExistsResponse.newBuilder()
                    .setExists(account != null)
                    .setStatus(account != null ? toGrpcStatus(account.getStatus()) : id.payu.account.grpc.AccountStatus.ACCOUNT_STATUS_UNSPECIFIED)
                    .build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
        }
    }

    @Override
    public void createAccount(id.payu.account.grpc.CreateAccountRequest request,
                              StreamObserver<AccountResponse> responseObserver) {
        // ponytail: account creation is a write with idempotency needs — fail
        // closed over gRPC until the use case exists.
        responseObserver.onError(Status.UNIMPLEMENTED
                .withDescription("CreateAccount is not exposed over gRPC")
                .asRuntimeException());
    }

    @Override
    public void updateAccount(id.payu.account.grpc.UpdateAccountRequest request,
                              StreamObserver<AccountResponse> responseObserver) {
        responseObserver.onError(Status.UNIMPLEMENTED
                .withDescription("UpdateAccount is not exposed over gRPC")
                .asRuntimeException());
    }

    private Account findByNumber(String accountNumber) {
        return persistencePort.findByAccountNumber(accountNumber).orElse(null);
    }

    private AccountResponse toResponse(Account account) {
        AccountResponse.Builder builder = AccountResponse.newBuilder()
                .setAccountId(account.getId().toString())
                .setUserId(account.getUserId() == null ? "" : account.getUserId().toString())
                .setAccountNumber(account.getAccountNumber())
                .setAccountType(account.getAccountType() == null ? "" : account.getAccountType())
                .setStatus(toGrpcStatus(account.getStatus()))
                .setCurrency(account.getCurrency() == null ? "IDR" : account.getCurrency());
        if (account.getCreatedAt() != null) {
            builder.setCreatedAt(id.payu.grpc.common.Timestamp.newBuilder()
                    .setSeconds(account.getCreatedAt().atZone(java.time.ZoneOffset.UTC).toEpochSecond())
                    .build());
        }
        return builder.build();
    }

    private id.payu.account.grpc.AccountStatus toGrpcStatus(AccountStatus status) {
        if (status == null) {
            return id.payu.account.grpc.AccountStatus.ACCOUNT_STATUS_UNSPECIFIED;
        }
        return switch (status) {
            case ACTIVE -> id.payu.account.grpc.AccountStatus.ACTIVE;
            case FROZEN -> id.payu.account.grpc.AccountStatus.ACCOUNT_STATUS_UNSPECIFIED;
            case CLOSED -> id.payu.account.grpc.AccountStatus.CLOSED;
            case PENDING_VERIFICATION -> id.payu.account.grpc.AccountStatus.PENDING_VERIFICATION;
        };
    }
}
