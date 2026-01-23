package id.payu.transaction.dto;

import id.payu.transaction.domain.model.SplitBill;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateSplitBillRequest {
    @NotNull(message = "Creator account ID is required")
    private UUID creatorAccountId;

    @NotNull(message = "Total amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
    private BigDecimal totalAmount;

    private String currency;

    @NotBlank(message = "Title is required")
    private String title;

    private String description;

    @NotNull(message = "Split type is required")
    private SplitBill.SplitType splitType;

    private Instant dueDate;

    @NotEmpty(message = "At least one participant is required")
    private List<ParticipantRequest> participants;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ParticipantRequest {
        @NotNull(message = "Account ID is required")
        private UUID accountId;

        @NotBlank(message = "Account number is required")
        private String accountNumber;

        @NotBlank(message = "Account name is required")
        private String accountName;

        private BigDecimal amountOwed;

        private BigDecimal percentage;

        private Boolean isCreator;
    }
}
