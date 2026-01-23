package id.payu.transaction.dto;

import id.payu.transaction.domain.model.SplitBill;
import id.payu.transaction.domain.model.SplitBillParticipant;
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
public class SplitBillResponse {
    private UUID id;
    private String referenceNumber;
    private UUID creatorAccountId;
    private BigDecimal totalAmount;
    private String currency;
    private String title;
    private String description;
    private String splitType;
    private String status;
    private Instant dueDate;
    private List<ParticipantResponse> participants;
    private BigDecimal totalPaid;
    private BigDecimal remainingAmount;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant completedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ParticipantResponse {
        private UUID id;
        private UUID accountId;
        private String accountNumber;
        private String accountName;
        private BigDecimal amountOwed;
        private BigDecimal amountPaid;
        private BigDecimal remainingAmount;
        private String status;
        private Instant settledAt;
        private Instant createdAt;
        private Instant updatedAt;
    }
}
