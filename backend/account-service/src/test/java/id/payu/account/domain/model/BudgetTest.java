package id.payu.account.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * ACCOUNT-006: exhaustive core-domain coverage for {@link Budget}.
 */
@DisplayName("Budget domain")
class BudgetTest {

    private Budget budget(UUID userId) {
        return new Budget(UUID.randomUUID(), userId, "FOOD",
                new BigDecimal("1000.0000"), BudgetPeriod.MONTHLY,
                BigDecimal.ZERO, null, true, LocalDateTime.now(), LocalDateTime.now(), 0);
    }

    @Nested
    @DisplayName("canSpend")
    class CanSpend {
        @Test
        void rejectsNullAndNonPositive() {
            Budget b = budget(UUID.randomUUID());
            assertThat(b.canSpend(null)).isFalse();
            assertThat(b.canSpend(BigDecimal.ZERO)).isFalse();
            assertThat(b.canSpend(new BigDecimal("-5"))).isFalse();
        }

        @Test
        void rejectsWhenInactive() {
            Budget b = budget(UUID.randomUUID());
            b.pause();
            assertThat(b.canSpend(BigDecimal.ONE)).isFalse();
        }

        @Test
        void rejectsOverLimit() {
            Budget b = budget(UUID.randomUUID());
            b.recordSpending(new BigDecimal("900.0000"));
            assertThat(b.canSpend(new BigDecimal("101.0000"))).isFalse();
            assertThat(b.canSpend(new BigDecimal("100.0000"))).isTrue();
        }
    }

    @Nested
    @DisplayName("recordSpending")
    class RecordSpending {
        @Test
        void accumulatesSpending() {
            Budget b = budget(UUID.randomUUID());
            b.recordSpending(new BigDecimal("100.0000"));
            b.recordSpending(new BigDecimal("50.0000"));
            assertThat(b.getCurrentSpent()).isEqualByComparingTo(new BigDecimal("150.0000"));
        }

        @Test
        void rejectsInvalidAmount() {
            Budget b = budget(UUID.randomUUID());
            assertThatThrownBy(() -> b.recordSpending(null)).isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> b.recordSpending(BigDecimal.ZERO)).isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void rejectsWhenInactive() {
            Budget b = budget(UUID.randomUUID());
            b.pause();
            assertThatThrownBy(() -> b.recordSpending(BigDecimal.ONE)).isInstanceOf(IllegalStateException.class);
        }
    }

    @Nested
    @DisplayName("resetIfNeeded")
    class ResetIfNeeded {
        @Test
        void initializesResetDateWhenNull() {
            Budget b = budget(UUID.randomUUID());
            b.resetIfNeeded();
            assertThat(b.getResetDate()).isNotNull();
        }

        @Test
        void resetsSpendingWhenPeriodElapsed() {
            Budget b = budget(UUID.randomUUID());
            b.setResetDate(LocalDate.now().minusDays(1));
            b.recordSpending(new BigDecimal("700.0000"));
            b.resetIfNeeded();
            assertThat(b.getCurrentSpent()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(b.getResetDate()).isAfter(LocalDate.now());
        }

        @Test
        void doesNotResetInsidePeriod() {
            Budget b = budget(UUID.randomUUID());
            LocalDate future = LocalDate.now().plusDays(10);
            b.setResetDate(future);
            b.recordSpending(new BigDecimal("700.0000"));
            b.resetIfNeeded();
            assertThat(b.getCurrentSpent()).isEqualByComparingTo(new BigDecimal("700.0000"));
            assertThat(b.getResetDate()).isEqualTo(future);
        }
    }

    @Nested
    @DisplayName("lifecycle and limit")
    class Lifecycle {
        @Test
        void pauseResumeToggleActive() {
            Budget b = budget(UUID.randomUUID());
            assertThat(b.isActive()).isTrue();
            b.pause();
            assertThat(b.isActive()).isFalse();
            b.resume();
            assertThat(b.isActive()).isTrue();
        }

        @Test
        void updateLimitValidatesPositive() {
            Budget b = budget(UUID.randomUUID());
            b.updateLimit(new BigDecimal("2000.0000"));
            assertThat(b.getLimitAmount()).isEqualByComparingTo(new BigDecimal("2000.0000"));
            assertThatThrownBy(() -> b.updateLimit(BigDecimal.ZERO)).isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> b.updateLimit(null)).isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("status and percentages")
    class Status {
        @Test
        void pausedWhenInactive() {
            Budget b = budget(UUID.randomUUID());
            b.pause();
            assertThat(b.getStatus()).isEqualTo(BudgetStatus.PAUSED);
        }

        @Test
        void exceededWhenAtOrOverLimit() {
            Budget b = budget(UUID.randomUUID());
            b.recordSpending(new BigDecimal("1000.0000"));
            assertThat(b.getStatus()).isEqualTo(BudgetStatus.EXCEEDED);
            assertThat(b.isExceeded()).as("isExceeded is strictly greater-than").isFalse();
            b.recordSpending(new BigDecimal("1.0000"));
            assertThat(b.isExceeded()).isTrue();
            assertThat(b.getSpentPercentage()).isGreaterThan(BigDecimal.valueOf(100));
        }

        @Test
        void nearLimitAtThreshold() {
            Budget b = budget(UUID.randomUUID());
            b.recordSpending(new BigDecimal("800.0000"));
            assertThat(b.getStatus()).isEqualTo(BudgetStatus.NEAR_LIMIT);
            assertThat(b.isNearLimit()).isTrue();
        }

        @Test
        void activeWhenBelowThreshold() {
            Budget b = budget(UUID.randomUUID());
            b.recordSpending(new BigDecimal("100.0000"));
            assertThat(b.getStatus()).isEqualTo(BudgetStatus.ACTIVE);
            assertThat(b.isNearLimit()).isFalse();
        }

        @Test
        void remainingAmountCanGoNegative() {
            Budget b = budget(UUID.randomUUID());
            b.recordSpending(new BigDecimal("1500.0000"));
            assertThat(b.getRemainingAmount()).isNegative();
        }

        @Test
        void zeroPercentageWhenNoLimit() {
            Budget b = budget(UUID.randomUUID());
            b.setLimitAmount(null);
            assertThat(b.getSpentPercentage()).isEqualByComparingTo(BigDecimal.ZERO);
        }
    }

    @Test
    void defaultConstructorInitializesDefaults() {
        Budget b = new Budget();
        assertThat(b.isActive()).isTrue();
        assertThat(b.getCurrentSpent()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void settersAndGettersRoundTrip() {
        Budget b = budget(UUID.randomUUID());
        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        LocalDate resetDate = LocalDate.now().plusDays(3);

        b.setId(id);
        b.setUserId(userId);
        b.setCategory("TRAVEL");
        b.setLimitAmount(new BigDecimal("500.0000"));
        b.setPeriod(BudgetPeriod.WEEKLY);
        b.setCurrentSpent(new BigDecimal("10.0000"));
        b.setResetDate(resetDate);
        b.setActive(true);
        b.setWarningThreshold(new BigDecimal("0.5"));
        b.setCreatedAt(now);
        b.setUpdatedAt(now);

        assertThat(b.getId()).isEqualTo(id);
        assertThat(b.getUserId()).isEqualTo(userId);
        assertThat(b.getCategory()).isEqualTo("TRAVEL");
        assertThat(b.getLimitAmount()).isEqualByComparingTo(new BigDecimal("500.0000"));
        assertThat(b.getPeriod()).isEqualTo(BudgetPeriod.WEEKLY);
        assertThat(b.getCurrentSpent()).isEqualByComparingTo(new BigDecimal("10.0000"));
        assertThat(b.getResetDate()).isEqualTo(resetDate);
        assertThat(b.isActive()).isTrue();
        assertThat(b.getWarningThreshold()).isEqualByComparingTo(new BigDecimal("0.5"));
        assertThat(b.getCreatedAt()).isEqualTo(now);
        assertThat(b.getUpdatedAt()).isEqualTo(now);
    }

    @Test
    void builderBuildsBudget() {
        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        LocalDate resetDate = LocalDate.now().plusDays(1);

        Budget b = Budget.builder()
                .id(id)
                .userId(userId)
                .category("FOOD")
                .limitAmount(new BigDecimal("1000.0000"))
                .period(BudgetPeriod.MONTHLY)
                .currentSpent(new BigDecimal("100.0000"))
                .resetDate(resetDate)
                .active(true)
                .createdAt(now)
                .updatedAt(now)
                .version(3)
                .build();

        assertThat(b.getId()).isEqualTo(id);
        assertThat(b.getUserId()).isEqualTo(userId);
        assertThat(b.getCategory()).isEqualTo("FOOD");
        assertThat(b.getLimitAmount()).isEqualByComparingTo(new BigDecimal("1000.0000"));
        assertThat(b.getPeriod()).isEqualTo(BudgetPeriod.MONTHLY);
        assertThat(b.getCurrentSpent()).isEqualByComparingTo(new BigDecimal("100.0000"));
        assertThat(b.getResetDate()).isEqualTo(resetDate);
        assertThat(b.isActive()).isTrue();
    }

    @Test
    void resetPeriodsComputeNextDates() {
        Budget daily = new Budget(UUID.randomUUID(), UUID.randomUUID(), "FOOD",
                new BigDecimal("100.0000"), BudgetPeriod.DAILY, BigDecimal.ZERO, null, true,
                LocalDateTime.now(), LocalDateTime.now(), 0);
        daily.resetIfNeeded();
        assertThat(daily.getResetDate()).isEqualTo(LocalDate.now().plusDays(1));

        Budget weekly = new Budget(UUID.randomUUID(), UUID.randomUUID(), "FOOD",
                new BigDecimal("100.0000"), BudgetPeriod.WEEKLY, BigDecimal.ZERO, null, true,
                LocalDateTime.now(), LocalDateTime.now(), 0);
        weekly.resetIfNeeded();
        assertThat(weekly.getResetDate()).isEqualTo(LocalDate.now().plusWeeks(1));
    }
}
