package id.payu.lending.adapter.client;

import id.payu.grpc.starter.config.GrpcChannelSupport;
import id.payu.account.grpc.AccountServiceGrpc;
import id.payu.account.grpc.GetAccountsByUserRequest;
import id.payu.account.grpc.GetUserProfileRequest;
import io.grpc.ManagedChannel;
import io.grpc.StatusRuntimeException;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * gRPC client for account-service (GRPC-001 server live).
 * Replaces the Feign REST calls for account-id resolution and user profile.
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
        // GRPC-012 / RELAY-011: NEVER bake deadline into shared stub — freezes at creation.
        // Deadline applies per call below.
        stub = AccountServiceGrpc.newBlockingStub(channel);
    }

    @PreDestroy
    void destroy() {
        if (channel != null) {
            channel.shutdown();
        }
    }

    /**
     * User profile for credit scoring (kycStatus, createdAt tenure).
     */
    public id.payu.lending.interfaces.dto.UserResponse getUserProfile(String userId) {
        try {
            id.payu.account.grpc.UserProfileResponse profile = stub
                    .withDeadlineAfter(GrpcChannelSupport.DEFAULT_DEADLINE_SECONDS, TimeUnit.SECONDS).getUserProfile(
                    GetUserProfileRequest.newBuilder().setUserId(userId).build());
            return new id.payu.lending.interfaces.dto.UserResponse(
                    java.util.UUID.fromString(profile.getUserId()),
                    profile.getExternalId(),
                    profile.getUsername(),
                    profile.getEmail(),
                    profile.getPhoneNumber(),
                    profile.getFullName(),
                    null,
                    profile.getStatus().name(),
                    profile.getKycStatus(),
                    java.time.LocalDateTime.ofInstant(
                            java.time.Instant.ofEpochSecond(profile.getCreatedAt().getSeconds()),
                            java.time.ZoneOffset.UTC));
        } catch (io.grpc.StatusRuntimeException e) {
            throw new RuntimeException("Failed to fetch user profile via gRPC: " + e.getStatus(), e);
        }
    }

    public List<UUID> getAccountIdsByUserId(String userId) {
        try {
            List<UUID> accountIds = new ArrayList<>();
            stub.withDeadlineAfter(GrpcChannelSupport.DEFAULT_DEADLINE_SECONDS, TimeUnit.SECONDS)
                    .getAccountsByUser(GetAccountsByUserRequest.newBuilder()
                    .setUserId(userId)
                    .build())
                    .forEachRemaining(account -> accountIds.add(UUID.fromString(account.getAccountId())));
            return accountIds;
        } catch (StatusRuntimeException e) {
            throw new RuntimeException("Failed to fetch account IDs via gRPC: " + e.getStatus(), e);
        }
    }
}
