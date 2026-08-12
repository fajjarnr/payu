package id.payu.lending.adapter.client;

import id.payu.grpc.starter.config.GrpcChannelSupport;
import id.payu.account.grpc.AccountServiceGrpc;
import id.payu.account.grpc.GetAccountsByUserRequest;
import io.grpc.ManagedChannel;
import io.grpc.StatusRuntimeException;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * gRPC client for account-service (GRPC-001 server live).
 * Replaces the Feign REST call for account-id resolution; the user-profile
 * lookup (getUserById) has no gRPC RPC yet and stays REST.
 */
@Component
public class AccountGrpcClient {

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

    public List<UUID> getAccountIdsByUserId(String userId) {
        try {
            List<UUID> accountIds = new ArrayList<>();
            stub.getAccountsByUser(GetAccountsByUserRequest.newBuilder()
                    .setUserId(userId)
                    .build())
                    .forEachRemaining(account -> accountIds.add(UUID.fromString(account.getAccountId())));
            return accountIds;
        } catch (StatusRuntimeException e) {
            throw new RuntimeException("Failed to fetch account IDs via gRPC: " + e.getStatus(), e);
        }
    }
}
