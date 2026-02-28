package id.payu.wallet.dto;

import id.payu.wallet.domain.model.SavingsGoal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SavingsGoalResponse {

    private UUID id;
    private UUID pocketId;
    private String name;
    private String description;
    private BigDecimal targetAmount;
    private BigDecimal currentAmount;
    private BigDecimal progressPercentage;
    private String currency;
    private LocalDate deadline;
    private String status;
    private String icon;
    private String color;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;

    public static SavingsGoalResponse from(SavingsGoal goal) {
        if (goal == null) {
            return null;
        }
        return SavingsGoalResponse.builder()
                .id(goal.getId())
                .pocketId(goal.getPocketId())
                .name(goal.getName())
                .description(goal.getDescription())
                .targetAmount(goal.getTargetAmount())
                .currentAmount(goal.getCurrentAmount())
                .progressPercentage(goal.calculateProgressPercentage())
                .currency(goal.getCurrency())
                .deadline(goal.getDeadline())
                .status(goal.getStatus() != null ? goal.getStatus().name() : null)
                .icon(goal.getIcon())
                .color(goal.getColor())
                .createdAt(goal.getCreatedAt())
                .completedAt(goal.getCompletedAt())
                .build();
    }
}
