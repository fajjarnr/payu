package id.payu.promotion.application.service;

import id.payu.promotion.domain.*;
import id.payu.promotion.dto.*;
import id.payu.promotion.adapter.persistence.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class GamificationService {

    private static final Logger LOG = LoggerFactory.getLogger(GamificationService.class);
    private static final int[] XP_PER_LEVEL = {0, 100, 300, 600, 1000, 1500, 2100, 2800, 3700, 4800};
    private static final int[] POINTS_PER_STREAK = {0, 5, 10, 15, 25, 40, 60, 85, 115, 150, 200};

    private final DailyCheckinRepository dailyCheckinRepository;
    private final UserLevelRepository userLevelRepository;
    private final XpTransactionRepository xpTransactionRepository;
    private final BadgeRepository badgeRepository;
    private final UserBadgeRepository userBadgeRepository;
    private final LevelRewardRepository levelRewardRepository;
    private final LoyaltyPointsService loyaltyPointsService;

    public GamificationService(
            DailyCheckinRepository dailyCheckinRepository,
            UserLevelRepository userLevelRepository,
            XpTransactionRepository xpTransactionRepository,
            BadgeRepository badgeRepository,
            UserBadgeRepository userBadgeRepository,
            LevelRewardRepository levelRewardRepository,
            LoyaltyPointsService loyaltyPointsService) {
        this.dailyCheckinRepository = dailyCheckinRepository;
        this.userLevelRepository = userLevelRepository;
        this.xpTransactionRepository = xpTransactionRepository;
        this.badgeRepository = badgeRepository;
        this.userBadgeRepository = userBadgeRepository;
        this.levelRewardRepository = levelRewardRepository;
        this.loyaltyPointsService = loyaltyPointsService;
    }

    @Transactional
    public DailyCheckinResponse performDailyCheckin(String accountId) {
        LocalDate today = LocalDate.now();

        Optional<DailyCheckin> existingCheckin = dailyCheckinRepository
                .findByAccountIdAndCheckinDate(accountId, today);

        if (existingCheckin.isPresent()) {
            throw new IllegalStateException("Already checked in today");
        }

        Integer streak = calculateStreak(accountId);
        streak++;

        Integer pointsEarned = calculateStreakPoints(streak);

        DailyCheckin checkin = new DailyCheckin();
        checkin.setAccountId(accountId);
        checkin.setCheckinDate(today);
        checkin.setStreakCount(streak);
        checkin.setPointsEarned(pointsEarned);
        checkin = dailyCheckinRepository.save(checkin);

        if (pointsEarned > 0) {
            loyaltyPointsService.addPoints(new CreateLoyaltyPointsRequest(
                accountId,
                "checkin-" + checkin.getId(),
                LoyaltyPoints.TransactionType.EARNED,
                pointsEarned,
                LocalDateTime.now().plusMonths(12)
            ));
        }

        addXp(accountId, 5, XpTransaction.SourceType.CHECKIN, null);

        checkAndAwardBadges(accountId, streak, null);

        LOG.info("Daily check-in: accountId={}, streak={}, points={}",
            accountId, streak, pointsEarned);

        return toCheckinResponse(checkin);
    }

    public DailyCheckinResponse getTodayCheckin(String accountId) {
        LocalDate today = LocalDate.now();
        Optional<DailyCheckin> checkin = dailyCheckinRepository
                .findByAccountIdAndCheckinDate(accountId, today);
        return checkin.map(this::toCheckinResponse).orElse(null);
    }

    public Integer getCurrentStreak(String accountId) {
        List<DailyCheckin> checkins = dailyCheckinRepository
                .findByAccountIdOrderByCheckinDateDesc(accountId);

        if (checkins.isEmpty()) {
            return 0;
        }

        DailyCheckin lastCheckin = checkins.get(0);
        LocalDate today = LocalDate.now();
        LocalDate lastCheckinDate = lastCheckin.getCheckinDate();

        if (lastCheckinDate.equals(today)) {
            return lastCheckin.getStreakCount();
        } else if (lastCheckinDate.equals(today.minusDays(1))) {
            return lastCheckin.getStreakCount();
        }

        return 0;
    }

    public Long getTotalCheckins(String accountId) {
        return dailyCheckinRepository.findByAccountIdOrderByCheckinDateDesc(accountId)
                .stream()
                .count();
    }

    @Transactional
    public GamificationEventResponse processTransaction(ProcessTransactionRequest request) {
        String accountId = request.accountId();
        String transactionId = request.transactionId();
        BigDecimal amount = request.amount();

        List<XpTransaction> existingTx = xpTransactionRepository
                .findByAccountIdOrderByCreatedAtDesc(accountId);
        boolean alreadyProcessed = existingTx.stream()
                .anyMatch(tx -> transactionId.equals(tx.getTransactionId()));

        if (alreadyProcessed) {
            LOG.info("Transaction already processed: {}", transactionId);
            return new GamificationEventResponse(
                Collections.emptyList(),
                null,
                0,
                0
            );
        }

        UserLevel oldUserLevel = getOrCreateUserLevel(accountId);
        Integer oldLevel = oldUserLevel.getLevel();

        Integer xpEarned = calculateTransactionXp(amount);
        Integer newLevel = addXp(accountId, xpEarned, XpTransaction.SourceType.TRANSACTION, transactionId);

        UserLevelResponse levelUp = null;
        if (newLevel > oldLevel) {
            Optional<UserLevel> updatedUserLevel = userLevelRepository.findByAccountId(accountId);
            if (updatedUserLevel.isPresent()) {
                levelUp = toUserLevelResponse(updatedUserLevel.get());
            }
            grantLevelRewards(accountId, newLevel);
        }

        List<EarnedBadgeResponse> badgesEarned = checkAndAwardBadges(
            accountId, null, transactionId);

        LOG.info("Transaction processed: accountId={}, xp={}, level={}",
            accountId, xpEarned, newLevel);

        return new GamificationEventResponse(
            badgesEarned,
            levelUp,
            xpEarned,
            0
        );
    }

    @Transactional
    public UserLevelResponse getUserLevel(String accountId) {
        Optional<UserLevel> userLevel = userLevelRepository.findByAccountId(accountId);
        return userLevel.map(this::toUserLevelResponse).orElse(null);
    }

    public List<EarnedBadgeResponse> getUserBadges(String accountId) {
        List<UserBadge> userBadges = userBadgeRepository.findByAccountId(accountId);
        List<UUID> badgeIds = userBadges.stream()
            .map(UserBadge::getBadgeId)
            .collect(Collectors.toList());

        if (badgeIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<Badge> badges = badgeIds.stream()
                .map(badgeRepository::findById)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());

        Map<UUID, Badge> badgeMap = badges.stream()
                .collect(Collectors.toMap(Badge::getId, b -> b));

        return userBadges.stream()
            .map(ub -> toEarnedBadgeResponse(ub, badgeMap.get(ub.getBadgeId())))
            .collect(Collectors.toList());
    }

    public List<BadgeProgressResponse> getBadgeProgress(String accountId) {
        List<Badge> allBadges = badgeRepository.findByIsActiveTrue();
        List<UserBadge> userBadges = userBadgeRepository.findByAccountId(accountId);
        Set<UUID> earnedBadgeIds = userBadges.stream()
                .map(UserBadge::getBadgeId)
                .collect(Collectors.toSet());

        Optional<UserLevel> userLevelOpt = userLevelRepository.findByAccountId(accountId);
        List<DailyCheckin> checkins = dailyCheckinRepository.findByAccountIdOrderByCheckinDateDesc(accountId);
        List<XpTransaction> xpTransactions = xpTransactionRepository.findByAccountIdOrderByCreatedAtDesc(accountId);

        Integer currentLevel = userLevelOpt.map(UserLevel::getLevel).orElse(1);
        Integer currentXp = userLevelOpt.map(UserLevel::getXp).orElse(0);
        int totalCheckins = checkins.size();
        long transactionCount = xpTransactions.stream()
                .filter(tx -> tx.getSourceType() == XpTransaction.SourceType.TRANSACTION)
                .count();

        BigDecimal totalTransactionAmount = xpTransactions.stream()
                .map(XpTransaction::getTransactionId)
                .filter(Objects::nonNull)
                .map(this::getTransactionAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return allBadges.stream()
            .map(badge -> toBadgeProgressResponse(
                badge,
                earnedBadgeIds.contains(badge.getId()),
                currentLevel,
                currentXp,
                totalCheckins,
                (int) transactionCount,
                totalTransactionAmount))
            .sorted((a, b) -> Boolean.compare(b.isEligible(), a.isEligible()))
            .collect(Collectors.toList());
    }

    public GamificationSummaryResponse getSummary(String accountId) {
        UserLevelResponse level = getUserLevel(accountId);
        List<EarnedBadgeResponse> badges = getUserBadges(accountId);
        DailyCheckinResponse lastCheckin = getLastCheckin(accountId);
        Integer currentStreak = getCurrentStreak(accountId);
        Long totalCheckins = getTotalCheckins(accountId);

        return new GamificationSummaryResponse(
            level,
            badges,
            lastCheckin,
            currentStreak,
            totalCheckins
        );
    }

    private Integer calculateStreak(String accountId) {
        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);

        Optional<DailyCheckin> yesterdayCheckin = dailyCheckinRepository
                .findByAccountIdAndCheckinDate(accountId, yesterday);

        return yesterdayCheckin
                .map(DailyCheckin::getStreakCount)
                .orElse(0);
    }

    private Integer calculateStreakPoints(Integer streak) {
        int index = Math.min(streak, POINTS_PER_STREAK.length - 1);
        return POINTS_PER_STREAK[index];
    }

    private Integer calculateTransactionXp(BigDecimal amount) {
        int xp = amount.divide(BigDecimal.valueOf(10000), 0, RoundingMode.DOWN).intValue();
        return Math.max(xp, 1);
    }

    private UserLevel getOrCreateUserLevel(String accountId) {
        Optional<UserLevel> userLevelOpt = userLevelRepository.findByAccountId(accountId);
        if (userLevelOpt.isPresent()) {
            return userLevelOpt.get();
        }

        UserLevel userLevel = new UserLevel();
        userLevel.setAccountId(accountId);
        userLevel.setLevel(1);
        userLevel.setXp(0);
        userLevel.setLevelName("Pemula");
        return userLevelRepository.save(userLevel);
    }

    private Integer addXp(String accountId, Integer xpToAdd, XpTransaction.SourceType sourceType, String transactionId) {
        UserLevel userLevel = getOrCreateUserLevel(accountId);

        Integer currentXp = userLevel.getXp();
        Integer newXp = currentXp + xpToAdd;

        Integer currentLevel = userLevel.getLevel();
        Integer newLevel = calculateLevel(newXp);

        userLevel.setXp(newXp);
        userLevel.setLevel(newLevel);
        userLevel.setLevelName(getLevelName(newLevel));
        userLevel.setUpdatedAt(LocalDateTime.now());
        userLevelRepository.save(userLevel);

        XpTransaction xpTx = new XpTransaction();
        xpTx.setAccountId(accountId);
        xpTx.setTransactionId(transactionId);
        xpTx.setSourceType(sourceType);
        xpTx.setXpEarned(xpToAdd);
        xpTx.setXpAfter(newXp);
        xpTransactionRepository.save(xpTx);

        LOG.info("XP added: accountId={}, xp={}, level={} -> {}",
            accountId, xpToAdd, currentLevel, newLevel);

        return newLevel;
    }

    private Integer calculateLevel(Integer xp) {
        for (int level = XP_PER_LEVEL.length - 1; level >= 1; level--) {
            if (xp >= XP_PER_LEVEL[level]) {
                return level + 1;
            }
        }
        return 1;
    }

    private String getLevelName(Integer level) {
        return switch (level) {
            case 1 -> "Pemula";
            case 2 -> "Pengunjung";
            case 3 -> "Pengguna";
            case 4 -> "Pecinta";
            case 5 -> "Penggemar";
            case 6 -> "Ahli";
            case 7 -> "Master";
            case 8 -> "Grandmaster";
            case 9 -> "Champion";
            case 10 -> "Legenda";
            default -> "Pemula";
        };
    }

    private Integer getXpToNextLevel(Integer currentLevel) {
        if (currentLevel >= XP_PER_LEVEL.length - 1) {
            return 0;
        }
        return XP_PER_LEVEL[currentLevel + 1] - XP_PER_LEVEL[currentLevel];
    }

    private void grantLevelRewards(String accountId, Integer level) {
        List<LevelReward> rewards = levelRewardRepository.findByLevel(level);
        for (LevelReward reward : rewards) {
            if (reward.getPointsReward() > 0) {
                loyaltyPointsService.addPoints(new CreateLoyaltyPointsRequest(
                    accountId,
                    "level-reward-" + level,
                    LoyaltyPoints.TransactionType.EARNED,
                    reward.getPointsReward(),
                    LocalDateTime.now().plusMonths(12)
                ));
                LOG.info("Level reward granted: accountId={}, level={}, points={}",
                    accountId, level, reward.getPointsReward());
            }
        }
    }

    private List<EarnedBadgeResponse> checkAndAwardBadges(String accountId, Integer streak, String transactionId) {
        List<EarnedBadgeResponse> earnedBadges = new ArrayList<>();
        List<Badge> allBadges = badgeRepository.findByIsActiveTrue();
        List<UserBadge> userBadges = userBadgeRepository.findByAccountId(accountId);
        Set<UUID> earnedBadgeIds = userBadges.stream()
                .map(UserBadge::getBadgeId)
                .collect(Collectors.toSet());

        Optional<UserLevel> userLevelOpt = userLevelRepository.findByAccountId(accountId);
        Integer currentLevel = userLevelOpt.map(UserLevel::getLevel).orElse(1);
        List<DailyCheckin> checkins = dailyCheckinRepository.findByAccountIdOrderByCheckinDateDesc(accountId);
        List<XpTransaction> xpTransactions = xpTransactionRepository.findByAccountIdOrderByCreatedAtDesc(accountId);

        int totalCheckins = checkins.size();
        long transactionCount = xpTransactions.stream()
                .filter(tx -> tx.getSourceType() == XpTransaction.SourceType.TRANSACTION)
                .count();

        BigDecimal totalTransactionAmount = xpTransactions.stream()
                .map(XpTransaction::getTransactionId)
                .filter(Objects::nonNull)
                .map(this::getTransactionAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        for (Badge badge : allBadges) {
            if (earnedBadgeIds.contains(badge.getId())) {
                continue;
            }

            if (checkBadgeRequirement(badge, streak, currentLevel, totalCheckins,
                    (int) transactionCount, totalTransactionAmount)) {
                UserBadge userBadge = new UserBadge();
                userBadge.setAccountId(accountId);
                userBadge.setBadgeId(badge.getId());
                userBadge = userBadgeRepository.save(userBadge);

                if (badge.getPointsReward() > 0) {
                    loyaltyPointsService.addPoints(new CreateLoyaltyPointsRequest(
                        accountId,
                        "badge-reward-" + badge.getId(),
                        LoyaltyPoints.TransactionType.EARNED,
                        badge.getPointsReward(),
                        LocalDateTime.now().plusMonths(12)
                    ));
                }

                earnedBadges.add(toEarnedBadgeResponse(userBadge, badge));

                LOG.info("Badge awarded: accountId={}, badge={}", accountId, badge.getName());
            }
        }

        return earnedBadges;
    }

    private boolean checkBadgeRequirement(Badge badge, Integer streak, Integer level,
            Integer totalCheckins, Integer transactionCount, BigDecimal totalAmount) {
        BigDecimal requirement = badge.getRequirementValue();

        return switch (badge.getRequirementType()) {
            case STREAK_DAYS -> streak != null && streak >= requirement.intValue();
            case LEVEL_REACHED -> level >= requirement.intValue();
            case TRANSACTION_COUNT -> transactionCount >= requirement.intValue();
            case TOTAL_AMOUNT -> totalAmount.compareTo(requirement) >= 0;
            default -> false;
        };
    }

    private BigDecimal getTransactionAmount(String transactionId) {
        return BigDecimal.ZERO;
    }

    private DailyCheckinResponse getLastCheckin(String accountId) {
        List<DailyCheckin> checkins = dailyCheckinRepository.findByAccountIdOrderByCheckinDateDesc(accountId);
        return checkins.isEmpty() ? null : toCheckinResponse(checkins.get(0));
    }

    private DailyCheckinResponse toCheckinResponse(DailyCheckin checkin) {
        return new DailyCheckinResponse(
            checkin.getId(),
            checkin.getAccountId(),
            checkin.getCheckinDate(),
            checkin.getStreakCount(),
            checkin.getPointsEarned(),
            checkin.getCreatedAt()
        );
    }

    private UserLevelResponse toUserLevelResponse(UserLevel userLevel) {
        return new UserLevelResponse(
            userLevel.getId(),
            userLevel.getAccountId(),
            userLevel.getLevel(),
            userLevel.getLevelName(),
            userLevel.getXp(),
            getXpToNextLevel(userLevel.getLevel()),
            userLevel.getCreatedAt(),
            userLevel.getUpdatedAt()
        );
    }

    private EarnedBadgeResponse toEarnedBadgeResponse(UserBadge userBadge, Badge badge) {
        return new EarnedBadgeResponse(
            userBadge.getId(),
            badge.getId(),
            badge.getName(),
            badge.getDescription(),
            badge.getIconUrl(),
            badge.getRequirementType(),
            badge.getRequirementValue(),
            badge.getPointsReward(),
            badge.getCategory(),
            userBadge.getEarnedAt()
        );
    }

    private BadgeProgressResponse toBadgeProgressResponse(Badge badge, Boolean isEarned,
            Integer currentLevel, Integer currentXp, Integer totalCheckins,
            Integer transactionCount, BigDecimal totalAmount) {
        BigDecimal currentProgress = BigDecimal.ZERO;

        switch (badge.getRequirementType()) {
            case STREAK_DAYS -> currentProgress = BigDecimal.valueOf(totalCheckins);
            case LEVEL_REACHED -> currentProgress = BigDecimal.valueOf(currentLevel);
            case TRANSACTION_COUNT -> currentProgress = BigDecimal.valueOf(transactionCount);
            case TOTAL_AMOUNT -> currentProgress = totalAmount;
        }

        boolean isEligible = !isEarned && currentProgress.compareTo(badge.getRequirementValue()) >= 0;

        return new BadgeProgressResponse(
            badge.getId(),
            badge.getName(),
            badge.getDescription(),
            badge.getIconUrl(),
            badge.getRequirementType(),
            badge.getRequirementValue(),
            currentProgress,
            isEarned,
            isEligible,
            null
        );
    }
}
