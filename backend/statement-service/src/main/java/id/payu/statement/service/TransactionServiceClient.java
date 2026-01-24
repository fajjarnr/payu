package id.payu.statement.service;

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

    public TransactionServiceClient() {
        this.restTemplate = new RestTemplate();
    }

    /**
     * Get transactions for a user within a date range
     */
    public List<StatementService.TransactionRecord> getTransactions(UUID userId, LocalDate startDate, LocalDate endDate) {
        try {
            String url = transactionServiceUrl + "/api/v1/transactions/user/" + userId
                + "?startDate=" + startDate + "&endDate=" + endDate;

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

        } catch (Exception e) {
            // Return empty list on error
        }

        return new ArrayList<>();
    }

    private record TransactionListResponse(List<TransactionDto> transactions) {}

    private record TransactionDto(
        LocalDate date,
        String description,
        String type,
        java.math.BigDecimal amount
    ) {}
}
