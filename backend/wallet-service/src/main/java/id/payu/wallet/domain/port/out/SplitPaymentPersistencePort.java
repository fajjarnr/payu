package id.payu.wallet.domain.port.out;

import id.payu.wallet.domain.model.SplitPaymentExecution;
import id.payu.wallet.domain.model.SplitPaymentRule;
import id.payu.wallet.domain.model.SplitExecutionStatus;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SplitPaymentPersistencePort {
    SplitPaymentRule saveRule(SplitPaymentRule rule);
    Optional<SplitPaymentRule> findRuleById(UUID ruleId);
    List<SplitPaymentRule> findRulesByPartnerId(String partnerId);

    SplitPaymentExecution saveExecution(SplitPaymentExecution execution);
    Optional<SplitPaymentExecution> findExecutionById(UUID executionId);
    Optional<SplitPaymentExecution> findExecutionByIdempotencyKey(String idempotencyKey);
    List<SplitPaymentExecution> findExecutionsByPayerAccountId(String payerAccountId);
    List<SplitPaymentExecution> findExecutionsByStatusIn(Collection<SplitExecutionStatus> statuses);
}
