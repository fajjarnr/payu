package id.payu.billing.adapter.client;

import id.payu.billing.domain.port.out.WalletPort;
import id.payu.grpc.common.Money;
import id.payu.wallet.grpc.CommitReservationRequest;
import id.payu.wallet.grpc.ReleaseReservationRequest;
import id.payu.wallet.grpc.ReservationResponse;
import id.payu.wallet.grpc.ReserveBalanceRequest;
import id.payu.wallet.grpc.TransactionResponse;
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
 * Replaces the REST-based WalletAdapter with high-performance gRPC calls.
 *
 * @since IMP-028
 */
@Primary
@Component("walletGrpcAdapter")
// GRPC-016: wallet calls are idempotent by referenceId, so retry is safe;
// circuit breaker prevents cascading failures when wallet is down.
@CircuitBreaker(name = "walletService")
@Retry(name = "walletService")
public class WalletGrpcAdapter implements WalletPort {

    private static final Logger log = LoggerFactory.getLogger(WalletGrpcAdapter.class);
    private static final String DEFAULT_CURRENCY = "IDR";

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
    public ReserveResult reserveBalance(String accountId, BigDecimal amount, String referenceNumber) {
        log.info("gRPC reserveBalance: accountId={}, amount={}, ref={}", accountId, amount, referenceNumber);

        try {
            ReserveBalanceRequest request = ReserveBalanceRequest.newBuilder()
                    .setWalletId(accountId)
                    .setAccountId(accountId)
                    .setAmount(Money.newBuilder()
                            .setCurrency(DEFAULT_CURRENCY)
                            .setAmount(amount.toPlainString())
                            .build())
                    .setReferenceId(referenceNumber)
                    .setDescription("Billing payment reserve")
                    .build();

            ReservationResponse response = walletStub.reserveBalance(request);

            if (response.getSuccess()) {
                log.info("gRPC balance reserved: reservationId={}", response.getReservationId());
                return new ReserveResult(response.getReservationId(), "RESERVED");
            } else {
                log.warn("gRPC reserveBalance failed: error={}", response.getError().getMessage());
                return new ReserveResult(null, "FAILED");
            }
        } catch (StatusRuntimeException e) {
            log.error("gRPC reserveBalance error: status={}, message={}", e.getStatus(), e.getMessage());
            throw new RuntimeException("Failed to reserve balance via gRPC: " + e.getStatus(), e);
        }
    }

    @Override
    public void commitReservation(String reservationId) {
        log.info("gRPC commitReservation: reservationId={}", reservationId);

        try {
            CommitReservationRequest request = CommitReservationRequest.newBuilder()
                    .setReservationId(reservationId)
                    .build();

            TransactionResponse response = walletStub.commitReservation(request);

            if (response.getSuccess()) {
                log.info("gRPC reservation committed: reservationId={}, txId={}", reservationId, response.getTransactionId());
            } else {
                log.warn("gRPC commitReservation failed: error={}", response.getError().getMessage());
                throw new RuntimeException("Failed to commit reservation: " + response.getError().getMessage());
            }
        } catch (StatusRuntimeException e) {
            log.error("gRPC commitReservation error: status={}, message={}", e.getStatus(), e.getMessage());
            throw new RuntimeException("Failed to commit reservation via gRPC: " + e.getStatus(), e);
        }
    }

    @Override
    public void releaseReservation(String reservationId) {
        log.info("gRPC releaseReservation: reservationId={}", reservationId);

        try {
            ReleaseReservationRequest request = ReleaseReservationRequest.newBuilder()
                    .setReservationId(reservationId)
                    .build();

            TransactionResponse response = walletStub.releaseReservation(request);

            if (response.getSuccess()) {
                log.info("gRPC reservation released: reservationId={}", reservationId);
            } else {
                log.warn("gRPC releaseReservation failed: error={}", response.getError().getMessage());
                throw new RuntimeException("Failed to release reservation: " + response.getError().getMessage());
            }
        } catch (StatusRuntimeException e) {
            log.error("gRPC releaseReservation error: status={}, message={}", e.getStatus(), e.getMessage());
            throw new RuntimeException("Failed to release reservation via gRPC: " + e.getStatus(), e);
        }
    }
}
