package id.payu.statement.adapter.client;

import id.payu.statement.domain.port.out.WalletServicePort;
import id.payu.wallet.grpc.BalanceResponse;
import id.payu.wallet.grpc.GetBalanceRequest;
import id.payu.wallet.grpc.WalletServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;

/**
 * gRPC adapter for wallet-service integration.
 * Replaces the REST-based WalletServiceClient with high-performance gRPC calls.
 *
 * @since IMP-028
 */
@Primary
@Component("walletGrpcAdapter")
public class WalletGrpcAdapter implements WalletServicePort {

    private static final Logger log = LoggerFactory.getLogger(WalletGrpcAdapter.class);

    @Value("${payu.grpc.clients.wallet-service.address:static://wallet-service:9090}")
    private String walletServiceAddress;

    private ManagedChannel channel;
    private WalletServiceGrpc.WalletServiceBlockingStub walletStub;

    @PostConstruct
    public void init() {
        String target = walletServiceAddress.replace("static://", "");
        String[] parts = target.split(":");
        String host = parts[0];
        int port = parts.length > 1 ? Integer.parseInt(parts[1]) : 9090;

        channel = ManagedChannelBuilder.forAddress(host, port)
                .usePlaintext()
                .build();
        walletStub = WalletServiceGrpc.newBlockingStub(channel);
        log.info("Initialized gRPC wallet-service stub at {}:{}", host, port);
    }

    @PreDestroy
    public void destroy() {
        if (channel != null && !channel.isShutdown()) {
            try {
                channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                channel.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    @Override
    public BigDecimal getCurrentBalance(String customerId) {
        log.info("gRPC getCurrentBalance: customerId={}", customerId);

        try {
            GetBalanceRequest request = GetBalanceRequest.newBuilder()
                    .setWalletId(customerId)
                    .setAccountId(customerId)
                    .build();

            BalanceResponse response = walletStub.getBalance(request);

            if (response.getBalance() != null && !response.getBalance().getAmount().isEmpty()) {
                BigDecimal balance = new BigDecimal(response.getBalance().getAmount());
                log.info("gRPC balance retrieved: customerId={}, balance={}", customerId, balance);
                return balance;
            }

            log.warn("gRPC getCurrentBalance: no balance returned for customerId={}", customerId);
            return BigDecimal.ZERO;
        } catch (StatusRuntimeException e) {
            log.error("gRPC getCurrentBalance error: customerId={}, status={}, message={}",
                    customerId, e.getStatus(), e.getMessage());
            throw new RuntimeException("Failed to fetch balance via gRPC for customer " + customerId + ": " + e.getStatus(), e);
        }
    }
}
