package id.payu.lending.domain.model;

import java.math.BigDecimal;

/**
 * Represents a single installment tenor option with calculated monthly payment.
 * Used by gateway-facing APIs to show available PayLater installment plans.
 */
public class InstallmentOption {

    private int tenor;
    private BigDecimal monthlyPayment;
    private BigDecimal totalPayment;
    private BigDecimal totalInterest;
    private BigDecimal interestRate;

    public InstallmentOption() {}

    public InstallmentOption(int tenor, BigDecimal monthlyPayment, BigDecimal totalPayment,
                              BigDecimal totalInterest, BigDecimal interestRate) {
        this.tenor = tenor;
        this.monthlyPayment = monthlyPayment;
        this.totalPayment = totalPayment;
        this.totalInterest = totalInterest;
        this.interestRate = interestRate;
    }

    public int getTenor() {
        return tenor;
    }

    public void setTenor(int tenor) {
        this.tenor = tenor;
    }

    public BigDecimal getMonthlyPayment() {
        return monthlyPayment;
    }

    public void setMonthlyPayment(BigDecimal monthlyPayment) {
        this.monthlyPayment = monthlyPayment;
    }

    public BigDecimal getTotalPayment() {
        return totalPayment;
    }

    public void setTotalPayment(BigDecimal totalPayment) {
        this.totalPayment = totalPayment;
    }

    public BigDecimal getTotalInterest() {
        return totalInterest;
    }

    public void setTotalInterest(BigDecimal totalInterest) {
        this.totalInterest = totalInterest;
    }

    public BigDecimal getInterestRate() {
        return interestRate;
    }

    public void setInterestRate(BigDecimal interestRate) {
        this.interestRate = interestRate;
    }
}
