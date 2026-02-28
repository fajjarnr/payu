package id.payu.wallet.domain.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class SavingsGoalTest {

    @Test
    void shouldCalculateProgressPercentage() {
        SavingsGoal goal = SavingsGoal.builder()
                .id(UUID.randomUUID())
                .targetAmount(new BigDecimal("1000000"))
                .currentAmount(new BigDecimal("250000"))
                .build();

        BigDecimal progress = goal.calculateProgressPercentage();

        assertEquals(new BigDecimal("25.00"), progress);
    }

    @Test
    void shouldReturnZeroProgressWhenTargetIsZero() {
        SavingsGoal goal = SavingsGoal.builder()
                .targetAmount(BigDecimal.ZERO)
                .currentAmount(new BigDecimal("100000"))
                .build();

        BigDecimal progress = goal.calculateProgressPercentage();

        assertEquals(BigDecimal.ZERO, progress);
    }

    @Test
    void shouldReturnZeroProgressWhenTargetIsNull() {
        SavingsGoal goal = SavingsGoal.builder()
                .targetAmount(null)
                .currentAmount(new BigDecimal("100000"))
                .build();

        BigDecimal progress = goal.calculateProgressPercentage();

        assertEquals(BigDecimal.ZERO, progress);
    }

    @Test
    void shouldCompleteGoalWhenTargetReached() {
        SavingsGoal goal = SavingsGoal.builder()
                .id(UUID.randomUUID())
                .targetAmount(new BigDecimal("1000000"))
                .currentAmount(new BigDecimal("500000"))
                .status(SavingsGoal.SavingsGoalStatus.ACTIVE)
                .build();

        goal.updateCurrentAmount(new BigDecimal("1000000"));

        assertEquals(SavingsGoal.SavingsGoalStatus.COMPLETED, goal.getStatus());
        assertNotNull(goal.getCompletedAt());
    }

    @Test
    void shouldCompleteGoalWhenTargetExceeded() {
        SavingsGoal goal = SavingsGoal.builder()
                .id(UUID.randomUUID())
                .targetAmount(new BigDecimal("1000000"))
                .currentAmount(new BigDecimal("500000"))
                .status(SavingsGoal.SavingsGoalStatus.ACTIVE)
                .build();

        goal.updateCurrentAmount(new BigDecimal("1500000"));

        assertEquals(SavingsGoal.SavingsGoalStatus.COMPLETED, goal.getStatus());
    }

    @Test
    void shouldNotAutoCompleteIfNotActive() {
        SavingsGoal goal = SavingsGoal.builder()
                .id(UUID.randomUUID())
                .targetAmount(new BigDecimal("1000000"))
                .currentAmount(new BigDecimal("500000"))
                .status(SavingsGoal.SavingsGoalStatus.PAUSED)
                .build();

        goal.updateCurrentAmount(new BigDecimal("1000000"));

        assertEquals(SavingsGoal.SavingsGoalStatus.PAUSED, goal.getStatus());
    }

    @Test
    void shouldPauseActiveGoal() {
        SavingsGoal goal = SavingsGoal.builder()
                .id(UUID.randomUUID())
                .status(SavingsGoal.SavingsGoalStatus.ACTIVE)
                .build();

        goal.pause();

        assertEquals(SavingsGoal.SavingsGoalStatus.PAUSED, goal.getStatus());
    }

    @Test
    void shouldResumePausedGoal() {
        SavingsGoal goal = SavingsGoal.builder()
                .id(UUID.randomUUID())
                .status(SavingsGoal.SavingsGoalStatus.PAUSED)
                .build();

        goal.resume();

        assertEquals(SavingsGoal.SavingsGoalStatus.ACTIVE, goal.getStatus());
    }

    @Test
    void shouldCancelActiveGoal() {
        SavingsGoal goal = SavingsGoal.builder()
                .id(UUID.randomUUID())
                .status(SavingsGoal.SavingsGoalStatus.ACTIVE)
                .build();

        goal.cancel();

        assertEquals(SavingsGoal.SavingsGoalStatus.CANCELLED, goal.getStatus());
    }

    @Test
    void shouldNotCancelCompletedGoal() {
        SavingsGoal goal = SavingsGoal.builder()
                .id(UUID.randomUUID())
                .status(SavingsGoal.SavingsGoalStatus.COMPLETED)
                .build();

        goal.cancel();

        assertEquals(SavingsGoal.SavingsGoalStatus.COMPLETED, goal.getStatus());
    }
}
