package id.payu.investment.adapter.client;

import id.payu.investment.domain.port.out.WalletServicePort;
import id.payu.grpc.common.Money;
import id.payu.wallet.grpc.BalanceResponse;
import id.payu.wallet.grpc.CommitReservationRequest;
import id.payu.wallet.grpc.CreditRequest;
import id.payu.wallet.grpc.GetBalanceRequest;
import id.payu.wallet.grpc.ReservationResponse;
import id.payu.wallet.grpc.ReserveBalanceRequest;
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
 * Replaces the REST-based WalletServiceAdapter with high-performance gRPC calls.
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
    public void deductBalance(String userId, BigDecimal amount, String referenceId) {
        log.info("gRPC deductBalance (reserve+commit): userId={}, amount={}, referenceId={}", userId, amount, referenceId);

        try {
            // Step 1: Reserve balance
            ReserveBalanceRequest reserveRequest = ReserveBalanceRequest.newBuilder()
                    .setWalletId(userId)
                    .setAccountId(userId)
                    .setAmount(Money.newBuilder()
                            .setCurrency(DEFAULT_CURRENCY)
                            .setAmount(amount.toPlainString())
                            .build())
                    .setReferenceId(referenceId)
                    .setDescription("Investment purchase deduction")
                    .build();

            ReservationResponse reserveResponse = walletStub.reserveBalance(reserveRequest);

            if (!reserveResponse.getSuccess()) {
                throw new RuntimeException("Failed to reserve balance: " + reserveResponse.getError().getMessage());
            }

            String reservationId = reserveResponse.getReservationId();
            log.info("gRPC balance reserved: reservationId={}", reservationId);

            // Step 2: Commit reservation
            CommitReservationRequest commitRequest = CommitReservationRequest.newBuilder()
                    .setReservationId(reservationId)
                    .build();

            TransactionResponse commitResponse = walletStub.commitReservation(commitRequest);

            if (!commitResponse.getSuccess()) {
                throw new RuntimeException("Failed to commit reservation: " + commitResponse.getError().getMessage());
            }

            log.info("gRPC reservation committed: reservationId={}, amount={}", reservationId, amount);
        } catch (StatusRuntimeException e) {
            log.error("gRPC deductBalance error: status={}, message={}", e.getStatus(), e.getMessage());
            throw new RuntimeException("Failed to deduct wallet balance via gRPC: " + e.getStatus(), e);
        }
    }

    @Override
    public void creditBalance(String userId, BigDecimal amount, String referenceId) {
        log.info("gRPC creditBalance: userId={}, amount={}, referenceId={}", userId, amount, referenceId);

        try {
            CreditRequest request = CreditRequest.newBuilder()
                    .setWalletId(userId)
                    .setAccountId(userId)
                    .setAmount(Money.newBuilder()
                            .setCurrency(DEFAULT_CURRENCY)
                            .setAmount(amount.toPlainString())
                            .build())
                    .setReferenceId(referenceId)
                    .setDescription("Investment redemption credit")
                    .build();

            TransactionResponse response = walletStub.credit(request);

            if (response.getSuccess()) {
                log.info("gRPC balance credited: userId={}, txId={}", userId, response.getTransactionId());
            } else {
                log.warn("gRPC creditBalance failed: error={}", response.getError().getMessage());
                throw new RuntimeException("Failed to credit wallet balance: " + response.getError().getMessage());
            }
        } catch (StatusRuntimeException e) {
            log.error("gRPC creditBalance error: status={}, message={}", e.getStatus(), e.getMessage());
            throw new RuntimeException("Failed to credit wallet balance via gRPC: " + e.getStatus(), e);
        }
    }

    @Override
    public boolean hasSufficientBalance(String userId, BigDecimal amount) {
        log.debug("gRPC hasSufficientBalance: userId={}, amount={}", userId, amount);

        try {
            GetBalanceRequest request = GetBalanceRequest.newBuilder()
                    .setWalletId(userId)
                    .setAccountId(userId)
                    .build();

            BalanceResponse response = walletStub.getAvailableBalance(request);

            if (response.getAvailableBalance() != null
                    && !response.getAvailableBalance().getAmount().isEmpty()) {
                BigDecimal availableBalance = new BigDecimal(response.getAvailableBalance().getAmount());
                boolean sufficient = availableBalance.compareTo(amount) >= 0;
                log.debug("gRPC balance check: userId={}, available={}, required={}, sufficient={}",
                        userId, availableBalance, amount, sufficient);
                return sufficient;
            }

            log.warn("gRPC hasSufficientBalance: no available balance returned for userId={}", userId);
            return false;
        } catch (StatusRuntimeException e) {
            log.error("gRPC hasSufficientBalance error: status={}, message={}", e.getStatus(), e.getMessage());
            return false;
        }
    }
}
