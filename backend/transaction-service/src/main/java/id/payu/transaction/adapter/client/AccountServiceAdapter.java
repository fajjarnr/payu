package id.payu.transaction.adapter.client;

import id.payu.grpc.starter.config.GrpcChannelFactory;
import id.payu.grpc.starter.config.GrpcChannelSupport;
import id.payu.transaction.domain.port.out.AccountServicePort;
import id.payu.account.grpc.AccountResponse;
import id.payu.account.grpc.AccountServiceGrpc;
import id.payu.account.grpc.GetAccountByNumberRequest;
import id.payu.account.grpc.GetAccountsByUserRequest;
import id.payu.account.grpc.GetUserProfileRequest;
import id.payu.account.grpc.UserProfileResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.grpc.ManagedChannel;
import io.grpc.StatusRuntimeException;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Adapter for calling account-service over gRPC (GRPC-001 server is live).
 * Implements circuit breaker and retry for resilience; fail-safe returns an
 * empty list so authorization denies by default when account-service is down.
 */
@Component
public class AccountServiceAdapter implements AccountServicePort {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AccountServiceAdapter.class);

    @Value("${payu.grpc.clients.account-service.address:static://account-service:9090}")
    private String accountServiceAddress;

    // GRPC-AUTH-001: the raw channel carried no client interceptor, so the
    // starter never attached the caller's bearer token and account-service
    // rejected every lookup with UNAUTHENTICATED.
    @org.springframework.beans.factory.annotation.Autowired
    private GrpcChannelFactory channelFactory;

    private ManagedChannel channel;
    private AccountServiceGrpc.AccountServiceBlockingStub stub;

    @PostConstruct
    void init() {
        channel = channelFactory.channel(accountServiceAddress);
        // GRPC-012: deadline per call (see WalletGrpcAdapter) — a shared stub
        // freezes its deadline at creation and dies 30s after boot.
        stub = channelFactory.interceptedStub(AccountServiceGrpc.newBlockingStub(channel));
    }

    @PreDestroy
    void destroy() {
        if (channel != null) {
            channel.shutdown();
        }
    }

    @Override
    @CircuitBreaker(name = "accountService", fallbackMethod = "getAccountIdsByUserIdFallback")
    @Retry(name = "accountService")
    public List<UUID> getAccountIdsByUserId(String userId) {
        if (userId == null || userId.isBlank()) {
            log.warn("UserId is null or blank, returning empty account list");
            return Collections.emptyList();
        }

        log.debug("Fetching account IDs for user: {}", userId);

        try {
            List<UUID> accountIds = new ArrayList<>();
            stub.withDeadlineAfter(GrpcChannelSupport.DEFAULT_DEADLINE_SECONDS, TimeUnit.SECONDS)
                    .getAccountsByUser(GetAccountsByUserRequest.newBuilder()
                    .setUserId(resolveAccountServiceUserId(userId))
                    .build())
                    .forEachRemaining(account -> accountIds.add(UUID.fromString(account.getAccountId())));
            log.debug("Found {} accounts for user: {}", accountIds.size(), userId);
            return accountIds;
        } catch (StatusRuntimeException e) {
            log.error("Failed to fetch account IDs for user {}: {}", userId, e.getStatus());
            throw e;
        }
    }

    /**
     * GRPC-AUTH-001 (R4): account-service keys accounts by its own user id
     * (users.id), while the caller only knows the IAM subject
     * (users.external_id = the token's {@code sub}). Resolve the subject to
     * the account-service user id first; a value that is already an
     * account-service user id is used as is.
     */
    private String resolveAccountServiceUserId(String userId) {
        try {
            UserProfileResponse profile = stub
                    .withDeadlineAfter(GrpcChannelSupport.DEFAULT_DEADLINE_SECONDS, TimeUnit.SECONDS)
                    .getUserProfile(GetUserProfileRequest.newBuilder()
                            .setUserId(userId)
                            .build());
            return profile.getUserId();
        } catch (StatusRuntimeException e) {
            if (e.getStatus().getCode() == io.grpc.Status.Code.NOT_FOUND) {
                // Not an IAM subject — treat it as an account-service user id.
                return userId;
            }
            throw e;
        }
    }

    /**
     * Fallback method for circuit breaker.
     * Returns empty list to fail-safe (deny access) when account service is unavailable.
     */
    private List<UUID> getAccountIdsByUserIdFallback(String userId, Exception e) {
        log.warn("Circuit breaker fallback for getAccountIdsByUserId: {}. UserId: {}", e.getMessage(), userId);
        // Fail-safe: return empty list so authorization fails (deny by default)
        return Collections.emptyList();
    }

    @Override
    @CircuitBreaker(name = "accountService", fallbackMethod = "getAccountIdByNumberFallback")
    @Retry(name = "accountService")
    public java.util.Optional<UUID> getAccountIdByNumber(String accountNumber) {
        if (accountNumber == null || accountNumber.isBlank()) {
            return java.util.Optional.empty();
        }
        try {
            AccountResponse response = stub
                    .withDeadlineAfter(GrpcChannelSupport.DEFAULT_DEADLINE_SECONDS, TimeUnit.SECONDS)
                    .getAccountByNumber(GetAccountByNumberRequest.newBuilder()
                            .setAccountNumber(accountNumber)
                            .build());
            return java.util.Optional.of(UUID.fromString(response.getAccountId()));
        } catch (StatusRuntimeException e) {
            // NOT_FOUND means unknown recipient — fail-safe empty, caller rejects.
            // Other statuses propagate to the retry/circuit-breaker path.
            if (e.getStatus().getCode() == io.grpc.Status.Code.NOT_FOUND) {
                log.info("Recipient account number not found: {}", accountNumber);
                return java.util.Optional.empty();
            }
            log.error("Failed to resolve account number {}: {}", accountNumber, e.getStatus());
            throw e;
        }
    }

    /**
     * Fallback: account-service down — fail-safe empty so the transfer is rejected,
     * never routed to a guessed account.
     */
    private java.util.Optional<UUID> getAccountIdByNumberFallback(String accountNumber, Exception e) {
        log.warn("Circuit breaker fallback for getAccountIdByNumber: {}. AccountNumber: {}", e.getMessage(), accountNumber);
        return java.util.Optional.empty();
    }
}
