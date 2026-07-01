package id.payu.wallet.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public class SavingsGoal {

    private UUID id;
    private UUID pocketId;
    private UUID userId;
    private String name;
    private String description;
    private BigDecimal targetAmount;
    private BigDecimal currentAmount;
    private String currency;
    private LocalDate deadline;
    private SavingsGoalStatus status;
    private String icon;
    private String color;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime completedAt;

    public SavingsGoal() {
    }

    public SavingsGoal(UUID id, UUID pocketId, UUID userId, String name, String description,
                       BigDecimal targetAmount, BigDecimal currentAmount, String currency,
                       LocalDate deadline, SavingsGoalStatus status, String icon, String color,
                       LocalDateTime createdAt, LocalDateTime updatedAt, LocalDateTime completedAt) {
        this.id = id;
        this.pocketId = pocketId;
        this.userId = userId;
        this.name = name;
        this.description = description;
        this.targetAmount = targetAmount;
        this.currentAmount = currentAmount != null ? currentAmount : BigDecimal.ZERO;
        this.currency = currency;
        this.deadline = deadline;
        this.status = status != null ? status : SavingsGoalStatus.ACTIVE;
        this.icon = icon;
        this.color = color;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.completedAt = completedAt;
    }

    /**
     * Calculate progress percentage (current / target * 100)
     */
    public BigDecimal calculateProgressPercentage() {
        if (targetAmount == null || targetAmount.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return currentAmount.multiply(BigDecimal.valueOf(100))
                .divide(targetAmount, 2, RoundingMode.HALF_EVEN);
    }

    /**
     * Update current amount and check if goal is completed
     */
    public void updateCurrentAmount(BigDecimal newAmount) {
        this.currentAmount = newAmount;
        this.updatedAt = LocalDateTime.now();

        if (currentAmount.compareTo(targetAmount) >= 0 && status == SavingsGoalStatus.ACTIVE) {
            complete();
        }
    }

    /**
     * Mark goal as completed
     */
    public void complete() {
        this.status = SavingsGoalStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Pause the savings goal
     */
    public void pause() {
        if (status == SavingsGoalStatus.ACTIVE) {
            this.status = SavingsGoalStatus.PAUSED;
            this.updatedAt = LocalDateTime.now();
        }
    }

    /**
     * Resume the savings goal
     */
    public void resume() {
        if (status == SavingsGoalStatus.PAUSED) {
            this.status = SavingsGoalStatus.ACTIVE;
            this.updatedAt = LocalDateTime.now();
        }
    }

    /**
     * Cancel the savings goal
     */
    public void cancel() {
        if (status != SavingsGoalStatus.COMPLETED) {
            this.status = SavingsGoalStatus.CANCELLED;
            this.updatedAt = LocalDateTime.now();
        }
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getPocketId() { return pocketId; }
    public void setPocketId(UUID pocketId) { this.pocketId = pocketId; }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public BigDecimal getTargetAmount() { return targetAmount; }
    public void setTargetAmount(BigDecimal targetAmount) { this.targetAmount = targetAmount; }

    public BigDecimal getCurrentAmount() { return currentAmount; }
    public void setCurrentAmount(BigDecimal currentAmount) { this.currentAmount = currentAmount; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public LocalDate getDeadline() { return deadline; }
    public void setDeadline(LocalDate deadline) { this.deadline = deadline; }

    public SavingsGoalStatus getStatus() { return status; }
    public void setStatus(SavingsGoalStatus status) { this.status = status; }

    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }

    // Builder
    public static SavingsGoalBuilder builder() {
        return new SavingsGoalBuilder();
    }

    public static class SavingsGoalBuilder {
        private UUID id;
        private UUID pocketId;
        private UUID userId;
        private String name;
        private String description;
        private BigDecimal targetAmount;
        private BigDecimal currentAmount;
        private String currency;
        private LocalDate deadline;
        private SavingsGoalStatus status;
        private String icon;
        private String color;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private LocalDateTime completedAt;

        SavingsGoalBuilder() {}

        public SavingsGoalBuilder id(UUID id) { this.id = id; return this; }
        public SavingsGoalBuilder pocketId(UUID pocketId) { this.pocketId = pocketId; return this; }
        public SavingsGoalBuilder userId(UUID userId) { this.userId = userId; return this; }
        public SavingsGoalBuilder name(String name) { this.name = name; return this; }
        public SavingsGoalBuilder description(String description) { this.description = description; return this; }
        public SavingsGoalBuilder targetAmount(BigDecimal targetAmount) { this.targetAmount = targetAmount; return this; }
        public SavingsGoalBuilder currentAmount(BigDecimal currentAmount) { this.currentAmount = currentAmount; return this; }
        public SavingsGoalBuilder currency(String currency) { this.currency = currency; return this; }
        public SavingsGoalBuilder deadline(LocalDate deadline) { this.deadline = deadline; return this; }
        public SavingsGoalBuilder status(SavingsGoalStatus status) { this.status = status; return this; }
        public SavingsGoalBuilder icon(String icon) { this.icon = icon; return this; }
        public SavingsGoalBuilder color(String color) { this.color = color; return this; }
        public SavingsGoalBuilder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public SavingsGoalBuilder updatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; return this; }
        public SavingsGoalBuilder completedAt(LocalDateTime completedAt) { this.completedAt = completedAt; return this; }

        public SavingsGoal build() {
            return new SavingsGoal(id, pocketId, userId, name, description, targetAmount,
                    currentAmount, currency, deadline, status, icon, color,
                    createdAt, updatedAt, completedAt);
        }
    }
}
