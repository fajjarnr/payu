package id.payu.lending.application.service;

import id.payu.lending.adapter.external.AccountClient;
import id.payu.lending.adapter.external.TransactionClient;
import id.payu.lending.domain.model.CreditScoringFact;
import id.payu.lending.dto.TransactionSummaryResponse;
import id.payu.lending.dto.UserResponse;
import id.payu.rules.service.RulesEngineService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Period;
import java.util.UUID;

@Service
public class EnhancedCreditScoringService {

    private static final Logger log = LoggerFactory.getLogger(EnhancedCreditScoringService.class);

    private final AccountClient accountClient;
    private final TransactionClient transactionClient;
    private final RulesEngineService rulesEngineService;

    public EnhancedCreditScoringService(AccountClient accountClient, TransactionClient transactionClient, RulesEngineService rulesEngineService) {
        this.accountClient = accountClient;
        this.transactionClient = transactionClient;
        this.rulesEngineService = rulesEngineService;
    }

    public BigDecimal calculateEnhancedCreditScore(UUID userId, BigDecimal baseScore) {
        log.info("Calculating enhanced credit score for user: {}", userId);

        BigDecimal maxScore = new BigDecimal("850");
        CreditScoringFact fact = new CreditScoringFact();
        fact.setScore(baseScore);

        try {
            UserResponse user = accountClient.getUserById(userId);
            fact.setKycStatus(user.kycStatus());

            Period accountTenure = Period.between(
                    user.createdAt().toLocalDate(),
                    LocalDate.now()
            );
            int months = accountTenure.getYears() * 12 + accountTenure.getMonths();
            fact.setTenureMonths(months);

            TransactionSummaryResponse summary = transactionClient.getTransactionSummary(userId);
            if (summary != null) {
                fact.setTotalTransactions(summary.totalTransactions());
                fact.setTotalAmount(summary.totalAmount());
                
                if (summary.totalTransactions() > 0) {
                    BigDecimal successRate = new BigDecimal(summary.successfulTransactions())
                            .divide(new BigDecimal(summary.totalTransactions()), 4, RoundingMode.HALF_EVEN);
                    fact.setSuccessRate(successRate);
                } else {
                    fact.setSuccessRate(BigDecimal.ZERO);
                }
            }

            // Fire Drools rules
            rulesEngineService.fireRules(fact);

        } catch (Exception e) {
            log.warn("Error fetching user data for enhanced scoring, using base score: {}", e.getMessage());
        }

        return fact.getScore().min(maxScore);
    }
}

