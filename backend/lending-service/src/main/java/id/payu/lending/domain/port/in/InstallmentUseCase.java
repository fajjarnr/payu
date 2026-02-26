package id.payu.lending.domain.port.in;

import id.payu.lending.domain.model.InstallmentCheckout;
import id.payu.lending.domain.model.InstallmentOption;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Input port for installment checkout operations (GAP-012).
 * Gateway-facing use cases for PayLater installment payments.
 */
public interface InstallmentUseCase {

    /**
     * Get available installment tenor options for a given purchase amount.
     * Returns calculated monthly payments for each available tenor.
     *
     * @param userId  the user requesting options
     * @param amount  the purchase amount to split into installments
     * @return list of available tenor options with simulated payments
     */
    List<InstallmentOption> getTenorOptions(UUID userId, BigDecimal amount);

    /**
     * Create an installment checkout — convert a purchase into an installment loan.
     * Validates PayLater eligibility, checks credit limit, creates a loan, and
     * generates the repayment schedule.
     *
     * @param userId          the authenticated user
     * @param partnerId       the merchant/partner ID
     * @param externalOrderId external order reference from the partner
     * @param amount          purchase amount
     * @param tenor           selected tenor (number of monthly installments)
     * @return the created checkout with loan details
     */
    InstallmentCheckout checkout(UUID userId, String partnerId, String externalOrderId,
                                  BigDecimal amount, int tenor);

    /**
     * Get an installment checkout by ID.
     */
    InstallmentCheckout getCheckout(UUID checkoutId);

    /**
     * Get all installment checkouts for a user.
     */
    List<InstallmentCheckout> getCheckoutsByUser(UUID userId);
}
