package id.payu.transaction.adapter.client;

import id.payu.transaction.domain.port.out.WalletServicePort;
import id.payu.transaction.dto.ReserveBalanceResponse;
import id.payu.grpc.common.Money;
import id.payu.wallet.grpc.CommitReservationRequest;
import id.payu.wallet.grpc.CreditRequest;
import id.payu.wallet.grpc.ReleaseReservationRequest;
import id.payu.wallet.grpc.ReservationResponse;
import id.payu.wallet.grpc.ReserveBalanceRequest;
import id.payu.wallet.grpc.TransactionResponse;
import id.payu.wallet.grpc.WalletServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * gRPC adapter for wallet-service integration.
 * Replaces the REST-based WalletRestAdapter with high-performance gRPC calls.
 *
 * @since IMP-028
 */
@ConditionalOnProperty(name = "payu.grpc.enabled", havingValue = "true", matchIfMissing = false)
@Component("walletGrpcAdapter")
public class WalletGrpcAdapter implements WalletServicePort {

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
    public ReserveBalanceResponse reserveBalance(UUID accountId, String transactionId, BigDecimal amount) {
        log.info("gRPC reserveBalance: accountId={}, transactionId={}, amount={}", accountId, transactionId, amount);

        try {
            ReserveBalanceRequest request = ReserveBalanceRequest.newBuilder()
                    .setWalletId(accountId.toString())
                    .setAccountId(accountId.toString())
                    .setAmount(Money.newBuilder()
                            .setCurrency(DEFAULT_CURRENCY)
                            .setAmount(amount.toPlainString())
                            .build())
                    .setReferenceId(transactionId)
                    .setDescription("Transaction reserve")
                    .build();

            ReservationResponse response = walletStub.reserveBalance(request);

            if (response.getSuccess()) {
                log.info("gRPC balance reserved: reservationId={}", response.getReservationId());
                return id.payu.transaction.dto.ReserveBalanceResponse.builder()
                        .reservationId(response.getReservationId())
                        .accountId(accountId.toString())
                        .referenceId(transactionId)
                        .status("RESERVED")
                        .build();
            } else {
                log.warn("gRPC reserveBalance failed: error={}", response.getError().getMessage());
                return id.payu.transaction.dto.ReserveBalanceResponse.builder()
                        .status("FAILED")
                        .referenceId(transactionId)
                        .build();
            }
        } catch (StatusRuntimeException e) {
            log.error("gRPC reserveBalance error: status={}, message={}", e.getStatus(), e.getMessage());
            throw new RuntimeException("Failed to reserve balance via gRPC: " + e.getStatus(), e);
        }
    }

    @Override
    public void commitBalance(UUID accountId, String transactionId, String reservationId, BigDecimal amount) {
        if (reservationId == null) {
            log.warn("No reservation ID provided for transactionId={}, skipping commit", transactionId);
            return;
        }

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
    public void releaseBalance(UUID accountId, String transactionId, String reservationId, BigDecimal amount) {
        if (reservationId == null) {
            log.warn("No reservation ID provided for transactionId={}, skipping release", transactionId);
            return;
        }

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

    @Override
    public void creditBalance(String accountId, String transactionId, BigDecimal amount) {
        log.info("gRPC creditBalance: accountId={}, transactionId={}, amount={}", accountId, transactionId, amount);

        try {
            CreditRequest request = CreditRequest.newBuilder()
                    .setWalletId(accountId)
                    .setAccountId(accountId)
                    .setAmount(Money.newBuilder()
                            .setCurrency(DEFAULT_CURRENCY)
                            .setAmount(amount.toPlainString())
                            .build())
                    .setReferenceId(transactionId)
                    .setDescription("Internal transfer credit")
                    .build();

            TransactionResponse response = walletStub.credit(request);

            if (response.getSuccess()) {
                log.info("gRPC balance credited: accountId={}, txId={}", accountId, response.getTransactionId());
            } else {
                log.warn("gRPC creditBalance failed: error={}", response.getError().getMessage());
                throw new RuntimeException("Failed to credit balance: " + response.getError().getMessage());
            }
        } catch (StatusRuntimeException e) {
            log.error("gRPC creditBalance error: status={}, message={}", e.getStatus(), e.getMessage());
            throw new RuntimeException("Failed to credit balance via gRPC: " + e.getStatus(), e);
        }
    }
}
