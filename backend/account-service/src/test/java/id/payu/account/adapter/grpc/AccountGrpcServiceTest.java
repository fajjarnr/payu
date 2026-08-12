package id.payu.account.adapter.grpc;

import id.payu.account.domain.model.Account;
import id.payu.account.domain.model.AccountStatus;
import id.payu.account.domain.port.out.AccountPersistencePort;
import id.payu.account.grpc.AccountExistsRequest;
import id.payu.account.grpc.AccountExistsResponse;
import id.payu.account.grpc.AccountResponse;
import id.payu.account.grpc.AccountVerificationResponse;
import id.payu.account.grpc.GetAccountRequest;
import id.payu.account.grpc.GetAccountsByUserRequest;
import id.payu.account.grpc.GetAccountByNumberRequest;
import id.payu.account.grpc.VerifyAccountRequest;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * GRPC-001: AccountService gRPC server read paths — the proto previously had
 * zero implementations (phantom contract).
 */
class AccountGrpcServiceTest {

    private AccountPersistencePort persistencePort;
    private id.payu.account.domain.port.out.UserPersistencePort userPersistencePort;
    private AccountGrpcService service;

    @BeforeEach
    void setUp() {
        persistencePort = mock(AccountPersistencePort.class);
        userPersistencePort = mock(id.payu.account.domain.port.out.UserPersistencePort.class);
        service = new AccountGrpcService(persistencePort, userPersistencePort);
    }

    private Account account(UUID id, String number, AccountStatus status) {
        return Account.builder()
                .id(id)
                .userId(UUID.randomUUID())
                .accountNumber(number)
                .accountType("SAVINGS")
                .status(status)
                .currency("IDR")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    private static class RecordingObserver<T> implements StreamObserver<T> {
        T value;
        Throwable error;

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
        }
    }

    @Test
    void getAccountReturnsMappedAccount() {
        UUID id = UUID.randomUUID();
        when(persistencePort.findById(id)).thenReturn(Optional.of(account(id, "9911223344", AccountStatus.ACTIVE)));

        RecordingObserver<AccountResponse> observer = new RecordingObserver<>();
        service.getAccount(GetAccountRequest.newBuilder().setAccountId(id.toString()).build(), observer);

        assertThat(observer.error).isNull();
        assertThat(observer.value.getAccountId()).isEqualTo(id.toString());
        assertThat(observer.value.getAccountNumber()).isEqualTo("9911223344");
        assertThat(observer.value.getStatus().name()).isEqualTo("ACTIVE");
    }

    @Test
    void getAccountUnknownReturnsNotFound() {
        UUID id = UUID.randomUUID();
        when(persistencePort.findById(id)).thenReturn(Optional.empty());

        RecordingObserver<AccountResponse> observer = new RecordingObserver<>();
        service.getAccount(GetAccountRequest.newBuilder().setAccountId(id.toString()).build(), observer);

        assertThat(observer.error).isInstanceOf(StatusRuntimeException.class);
        assertThat(((StatusRuntimeException) observer.error).getStatus().getCode())
                .isEqualTo(Status.Code.NOT_FOUND);
    }

    @Test
    void getAccountsByUserStreamsAllAccounts() {
        UUID userId = UUID.randomUUID();
        when(persistencePort.findByUserId(userId))
                .thenReturn(List.of(account(UUID.randomUUID(), "111", AccountStatus.ACTIVE),
                        account(UUID.randomUUID(), "222", AccountStatus.CLOSED)));

        RecordingObserver<AccountResponse> observer = new RecordingObserver<>();
        service.getAccountsByUser(GetAccountsByUserRequest.newBuilder().setUserId(userId.toString()).build(), observer);

        assertThat(observer.error).isNull();
        assertThat(observer.value.getAccountNumber()).isEqualTo("222");
    }

    @Test
    void verifyAccountRejectsInactiveOrMismatchedNumber() {
        UUID id = UUID.randomUUID();
        when(persistencePort.findById(id)).thenReturn(Optional.of(account(id, "9911223344", AccountStatus.ACTIVE)));

        RecordingObserver<AccountVerificationResponse> ok = new RecordingObserver<>();
        service.verifyAccount(VerifyAccountRequest.newBuilder()
                .setAccountId(id.toString()).setAccountNumber("9911223344").build(), ok);
        assertThat(ok.error).isNull();
        assertThat(ok.value.getValid()).isTrue();

        RecordingObserver<AccountVerificationResponse> mismatch = new RecordingObserver<>();
        service.verifyAccount(VerifyAccountRequest.newBuilder()
                .setAccountId(id.toString()).setAccountNumber("0000000000").build(), mismatch);
        assertThat(mismatch.value.getValid()).isFalse();
    }

    @Test
    void accountExistsReportsPresenceAndStatus() {
        UUID id = UUID.randomUUID();
        when(persistencePort.findById(id)).thenReturn(Optional.of(account(id, "9911223344", AccountStatus.ACTIVE)));

        RecordingObserver<AccountExistsResponse> observer = new RecordingObserver<>();
        service.accountExists(AccountExistsRequest.newBuilder().setAccountId(id.toString()).build(), observer);

        assertThat(observer.error).isNull();
        assertThat(observer.value.getExists()).isTrue();
        assertThat(observer.value.getStatus().name()).isEqualTo("ACTIVE");
    }

    @Test
    void getUserProfileReturnsKycAndTenureData() {
        id.payu.account.domain.model.User user = id.payu.account.domain.model.User.builder()
                .id(UUID.randomUUID())
                .externalId("ext-1")
                .username("scorer")
                .kycStatus(id.payu.account.domain.model.KycStatus.VERIFIED)
                .status(id.payu.account.domain.model.UserStatus.ACTIVE)
                .createdAt(LocalDateTime.of(2025, 1, 1, 0, 0))
                .build();
        when(userPersistencePort.findByExternalId("ext-1")).thenReturn(Optional.of(user));

        RecordingObserver<id.payu.account.grpc.UserProfileResponse> observer = new RecordingObserver<>();
        service.getUserProfile(id.payu.account.grpc.GetUserProfileRequest.newBuilder()
                .setUserId("ext-1").build(), observer);

        assertThat(observer.error).isNull();
        assertThat(observer.value.getKycStatus()).isEqualTo("VERIFIED");
        assertThat(observer.value.getUsername()).isEqualTo("scorer");
        assertThat(observer.value.getCreatedAt().getSeconds()).isGreaterThan(0);
    }

    @Test
    void getUserProfileUnknownReturnsNotFound() {
        when(userPersistencePort.findByExternalId("nobody")).thenReturn(Optional.empty());

        RecordingObserver<id.payu.account.grpc.UserProfileResponse> observer = new RecordingObserver<>();
        service.getUserProfile(id.payu.account.grpc.GetUserProfileRequest.newBuilder()
                .setUserId("nobody").build(), observer);

        assertThat(observer.error).isInstanceOf(StatusRuntimeException.class);
        assertThat(((StatusRuntimeException) observer.error).getStatus().getCode())
                .isEqualTo(Status.Code.NOT_FOUND);
    }

    @Test
    void getAccountByNumberReturnsAccount() {
        when(persistencePort.findByAccountNumber("9911223344"))
                .thenReturn(Optional.of(account(UUID.randomUUID(), "9911223344", AccountStatus.ACTIVE)));

        RecordingObserver<AccountResponse> observer = new RecordingObserver<>();
        service.getAccountByNumber(GetAccountByNumberRequest.newBuilder().setAccountNumber("9911223344").build(), observer);

        assertThat(observer.error).isNull();
        assertThat(observer.value.getAccountNumber()).isEqualTo("9911223344");
    }
}
