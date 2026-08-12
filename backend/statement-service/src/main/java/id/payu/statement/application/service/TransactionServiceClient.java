package id.payu.statement.application.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

/**
 * Client for Transaction Service.
 * GRPC-008: aligned with the actual transaction-service contract
 * (GET /api/v1/transactions?accountId=..&startDate=..&endDate=.. and
 * GET /api/v1/transactions/{transactionId}, both wrapped in ApiResponse).
 */
@Component
public class TransactionServiceClient {

    private final RestTemplate restTemplate;

    @Value("${services.transaction.url:http://transaction-service:8003}")
    private String transactionServiceUrl;

    public TransactionServiceClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Get transactions for an account within a date range.
     */
    public List<StatementService.TransactionRecord> getTransactions(String accountId, LocalDate startDate, LocalDate endDate) {
        String url = transactionServiceUrl + "/api/v1/transactions?accountId=" + accountId
            + "&startDate=" + startDate + "&endDate=" + endDate;

        try {
            TransactionListResponse response = restTemplate.getForObject(url, TransactionListResponse.class);

            if (response != null && response.getData() != null) {
                return response.getData().stream()
                    .map(t -> new StatementService.TransactionRecord(
                        toLocalDate(t.getCreatedAt()),
                        t.getDescription(),
                        t.getAmount(),
                        "CREDIT".equals(t.getType()) ? TransactionType.CREDIT : TransactionType.DEBIT
                    ))
                    .toList();
            }

            return new ArrayList<>();
        } catch (Exception e) {
            // BUG-BE-053: Do NOT silently swallow — log and propagate so statement fails explicitly
            throw new RuntimeException("Failed to fetch transactions for account " + accountId + ": " + e.getMessage(), e);
        }
    }

    /**
     * Get single transaction by ID.
     */
    public StatementService.TransactionRecord getTransaction(String transactionId) {
        String url = transactionServiceUrl + "/api/v1/transactions/" + transactionId;

        try {
            TransactionEnvelope response = restTemplate.getForObject(url, TransactionEnvelope.class);

            if (response != null && response.getData() != null) {
                TransactionDto t = response.getData();
                return new StatementService.TransactionRecord(
                    toLocalDate(t.getCreatedAt()),
                    t.getDescription(),
                    t.getAmount(),
                    "CREDIT".equals(t.getType()) ? TransactionType.CREDIT : TransactionType.DEBIT
                );
            }

            throw new RuntimeException("Transaction not found: " + transactionId);
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch transaction " + transactionId + ": " + e.getMessage(), e);
        }
    }

    private static LocalDate toLocalDate(String createdAt) {
        if (createdAt == null || createdAt.isBlank()) {
            return null;
        }
        try {
            return Instant.parse(createdAt).atZone(ZoneId.systemDefault()).toLocalDate();
        } catch (Exception e) {
            return LocalDate.parse(createdAt);
        }
    }

    /** ApiResponse envelope: { success, data, ... } */
    private static class TransactionListResponse {
        private List<TransactionDto> data;

        public List<TransactionDto> getData() {
            return data;
        }

        public void setData(List<TransactionDto> data) {
            this.data = data;
        }
    }

    /** ApiResponse envelope for a single transaction: { success, data: {...} } */
    private static class TransactionEnvelope {
        private TransactionDto data;

        public TransactionDto getData() {
            return data;
        }

        public void setData(TransactionDto data) {
            this.data = data;
        }
    }

    private static class TransactionDto {
        private String createdAt;
        private String description;
        private String type;
        private BigDecimal amount;

        public String getCreatedAt() {
            return createdAt;
        }

        public void setCreatedAt(String createdAt) {
            this.createdAt = createdAt;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public BigDecimal getAmount() {
            return amount;
        }

        public void setAmount(BigDecimal amount) {
            this.amount = amount;
        }
    }
}
