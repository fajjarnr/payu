package id.payu.lending.domain.model;

import java.math.BigDecimal;

public class CreditScoringFact {
    private String kycStatus;
    private int tenureMonths;
    private int totalTransactions;
    private BigDecimal totalAmount;
    private BigDecimal successRate;
    private BigDecimal score;

    public CreditScoringFact() {}

    public CreditScoringFact(String kycStatus, int tenureMonths, int totalTransactions, BigDecimal totalAmount, BigDecimal successRate, BigDecimal score) {
        this.kycStatus = kycStatus;
        this.tenureMonths = tenureMonths;
        this.totalTransactions = totalTransactions;
        this.totalAmount = totalAmount;
        this.successRate = successRate;
        this.score = score;
    }

    public String getKycStatus() { return kycStatus; }
    public void setKycStatus(String kycStatus) { this.kycStatus = kycStatus; }

    public int getTenureMonths() { return tenureMonths; }
    public void setTenureMonths(int tenureMonths) { this.tenureMonths = tenureMonths; }

    public int getTotalTransactions() { return totalTransactions; }
    public void setTotalTransactions(int totalTransactions) { this.totalTransactions = totalTransactions; }

    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }

    public BigDecimal getSuccessRate() { return successRate; }
    public void setSuccessRate(BigDecimal successRate) { this.successRate = successRate; }

    public BigDecimal getScore() { return score; }
    public void setScore(BigDecimal score) { this.score = score; }

    public void addScore(BigDecimal value) {
        if (this.score == null) {
            this.score = value;
        } else {
            this.score = this.score.add(value);
        }
    }

    public void subtractScore(BigDecimal value) {
        if (this.score == null) {
            this.score = value.negate();
        } else {
            this.score = this.score.subtract(value);
        }
    }
}
