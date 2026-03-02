package id.payu.transaction.dto;

import id.payu.transaction.domain.model.TransferMethod;
import id.payu.transaction.domain.model.TransferRoute;
import java.math.BigDecimal;

/**
 * Response DTO for transfer route information.
 */
public class TransferRouteResponse {
    public TransferRouteResponse() {
    }

    public TransferRouteResponse(TransferMethod method, String methodDisplay, BigDecimal fee, String currency, String estimatedTime, BigDecimal minAmount, BigDecimal maxAmount, boolean eligible) {
        this.method = method;
        this.methodDisplay = methodDisplay;
        this.fee = fee;
        this.currency = currency;
        this.estimatedTime = estimatedTime;
        this.minAmount = minAmount;
        this.maxAmount = maxAmount;
        this.eligible = eligible;
    }

    public static TransferRouteResponseBuilder builder() {
        return new TransferRouteResponseBuilder();
    }

    public static class TransferRouteResponseBuilder {
        private TransferMethod method;
        private String methodDisplay;
        private BigDecimal fee;
        private String currency;
        private String estimatedTime;
        private BigDecimal minAmount;
        private BigDecimal maxAmount;
        private boolean eligible;

        public TransferRouteResponseBuilder method(TransferMethod method) {
            this.method = method;
            return this;
        }
        public TransferRouteResponseBuilder methodDisplay(String methodDisplay) {
            this.methodDisplay = methodDisplay;
            return this;
        }
        public TransferRouteResponseBuilder fee(BigDecimal fee) {
            this.fee = fee;
            return this;
        }
        public TransferRouteResponseBuilder currency(String currency) {
            this.currency = currency;
            return this;
        }
        public TransferRouteResponseBuilder estimatedTime(String estimatedTime) {
            this.estimatedTime = estimatedTime;
            return this;
        }
        public TransferRouteResponseBuilder minAmount(BigDecimal minAmount) {
            this.minAmount = minAmount;
            return this;
        }
        public TransferRouteResponseBuilder maxAmount(BigDecimal maxAmount) {
            this.maxAmount = maxAmount;
            return this;
        }
        public TransferRouteResponseBuilder eligible(boolean eligible) {
            this.eligible = eligible;
            return this;
        }

        public TransferRouteResponse build() {
            return new TransferRouteResponse(method, methodDisplay, fee, currency, estimatedTime, minAmount, maxAmount, eligible);
        }
    }

    public TransferMethod getMethod() {
        return method;
    }

    public void setMethod(TransferMethod method) {
        this.method = method;
    }

    public String getMethodDisplay() {
        return methodDisplay;
    }

    public void setMethodDisplay(String methodDisplay) {
        this.methodDisplay = methodDisplay;
    }

    public BigDecimal getFee() {
        return fee;
    }

    public void setFee(BigDecimal fee) {
        this.fee = fee;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getEstimatedTime() {
        return estimatedTime;
    }

    public void setEstimatedTime(String estimatedTime) {
        this.estimatedTime = estimatedTime;
    }

    public BigDecimal getMinAmount() {
        return minAmount;
    }

    public void setMinAmount(BigDecimal minAmount) {
        this.minAmount = minAmount;
    }

    public BigDecimal getMaxAmount() {
        return maxAmount;
    }

    public void setMaxAmount(BigDecimal maxAmount) {
        this.maxAmount = maxAmount;
    }

    public boolean isEligible() {
        return eligible;
    }

    public void setEligible(boolean eligible) {
        this.eligible = eligible;
    }



    private TransferMethod method;
    private String methodDisplay;
    private BigDecimal fee;
    private String currency;
    private String estimatedTime;
    private BigDecimal minAmount;
    private BigDecimal maxAmount;
    private boolean eligible;

    /**
     * Creates a response DTO from a domain entity.
     *
     * @param route the domain entity
     * @param eligible whether this route is eligible for the requested amount
     * @return the response DTO
     */
    public static TransferRouteResponse fromEntity(TransferRoute route, boolean eligible) {
        return TransferRouteResponse.builder()
                .method(route.getMethod())
                .methodDisplay(formatMethodName(route.getMethod()))
                .fee(route.getFee().getAmount())
                .currency(route.getFee().getCurrency().getCurrencyCode())
                .estimatedTime(route.getEstimatedTimeDisplay())
                .minAmount(route.getMinAmount().getAmount())
                .maxAmount(route.getMaxAmount().getAmount())
                .eligible(eligible)
                .build();
    }

    private static String formatMethodName(TransferMethod method) {
        return switch (method) {
            case BI_FAST -> "BI-FAST (Real-time)";
            case RTGS -> "RTGS (High Value)";
            case SKN -> "SKN (Batch)";
        };
    }
}
