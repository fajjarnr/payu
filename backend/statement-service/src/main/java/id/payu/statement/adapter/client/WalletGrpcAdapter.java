package id.payu.statement.adapter.client;

import id.payu.statement.domain.port.out.WalletServicePort;
import id.payu.wallet.grpc.BalanceResponse;
import id.payu.wallet.grpc.GetBalanceRequest;
import id.payu.wallet.grpc.GetHistoryRequest;
import id.payu.wallet.grpc.WalletServiceGrpc;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.grpc.ManagedChannel;
import id.payu.grpc.starter.config.GrpcChannelSupport;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.grpc.ManagedChannel;
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
// GRPC-016: wallet calls are idempotent by referenceId, so retry is safe;
// circuit breaker prevents cascading failures when wallet is down.
@CircuitBreaker(name = "walletService")
@Retry(name = "walletService")
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

        channel = GrpcChannelSupport.channel(walletServiceAddress);
        walletStub = GrpcChannelSupport.withDeadline(
                WalletServiceGrpc.newBlockingStub(channel),
                GrpcChannelSupport.DEFAULT_DEADLINE_SECONDS);
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

    @Override
    public java.util.Optional<BigDecimal> getBalanceAsOf(String customerId, java.time.LocalDate endDate) {
        log.info("gRPC getBalanceAsOf: customerId={}, endDate={}", customerId, endDate);
        try {
            GetHistoryRequest request = GetHistoryRequest.newBuilder()
                    .setWalletId(customerId)
                    .setAccountId(customerId)
                    .build();

            // Ledger entries are returned newest-first; the first entry at or
            // before the cutoff is the balance_after snapshot at period end.
            long cutoffSeconds = endDate.atTime(23, 59, 59)
                    .atZone(java.time.ZoneId.systemDefault()).toEpochSecond();

            java.util.Iterator<id.payu.wallet.grpc.LedgerEntry> entries = walletStub.getHistory(request);
            while (entries.hasNext()) {
                id.payu.wallet.grpc.LedgerEntry entry = entries.next();
                if (entry.hasTimestamp() && entry.getTimestamp().getSeconds() <= cutoffSeconds) {
                    if (entry.hasBalanceAfter() && !entry.getBalanceAfter().getAmount().isEmpty()) {
                        return java.util.Optional.of(new BigDecimal(entry.getBalanceAfter().getAmount()));
                    }
                    return java.util.Optional.empty();
                }
            }
            return java.util.Optional.empty();
        } catch (StatusRuntimeException e) {
            log.error("gRPC getBalanceAsOf error: customerId={}, status={}, message={}",
                    customerId, e.getStatus(), e.getMessage());
            throw new RuntimeException("Failed to fetch balance-as-of via gRPC for customer " + customerId + ": " + e.getStatus(), e);
        }
    }
}
