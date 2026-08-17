package id.payu.statement.interfaces.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Statement transaction record (GRPC-005: moved out of StatementService so
 * client adapters can produce it without depending on the application layer).
 */
@lombok.Data
@lombok.AllArgsConstructor
public class TransactionRecord {
    private LocalDate date;
    private String description;
    private BigDecimal amount;
    private TransactionType type;
}
