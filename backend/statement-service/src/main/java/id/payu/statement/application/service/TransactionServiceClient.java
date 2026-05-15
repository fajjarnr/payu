package id.payu.statement.application.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Client for Transaction Service - Feign could be used alternatively
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
     * Get transactions for a user within a date range
     */
    public List<StatementService.TransactionRecord> getTransactions(String customerId, LocalDate startDate, LocalDate endDate) {
        String url = transactionServiceUrl + "/api/v1/transactions/customer/" + customerId
            + "?startDate=" + startDate + "&endDate=" + endDate;

        try {
            TransactionListResponse response = restTemplate.getForObject(url, TransactionListResponse.class);

            if (response != null && response.getTransactions() != null) {
                return response.getTransactions().stream()
                    .map(t -> new StatementService.TransactionRecord(
                        t.getDate(),
                        t.getDescription(),
                        t.getAmount(),
                        "CREDIT".equals(t.getType()) ? TransactionType.CREDIT : TransactionType.DEBIT
                    ))
                    .toList();
            }

            return new ArrayList<>();
        } catch (Exception e) {
            // BUG-BE-053: Do NOT silently swallow — log and propagate so statement fails explicitly
            throw new RuntimeException("Failed to fetch transactions for customer " + customerId + ": " + e.getMessage(), e);
        }
    }

    /**
     * Get single transaction by ID.
     */
    public StatementService.TransactionRecord getTransaction(String transactionId) {
        String url = transactionServiceUrl + "/api/v1/transactions/" + transactionId;

        try {
            TransactionDto response = restTemplate.getForObject(url, TransactionDto.class);

            if (response != null) {
                return new StatementService.TransactionRecord(
                    response.getDate(),
                    response.getDescription(),
                    response.getAmount(),
                    "CREDIT".equals(response.getType()) ? TransactionType.CREDIT : TransactionType.DEBIT
                );
            }

            throw new RuntimeException("Transaction not found: " + transactionId);
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch transaction " + transactionId + ": " + e.getMessage(), e);
        }
    }

    private static class TransactionListResponse {
        private List<TransactionDto> transactions;

        public List<TransactionDto> getTransactions() {
            return transactions;
        }

        public void setTransactions(List<TransactionDto> transactions) {
            this.transactions = transactions;
        }
    }

    private static class TransactionDto {
        private LocalDate date;
        private String description;
        private String type;
        private java.math.BigDecimal amount;

        public LocalDate getDate() {
            return date;
        }

        public void setDate(LocalDate date) {
            this.date = date;
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

        public java.math.BigDecimal getAmount() {
            return amount;
        }

        public void setAmount(java.math.BigDecimal amount) {
            this.amount = amount;
        }
    }
}
