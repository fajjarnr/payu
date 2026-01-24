package id.payu.statement.service.dto;

import id.payu.statement.domain.entity.Statement;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for statement
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StatementResponse {

    private UUID id;
    private UUID userId;
    private String accountNumber;
    private LocalDate statementPeriod;
    private BigDecimal openingBalance;
    private BigDecimal closingBalance;
    private BigDecimal totalCredits;
    private BigDecimal totalDebits;
    private Integer transactionCount;
    private Statement.StatementStatus status;
    private LocalDateTime generatedAt;
    private LocalDateTime createdAt;

    // Formatted fields for UI
    private String periodFormatted;
    private String openingBalanceFormatted;
    private String closingBalanceFormatted;
    private String totalCreditsFormatted;
    private String totalDebitsFormatted;
    private String downloadUrl;

    /**
     * Create formatted response from entity
     */
    public static StatementResponse fromEntity(Statement entity, String baseUrl) {
        StatementResponse response = StatementResponse.builder()
            .id(entity.getId())
            .userId(entity.getUserId())
            .accountNumber(entity.getAccountNumber())
            .statementPeriod(entity.getStatementPeriod())
            .openingBalance(entity.getOpeningBalance())
            .closingBalance(entity.getClosingBalance())
            .totalCredits(entity.getTotalCredits())
            .totalDebits(entity.getTotalDebits())
            .transactionCount(entity.getTransactionCount())
            .status(entity.getStatus())
            .generatedAt(entity.getGeneratedAt())
            .createdAt(entity.getCreatedAt())
            .build();

        // Format period
        if (entity.getStatementPeriod() != null) {
            response.periodFormatted = entity.getStatementPeriod().format(
                java.time.format.DateTimeFormatter.ofPattern("MMMM yyyy"));
        }

        // Format amounts
        response.openingBalanceFormatted = formatCurrency(entity.getOpeningBalance());
        response.closingBalanceFormatted = formatCurrency(entity.getClosingBalance());
        response.totalCreditsFormatted = formatCurrency(entity.getTotalCredits());
        response.totalDebitsFormatted = formatCurrency(entity.getTotalDebits());

        // Generate download URL
        response.downloadUrl = baseUrl + "/api/v1/statements/" + entity.getId() + "/download";

        return response;
    }

    private static String formatCurrency(BigDecimal amount) {
        if (amount == null) {
            return "Rp 0";
        }
        return "Rp " + amount.setScale(0, java.math.RoundingMode.HALF_UP)
            .toBigInteger()
            .toString()
            .replaceAll("\\B(?=(\\d{3})+(?!\\d))", ".");
    }
}
