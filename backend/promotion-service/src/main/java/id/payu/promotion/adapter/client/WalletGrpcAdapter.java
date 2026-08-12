package id.payu.promotion.adapter.client;

import id.payu.promotion.domain.port.out.WalletServicePort;
import id.payu.grpc.common.Money;
import id.payu.wallet.grpc.CreditRequest;
import id.payu.wallet.grpc.TransactionResponse;
import id.payu.wallet.grpc.WalletServiceGrpc;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.grpc.ManagedChannel;
import id.payu.grpc.starter.config.GrpcChannelFactory;
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
 * Replaces the REST-based WalletClient with high-performance gRPC calls.
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
    private static final String DEFAULT_CURRENCY = "IDR";

    @Value("${payu.grpc.clients.wallet-service.address:static://wallet-service:9090}")
    private String walletServiceAddress;

    @org.springframework.beans.factory.annotation.Autowired
    private GrpcChannelFactory channelFactory;
    private ManagedChannel channel;
    private WalletServiceGrpc.WalletServiceBlockingStub walletStub;

    @PostConstruct
    public void init() {
        String target = walletServiceAddress.replace("static://", "");
        String[] parts = target.split(":");
        String host = parts[0];
        int port = parts.length > 1 ? Integer.parseInt(parts[1]) : 9090;

        channel = channelFactory.channel(walletServiceAddress);
        walletStub = channelFactory.blockingStub(
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
    public boolean creditWallet(String accountId, BigDecimal amount, String referenceId, String description) {
        log.info("gRPC creditWallet: accountId={}, amount={}, referenceId={}", accountId, amount, referenceId);

        try {
            CreditRequest request = CreditRequest.newBuilder()
                    .setWalletId(accountId)
                    .setAccountId(accountId)
                    .setAmount(Money.newBuilder()
                            .setCurrency(DEFAULT_CURRENCY)
                            .setAmount(amount.toPlainString())
                            .build())
                    .setReferenceId(referenceId)
                    .setDescription(description != null ? description : "PromotionEntity credit")
                    .build();

            TransactionResponse response = walletStub.credit(request);

            if (response.getSuccess()) {
                log.info("gRPC wallet credited: accountId={}, txId={}", accountId, response.getTransactionId());
                return true;
            } else {
                log.warn("gRPC creditWallet failed: error={}", response.getError().getMessage());
                return false;
            }
        } catch (StatusRuntimeException e) {
            log.error("gRPC creditWallet error: accountId={}, status={}, message={}",
                    accountId, e.getStatus(), e.getMessage());
            throw new WalletCreditException("Failed to credit wallet via gRPC: " + e.getStatus(), e);
        }
    }
}
