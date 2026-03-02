package id.payu.fx.adapter.client;

import id.payu.fx.domain.port.out.WalletServicePort;
import id.payu.grpc.common.Money;
import id.payu.wallet.grpc.CreditRequest;
import id.payu.wallet.grpc.DebitRequest;
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
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;

/**
 * gRPC adapter for wallet-service integration during FX conversions.
 * Replaces the REST-based WalletServiceAdapter with high-performance gRPC calls.
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
    public boolean debit(String accountId, String transactionId, BigDecimal amount, String currency) {
        log.info("gRPC debit: accountId={}, amount={} {}, txId={}", accountId, amount, currency, transactionId);

        try {
            DebitRequest request = DebitRequest.newBuilder()
                    .setWalletId(accountId)
                    .setAccountId(accountId)
                    .setAmount(Money.newBuilder()
                            .setCurrency(currency)
                            .setAmount(amount.toPlainString())
                            .build())
                    .setReferenceId("fx-debit-" + transactionId)
                    .setDescription("FX debit")
                    .build();

            TransactionResponse response = walletStub.debit(request);

            if (response.getSuccess()) {
                log.info("gRPC debit successful: accountId={}, txId={}", accountId, response.getTransactionId());
                return true;
            } else {
                log.warn("gRPC debit failed: error={}", response.getError().getMessage());
                return false;
            }
        } catch (StatusRuntimeException e) {
            log.error("gRPC debit error: accountId={}, amount={} {}: status={}, message={}",
                    accountId, amount, currency, e.getStatus(), e.getMessage());
            return false;
        }
    }

    @Override
    public boolean credit(String accountId, String transactionId, BigDecimal amount, String currency) {
        log.info("gRPC credit: accountId={}, amount={} {}, txId={}", accountId, amount, currency, transactionId);

        try {
            CreditRequest request = CreditRequest.newBuilder()
                    .setWalletId(accountId)
                    .setAccountId(accountId)
                    .setAmount(Money.newBuilder()
                            .setCurrency(currency)
                            .setAmount(amount.toPlainString())
                            .build())
                    .setReferenceId("fx-credit-" + transactionId)
                    .setDescription("FX credit")
                    .build();

            TransactionResponse response = walletStub.credit(request);

            if (response.getSuccess()) {
                log.info("gRPC credit successful: accountId={}, txId={}", accountId, response.getTransactionId());
                return true;
            } else {
                log.warn("gRPC credit failed: error={}", response.getError().getMessage());
                return false;
            }
        } catch (StatusRuntimeException e) {
            log.error("gRPC credit error: accountId={}, amount={} {}: status={}, message={}",
                    accountId, amount, currency, e.getStatus(), e.getMessage());
            return false;
        }
    }

    @Override
    public void reverseDebit(String accountId, String transactionId, BigDecimal amount, String currency) {
        log.info("gRPC reverseDebit: accountId={}, amount={} {}, txId={}", accountId, amount, currency, transactionId);

        try {
            CreditRequest request = CreditRequest.newBuilder()
                    .setWalletId(accountId)
                    .setAccountId(accountId)
                    .setAmount(Money.newBuilder()
                            .setCurrency(currency)
                            .setAmount(amount.toPlainString())
                            .build())
                    .setReferenceId("fx-reverse-" + transactionId)
                    .setDescription("FX reversal")
                    .build();

            TransactionResponse response = walletStub.credit(request);

            if (response.getSuccess()) {
                log.info("gRPC reverseDebit successful: accountId={}, txId={}", accountId, response.getTransactionId());
            } else {
                log.error("gRPC reverseDebit failed: error={}", response.getError().getMessage());
            }
        } catch (StatusRuntimeException e) {
            log.error("CRITICAL: gRPC reverseDebit error: accountId={}, amount={} {}: status={}, message={}",
                    accountId, amount, currency, e.getStatus(), e.getMessage());
            // At this point, manual intervention may be needed.
        }
    }
}
