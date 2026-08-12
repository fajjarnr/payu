package id.payu.lending.adapter.client;

import id.payu.grpc.common.Money;
import id.payu.lending.domain.port.out.WalletPaymentPort;
import id.payu.wallet.grpc.LoanRepaymentRequest;
import id.payu.wallet.grpc.TransactionResponse;
import id.payu.wallet.grpc.WalletServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Component
public class WalletGrpcPaymentAdapter implements WalletPaymentPort {

    @Value("${payu.grpc.clients.wallet-service.address:static://wallet-service:9090}")
    private String walletServiceAddress;

    private ManagedChannel channel;
    private WalletServiceGrpc.WalletServiceBlockingStub walletStub;

    @PostConstruct
    void init() {
        String[] parts = walletServiceAddress.replace("static://", "").split(":");
        channel = ManagedChannelBuilder.forAddress(parts[0], parts.length > 1 ? Integer.parseInt(parts[1]) : 9090)
                .usePlaintext()
                .build();
        walletStub = WalletServiceGrpc.newBlockingStub(channel);
    }

    @PreDestroy
    void destroy() {
        if (channel != null) {
            channel.shutdown();
        }
    }

    @Override
    public String collectRepayment(UUID loanId, UUID userId, BigDecimal amount, String currency,
                                   String referenceId, String description) {
        try {
            TransactionResponse response = walletStub
                    .withDeadlineAfter(3, TimeUnit.SECONDS)
                    .repayLoan(LoanRepaymentRequest.newBuilder()
                            .setWalletId(userId.toString())
                            .setAccountId(userId.toString())
                            .setAmount(Money.newBuilder()
                                    .setCurrency(currency)
                                    .setAmount(amount.toPlainString())
                                    .build())
                            .setReferenceId(referenceId)
                            .setLoanId(loanId.toString())
                            .setDescription(description)
                            .build());
            if (!response.getSuccess()) {
                throw new IllegalStateException("Wallet rejected loan repayment");
            }
            return response.getTransactionId();
        } catch (StatusRuntimeException e) {
            throw new IllegalStateException("Wallet repayment call failed: " + e.getStatus(), e);
        }
    }

    @Override
    public String creditAccount(String accountId, BigDecimal amount, String currency,
                                String referenceId, String description) {
        try {
            TransactionResponse response = walletStub
                    .withDeadlineAfter(3, TimeUnit.SECONDS)
                    .credit(id.payu.wallet.grpc.CreditRequest.newBuilder()
                            .setWalletId(accountId)
                            .setAccountId(accountId)
                            .setAmount(Money.newBuilder()
                                    .setCurrency(currency)
                                    .setAmount(amount.toPlainString())
                                    .build())
                            .setReferenceId(referenceId)
                            .setDescription(description)
                            .build());
            if (!response.getSuccess()) {
                throw new IllegalStateException("Wallet rejected PayLater credit");
            }
            return response.getTransactionId();
        } catch (StatusRuntimeException e) {
            throw new IllegalStateException("Wallet credit call failed: " + e.getStatus(), e);
        }
    }
}
