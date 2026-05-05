package id.payu.transaction.domain.model;

import java.time.Duration;

/**
 * Value Object representing a transfer route option with fee and timing information.
 *
 * <p>This class encapsulates the characteristics of a transfer method including
 * fees, processing time, and amount limits. It provides methods for eligibility
 * checking and comparison for smart routing decisions.</p>
 *
 * <p>Implements {@link Comparable} to allow sorting by fee (cheapest first), which
 * is the default ranking strategy for smart routing.</p>
 *
 * <p>PCI-DSS Compliance:</p>
 * <ul>
 *   <li>Uses Money Value Object for precise fee calculations</li>
 *   <li>Immutable to prevent accidental modifications</li>
 * </ul>
 *
 * @see TransferMethod
 * @see Money
 */
public class TransferRoute implements Comparable<TransferRoute> {

    private final TransferMethod method;
    private final Money fee;
    private final Duration estimatedTime;
    private final Money minAmount;
    private final Money maxAmount;

    public TransferRoute() {
        this.method = null;
        this.fee = null;
        this.estimatedTime = null;
        this.minAmount = null;
        this.maxAmount = null;
    }

    public TransferRoute(TransferMethod method, Money fee, Duration estimatedTime, Money minAmount, Money maxAmount) {
        this.method = method;
        this.fee = fee;
        this.estimatedTime = estimatedTime;
        this.minAmount = minAmount;
        this.maxAmount = maxAmount;
    }

    public static TransferRouteBuilder builder() {
        return new TransferRouteBuilder();
    }

    public TransferMethod getMethod() {
        return method;
    }

    public Money getFee() {
        return fee;
    }

    public Duration getEstimatedTime() {
        return estimatedTime;
    }

    public Money getMinAmount() {
        return minAmount;
    }

    public Money getMaxAmount() {
        return maxAmount;
    }

    public static class TransferRouteBuilder {
        private TransferMethod method;
        private Money fee;
        private Duration estimatedTime;
        private Money minAmount;
        private Money maxAmount;

        public TransferRouteBuilder method(TransferMethod method) {
            this.method = method;
            return this;
        }

        public TransferRouteBuilder fee(Money fee) {
            this.fee = fee;
            return this;
        }

        public TransferRouteBuilder estimatedTime(Duration estimatedTime) {
            this.estimatedTime = estimatedTime;
            return this;
        }

        public TransferRouteBuilder minAmount(Money minAmount) {
            this.minAmount = minAmount;
            return this;
        }

        public TransferRouteBuilder maxAmount(Money maxAmount) {
            this.maxAmount = maxAmount;
            return this;
        }

        public TransferRoute build() {
            return new TransferRoute(method, fee, estimatedTime, minAmount, maxAmount);
        }
    }

    /**
     * Creates a standard BI-FAST route with default parameters.
     *
     * @return a BI-FAST transfer route
     */
    public static TransferRoute biFast() {
        return TransferRoute.builder()
                .method(TransferMethod.BI_FAST)
                .fee(Money.idr("2500"))
                .estimatedTime(Duration.ofSeconds(30))
                .minAmount(Money.idr("1"))
                .maxAmount(Money.idr("50000000"))
                .build();
    }

    /**
     * Creates a standard RTGS route with default parameters.
     *
     * @return an RTGS transfer route
     */
    public static TransferRoute rtgs() {
        return TransferRoute.builder()
                .method(TransferMethod.RTGS)
                .fee(Money.idr("25000"))
                .estimatedTime(Duration.ofMinutes(5))
                .minAmount(Money.idr("100000000"))
                .maxAmount(Money.idr("10000000000"))
                .build();
    }

    /**
     * Creates a standard SKN route with default parameters.
     *
     * @return an SKN transfer route
     */
    public static TransferRoute skn() {
        return TransferRoute.builder()
                .method(TransferMethod.SKN)
                .fee(Money.idr("5000"))
                .estimatedTime(Duration.ofHours(4))
                .minAmount(Money.idr("1"))
                .maxAmount(Money.idr("1000000000"))
                .build();
    }

    /**
     * Checks if this route is eligible for the given amount.
     * Eligibility requires:
     * <ul>
     *   <li>Amount must be within [minAmount, maxAmount] range</li>
     *   <li>Amount must have the same currency as the route limits</li>
     * </ul>
     *
     * @param amount the amount to check
     * @return true if the route can handle this amount
     * @throws IllegalArgumentException if amount is null
     */
    public boolean isEligibleFor(Money amount) {
        if (amount == null) {
            throw new IllegalArgumentException("Amount cannot be null");
        }

        // Currency must match
        if (!amount.getCurrency().equals(minAmount.getCurrency())) {
            return false;
        }

        // Amount must be >= minAmount and <= maxAmount
        return !amount.isLessThan(minAmount) && !amount.isGreaterThan(maxAmount);
    }

    /**
     * Calculates the total amount including fees.
     *
     * @param amount the transfer amount
     * @return the total amount (transfer amount + fee)
     * @throws IllegalArgumentException if amount is null or has different currency
     */
    public Money calculateTotalAmount(Money amount) {
        if (amount == null) {
            throw new IllegalArgumentException("Amount cannot be null");
        }
        return amount.add(fee);
    }

    /**
     * Gets a human-readable display string for the estimated time.
     *
     * @return formatted estimated time (e.g., "30 seconds", "5 minutes")
     */
    public String getEstimatedTimeDisplay() {
        if (estimatedTime.getSeconds() < 60) {
            return estimatedTime.getSeconds() + " seconds";
        } else if (estimatedTime.toMinutes() < 60) {
            return estimatedTime.toMinutes() + " minutes";
        } else {
            return estimatedTime.toHours() + " hours";
        }
    }

    /**
     * Compares this route with another based on fee (cheapest first).
     * This enables sorting routes by cost for smart routing decisions.
     *
     * @param other the route to compare with
     * @return negative if this route is cheaper, positive if more expensive,
     *         zero if fees are equal
     */
    @Override
    public int compareTo(TransferRoute other) {
        return this.fee.compareTo(other.fee);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TransferRoute that = (TransferRoute) o;
        return method == that.method &&
                java.util.Objects.equals(fee, that.fee) &&
                java.util.Objects.equals(estimatedTime, that.estimatedTime) &&
                java.util.Objects.equals(minAmount, that.minAmount) &&
                java.util.Objects.equals(maxAmount, that.maxAmount);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(method, fee, estimatedTime, minAmount, maxAmount);
    }

    @Override
    public String toString() {
        return "TransferRoute{" +
                "method=" + method +
                ", fee=" + fee +
                ", estimatedTime=" + estimatedTime +
                ", minAmount=" + minAmount +
                ", maxAmount=" + maxAmount +
                '}';
    }
}
