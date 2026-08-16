package id.payu.wallet.application.service;

import id.payu.wallet.domain.model.Pocket;
import id.payu.wallet.domain.model.SavingsGoal;
import id.payu.wallet.domain.model.SavingsGoalStatus;
import id.payu.wallet.domain.port.in.PocketUseCase;
import id.payu.wallet.domain.port.in.SavingsGoalUseCase;
import id.payu.wallet.domain.port.out.SavingsGoalPersistencePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SavingsGoalService implements SavingsGoalUseCase {

    private final SavingsGoalPersistencePort savingsGoalPersistencePort;
    private final PocketUseCase pocketUseCase;
    private final Clock clock;

    private Pocket verifyPocketOwnership(UUID pocketId, String authenticatedAccountId) {
        Pocket pocket = pocketUseCase.getPocketById(pocketId)
                .orElseThrow(() -> new PocketNotFoundException("Wallet not found"));

        if (!Objects.equals(pocket.getAccountId(), authenticatedAccountId)) {
            throw new SavingsGoalForbiddenException("Not authorized to access savings goals for this wallet");
        }
        return pocket;
    }

    /**
     * SAVINGS-UUID-001: the wallet accountId is a PayU string identifier
     * (e.g. ACC-12345678, sender-...) which is not a valid UUID. The
     * savings_goals.user_id column is UUID NOT NULL, so map non-UUID account
     * ids to a deterministic UUID (stable per account, no crash).
     */
    private UUID resolveUserId(String accountId) {
        try {
            return UUID.fromString(accountId);
        } catch (IllegalArgumentException e) {
            return UUID.nameUUIDFromBytes(accountId.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        }
    }

    private SavingsGoal verifyGoalExists(UUID goalId, UUID pocketId) {        SavingsGoal goal = savingsGoalPersistencePort.findById(goalId)
                .orElseThrow(() -> new SavingsGoalNotFoundException("Savings goal not found"));

        if (!Objects.equals(goal.getPocketId(), pocketId)) {
            throw new SavingsGoalNotFoundException("Savings goal not found");
        }
        return goal;
    }

    @Override
    @Transactional(readOnly = true)
    public List<SavingsGoal> getSavingsGoals(UUID walletId, String authenticatedAccountId) {
        log.info("Getting savings goals for wallet: {}", walletId);
        verifyPocketOwnership(walletId, authenticatedAccountId);

        return savingsGoalPersistencePort.findByPocketIdAndStatusNot(walletId, SavingsGoalStatus.CANCELLED);
    }

    @Override
    @Transactional
    public SavingsGoal createSavingsGoal(UUID walletId, String authenticatedAccountId, String name, String description,
                                         BigDecimal targetAmount, LocalDate deadline, String icon, String color) {
        log.info("Creating savings goal for wallet: {}", walletId);
        Pocket pocket = verifyPocketOwnership(walletId, authenticatedAccountId);

        SavingsGoal goal = SavingsGoal.builder()
                .id(UUID.randomUUID())
                .pocketId(walletId)
                .userId(resolveUserId(pocket.getAccountId()))
                .name(name)
                .description(description)
                .targetAmount(targetAmount)
                .currentAmount(BigDecimal.ZERO)
                .currency(pocket.getCurrency())
                .deadline(deadline)
                .status(SavingsGoalStatus.ACTIVE)
                .icon(icon)
                .color(color)
                .createdAt(LocalDateTime.now(clock))
                .updatedAt(LocalDateTime.now(clock))
                .build();

        return savingsGoalPersistencePort.save(goal);
    }

    @Override
    @Transactional
    public SavingsGoal updateSavingsGoal(UUID walletId, UUID goalId, String authenticatedAccountId, String name, String description,
                                         BigDecimal targetAmount, LocalDate deadline, String icon, String color) {
        log.info("Updating savings goal: {} for wallet: {}", goalId, walletId);
        verifyPocketOwnership(walletId, authenticatedAccountId);
        SavingsGoal goal = verifyGoalExists(goalId, walletId);

        goal.setName(name);
        goal.setDescription(description);
        goal.setTargetAmount(targetAmount);
        goal.setDeadline(deadline);
        goal.setIcon(icon);
        goal.setColor(color);
        goal.setUpdatedAt(LocalDateTime.now(clock));

        return savingsGoalPersistencePort.save(goal);
    }

    @Override
    @Transactional
    public void deleteSavingsGoal(UUID walletId, UUID goalId, String authenticatedAccountId) {
        log.info("Deleting savings goal: {} for wallet: {}", goalId, walletId);
        verifyPocketOwnership(walletId, authenticatedAccountId);
        SavingsGoal goal = verifyGoalExists(goalId, walletId);

        goal.setStatus(SavingsGoalStatus.CANCELLED);
        goal.setUpdatedAt(LocalDateTime.now(clock));
        savingsGoalPersistencePort.save(goal);
    }

    @Override
    @Transactional
    public SavingsGoal pauseSavingsGoal(UUID walletId, UUID goalId, String authenticatedAccountId) {
        log.info("Pausing savings goal: {} for wallet: {}", goalId, walletId);
        verifyPocketOwnership(walletId, authenticatedAccountId);
        SavingsGoal goal = verifyGoalExists(goalId, walletId);

        if (goal.getStatus() == SavingsGoalStatus.ACTIVE) {
            goal.setStatus(SavingsGoalStatus.PAUSED);
            goal.setUpdatedAt(LocalDateTime.now(clock));
            return savingsGoalPersistencePort.save(goal);
        }
        return goal;
    }

    @Override
    @Transactional
    public SavingsGoal resumeSavingsGoal(UUID walletId, UUID goalId, String authenticatedAccountId) {
        log.info("Resuming savings goal: {} for wallet: {}", goalId, walletId);
        verifyPocketOwnership(walletId, authenticatedAccountId);
        SavingsGoal goal = verifyGoalExists(goalId, walletId);

        if (goal.getStatus() == SavingsGoalStatus.PAUSED) {
            goal.setStatus(SavingsGoalStatus.ACTIVE);
            goal.setUpdatedAt(LocalDateTime.now(clock));
            return savingsGoalPersistencePort.save(goal);
        }
        return goal;
    }
}
