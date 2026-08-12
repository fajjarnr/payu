package id.payu.statement.adapter.client;

import id.payu.grpc.starter.config.GrpcChannelSupport;
import id.payu.statement.dto.TransactionRecord;
import id.payu.statement.dto.TransactionType;
import io.grpc.ManagedChannel;
import io.grpc.StatusRuntimeException;
import id.payu.transaction.grpc.GetHistoryRequest;
import id.payu.transaction.grpc.GetTransactionRequest;
import id.payu.transaction.grpc.TransactionResponse;
import id.payu.transaction.grpc.TransactionServiceGrpc;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

/**
 * Client for Transaction Service over gRPC (GRPC-005).
 * Replaces the REST client — the TransactionService gRPC server (GRPC-002)
 * is live; statement no longer does synchronous REST lookups.
 */
@Component
public class TransactionServiceClient {

    private ManagedChannel channel;
    private TransactionServiceGrpc.TransactionServiceBlockingStub stub;

    @Value("${payu.grpc.clients.transaction-service.address:static://transaction-service:9090}")
    private String transactionServiceAddress;

    @PostConstruct
    void init() {
        channel = GrpcChannelSupport.channel(transactionServiceAddress);
        stub = GrpcChannelSupport.withDeadline(
                TransactionServiceGrpc.newBlockingStub(channel),
                GrpcChannelSupport.DEFAULT_DEADLINE_SECONDS);
    }

    @PreDestroy
    void destroy() {
        if (channel != null) {
            channel.shutdown();
        }
    }

    /**
     * Get transactions for an account within a date range.
     * The server returns the account history; date filtering happens here
     * (statement-period semantics are a statement concern).
     */
    public List<TransactionRecord> getTransactions(String accountId, LocalDate startDate, LocalDate endDate) {
        try {
            List<TransactionRecord> records = new ArrayList<>();
            GetHistoryRequest request = GetHistoryRequest.newBuilder()
                    .setAccountId(accountId)
                    .setPage(id.payu.grpc.common.PageRequest.newBuilder().setPage(0).setSize(500).build())
                    .build();
            stub.getHistory(request).forEachRemaining(tx -> {
                LocalDate date = toLocalDate(tx.getCreatedAt());
                if (date != null && !date.isBefore(startDate) && !date.isAfter(endDate)) {
                    records.add(new TransactionRecord(
                            date,
                            tx.getDescription(),
                            new java.math.BigDecimal(tx.getAmount().getAmount()),
                            tx.getType() == id.payu.transaction.grpc.TransactionType.CREDIT
                    ? TransactionType.CREDIT : TransactionType.DEBIT
                    ));
                }
            });
            return records;
        } catch (StatusRuntimeException e) {
            // BUG-BE-053: do not silently swallow — fail explicitly so statement fails visibly
            throw new RuntimeException("Failed to fetch transactions for account " + accountId + ": " + e.getStatus(), e);
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch transactions for account " + accountId + ": " + e.getMessage(), e);
        }
    }

    /**
     * Get single transaction by ID.
     */
    public TransactionRecord getTransaction(String transactionId) {
        try {
            TransactionResponse tx = stub.getTransaction(GetTransactionRequest.newBuilder()
                    .setTransactionId(transactionId)
                    .build());
            return new TransactionRecord(
                    toLocalDate(tx.getCreatedAt()),
                    tx.getDescription(),
                    new java.math.BigDecimal(tx.getAmount().getAmount()),
                    tx.getType() == id.payu.transaction.grpc.TransactionType.CREDIT
                    ? TransactionType.CREDIT : TransactionType.DEBIT
            );
        } catch (StatusRuntimeException e) {
            throw new RuntimeException("Failed to fetch transaction " + transactionId + ": " + e.getStatus(), e);
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch transaction " + transactionId + ": " + e.getMessage(), e);
        }
    }

    private static LocalDate toLocalDate(id.payu.grpc.common.Timestamp timestamp) {
        if (timestamp == null || timestamp.getSeconds() == 0) {
            return null;
        }
        return Instant.ofEpochSecond(timestamp.getSeconds(), timestamp.getNanos())
                .atZone(ZoneId.systemDefault()).toLocalDate();
    }
}
