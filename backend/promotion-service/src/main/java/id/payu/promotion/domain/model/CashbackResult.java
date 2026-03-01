package id.payu.promotion.domain.model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Value object representing the result of cashback processing.
 */
public class CashbackResult {

    private final boolean success;
    private final int processedCount;
    private final BigDecimal totalCashbackAmount;
    private final List<String> processedRuleIds;
    private final String errorMessage;

    private CashbackResult(Builder builder) {
        this.success = builder.success;
        this.processedCount = builder.processedCount;
        this.totalCashbackAmount = builder.totalCashbackAmount;
        this.processedRuleIds = builder.processedRuleIds;
        this.errorMessage = builder.errorMessage;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static CashbackResult empty() {
        return builder()
                .success(true)
                .processedCount(0)
                .totalCashbackAmount(BigDecimal.ZERO)
                .build();
    }

    public boolean isSuccess() {
        return success;
    }

    public int getProcessedCount() {
        return processedCount;
    }

    public BigDecimal getTotalCashbackAmount() {
        return totalCashbackAmount;
    }

    public List<String> getProcessedRuleIds() {
        return processedRuleIds;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public static class Builder {
        private boolean success;
        private int processedCount;
        private BigDecimal totalCashbackAmount = BigDecimal.ZERO;
        private List<String> processedRuleIds = new ArrayList<>();
        private String errorMessage;

        public Builder success(boolean success) {
            this.success = success;
            return this;
        }

        public Builder processedCount(int processedCount) {
            this.processedCount = processedCount;
            return this;
        }

        public Builder totalCashbackAmount(BigDecimal totalCashbackAmount) {
            this.totalCashbackAmount = totalCashbackAmount;
            return this;
        }

        public Builder processedRuleIds(List<String> processedRuleIds) {
            this.processedRuleIds = processedRuleIds;
            return this;
        }

        public Builder errorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
            return this;
        }

        public CashbackResult build() {
            return new CashbackResult(this);
        }
    }
}
