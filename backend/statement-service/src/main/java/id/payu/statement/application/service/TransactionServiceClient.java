package id.payu.statement.application.service;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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
                        "CREDIT".equals(t.getType()) ? StatementService.TransactionType.CREDIT : StatementService.TransactionType.DEBIT
                    ))
                    .toList();
            }

            return new ArrayList<>();
        } catch (Exception e) {
            // BUG-BE-053: Do NOT silently swallow — log and propagate so statement fails explicitly
            throw new RuntimeException("Failed to fetch transactions for customer " + customerId + ": " + e.getMessage(), e);
        }
    }

    @Data
    private static class TransactionListResponse {
        private List<TransactionDto> transactions;
    }

    @Data
    private static class TransactionDto {
        private LocalDate date;
        private String description;
        private String type;
        private java.math.BigDecimal amount;
    }
}
