package id.payu.transaction.adapter.client;

import id.payu.grpc.starter.config.GrpcChannelSupport;
import id.payu.transaction.domain.port.out.AccountServicePort;
import id.payu.account.grpc.AccountResponse;
import id.payu.account.grpc.AccountServiceGrpc;
import id.payu.account.grpc.GetAccountsByUserRequest;
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

    private ManagedChannel channel;
    private AccountServiceGrpc.AccountServiceBlockingStub stub;

    @PostConstruct
    void init() {
        channel = GrpcChannelSupport.channel(accountServiceAddress);
        stub = GrpcChannelSupport.withDeadline(
                AccountServiceGrpc.newBlockingStub(channel),
                GrpcChannelSupport.DEFAULT_DEADLINE_SECONDS);
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
            stub.getAccountsByUser(GetAccountsByUserRequest.newBuilder()
                    .setUserId(userId)
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
     * Fallback method for circuit breaker.
     * Returns empty list to fail-safe (deny access) when account service is unavailable.
     */
    private List<UUID> getAccountIdsByUserIdFallback(String userId, Exception e) {
        log.warn("Circuit breaker fallback for getAccountIdsByUserId: {}. UserId: {}", e.getMessage(), userId);
        // Fail-safe: return empty list so authorization fails (deny by default)
        return Collections.emptyList();
    }
}
