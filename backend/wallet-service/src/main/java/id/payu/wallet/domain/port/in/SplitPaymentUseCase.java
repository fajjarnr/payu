package id.payu.wallet.domain.port.in;

import id.payu.wallet.domain.model.SplitPaymentExecution;
import id.payu.wallet.domain.model.SplitPaymentRule;
import id.payu.wallet.domain.model.SplitRecipient;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import id.payu.wallet.domain.model.SplitType;

/**
 * Input port for split payment use cases.
 * Supports multi-merchant payment splitting for marketplace partners.
 */
public interface SplitPaymentUseCase {

    // --- Rule Management ---

    /**
     * Create a new split payment rule.
     */
    SplitPaymentRule createRule(String partnerId, String ruleName,
                                SplitType splitType, String currency,
                                List<SplitRecipient> recipients);

    /**
     * Get rule by ID.
     */
    SplitPaymentRule getRule(UUID ruleId);

    /**
     * Get rules by partner.
     */
    List<SplitPaymentRule> getRulesByPartner(String partnerId);

    /**
     * Deactivate a rule.
     */
    void deactivateRule(UUID ruleId);

    // --- Execution ---

    /**
     * Execute a split payment using a predefined rule.
     * Atomically debits payer and credits all recipients.
     *
     * @param ruleId              the split rule to apply
     * @param payerAccountId      payer's wallet account ID
     * @param totalAmount         total payment amount
     * @param externalReferenceId partner's external reference
     * @param description         human-readable description
     * @param idempotencyKey      for dedup
     * @return the execution with all legs
     */
    SplitPaymentExecution executeSplit(UUID ruleId, String payerAccountId,
                                       BigDecimal totalAmount,
                                       String externalReferenceId,
                                       String description,
                                       String idempotencyKey);

    /**
     * Execute an ad-hoc split payment (inline recipients, no predefined rule).
     */
    SplitPaymentExecution executeAdHocSplit(String payerAccountId, String partnerId,
                                            BigDecimal totalAmount, String currency,
                                            List<SplitRecipient> recipients,
                                            String externalReferenceId,
                                            String description,
                                            String idempotencyKey);

    /**
     * Get execution by ID.
     */
    SplitPaymentExecution getExecution(UUID executionId);

    /**
     * Get executions by payer.
     */
    List<SplitPaymentExecution> getExecutionsByPayer(String payerAccountId);

    /**
     * Reverse a completed split payment — credits payer, debits all recipients.
     */
    SplitPaymentExecution reverseExecution(UUID executionId, String reason);
}
