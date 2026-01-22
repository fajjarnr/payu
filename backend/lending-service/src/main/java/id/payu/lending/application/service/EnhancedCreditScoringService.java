package id.payu.lending.application.service;

import id.payu.lending.adapter.external.AccountClient;
import id.payu.lending.adapter.external.TransactionClient;
import id.payu.lending.domain.model.CreditScore;
import id.payu.lending.dto.TransactionSummaryResponse;
import id.payu.lending.dto.UserResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class EnhancedCreditScoringService {

    private final AccountClient accountClient;
    private final TransactionClient transactionClient;

    public BigDecimal calculateEnhancedCreditScore(UUID userId, BigDecimal baseScore) {
        log.info("Calculating enhanced credit score for user: {}", userId);

        BigDecimal score = baseScore;
        BigDecimal maxScore = new BigDecimal("850");

        try {
            UserResponse user = accountClient.getUserById(userId);
            score = score.add(calculateKycScore(user));

            Period accountTenure = Period.between(
                    user.createdAt().toLocalDate(),
                    LocalDate.now()
            );
            score = score.add(calculateTenureScore(accountTenure));

            TransactionSummaryResponse summary = transactionClient.getTransactionSummary(userId);
            score = score.add(calculateTransactionScore(summary));

        } catch (Exception e) {
            log.warn("Error fetching user data for enhanced scoring, using base score: {}", e.getMessage());
        }

        return score.min(maxScore);
    }

    private BigDecimal calculateKycScore(UserResponse user) {
        String kycStatus = user.kycStatus();

        if (kycStatus == null) {
            return BigDecimal.ZERO;
        }

        return switch (kycStatus.toUpperCase()) {
            case "APPROVED" -> new BigDecimal("50");
            case "PENDING" -> new BigDecimal("25");
            default -> BigDecimal.ZERO;
        };
    }

    private BigDecimal calculateTenureScore(Period tenure) {
        int months = tenure.getYears() * 12 + tenure.getMonths();

        if (months >= 36) {
            return new BigDecimal("40");
        } else if (months >= 24) {
            return new BigDecimal("30");
        } else if (months >= 12) {
            return new BigDecimal("20");
        } else if (months >= 6) {
            return new BigDecimal("10");
        } else {
            return BigDecimal.ZERO;
        }
    }

    private BigDecimal calculateTransactionScore(TransactionSummaryResponse summary) {
        if (summary == null || summary.totalTransactions() == 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal score = BigDecimal.ZERO;
        Integer totalTransactions = summary.totalTransactions();
        BigDecimal totalAmount = summary.totalAmount();
        BigDecimal successRate = new BigDecimal(summary.successfulTransactions())
                .divide(new BigDecimal(totalTransactions), 4, RoundingMode.HALF_UP);

        if (successRate.compareTo(new BigDecimal("0.98")) >= 0) {
            score = score.add(new BigDecimal("30"));
        } else if (successRate.compareTo(new BigDecimal("0.95")) >= 0) {
            score = score.add(new BigDecimal("20"));
        } else if (successRate.compareTo(new BigDecimal("0.90")) >= 0) {
            score = score.add(new BigDecimal("10"));
        } else {
            score = score.subtract(new BigDecimal("20"));
        }

        if (totalAmount.compareTo(new BigDecimal("100000000")) > 0) {
            score = score.add(new BigDecimal("20"));
        } else if (totalAmount.compareTo(new BigDecimal("50000000")) > 0) {
            score = score.add(new BigDecimal("15"));
        } else if (totalAmount.compareTo(new BigDecimal("10000000")) > 0) {
            score = score.add(new BigDecimal("10"));
        }

        if (totalTransactions > 100) {
            score = score.add(new BigDecimal("10"));
        } else if (totalTransactions > 50) {
            score = score.add(new BigDecimal("5"));
        }

        return score;
    }
}
