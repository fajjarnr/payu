package id.payu.promotion.service;

import id.payu.promotion.domain.*;
import id.payu.promotion.dto.*;
import io.smallrye.reactive.messaging.kafka.KafkaRecord;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.jboss.logging.Logger;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@ApplicationScoped
public class GamificationService {

    private static final Logger LOG = Logger.getLogger(GamificationService.class);
    private static final int[] XP_PER_LEVEL = {0, 100, 300, 600, 1000, 1500, 2100, 2800, 3700, 4800};
    private static final int[] POINTS_PER_STREAK = {0, 5, 10, 15, 25, 40, 60, 85, 115, 150, 200};

    @Inject
    EntityManager entityManager;

    @Inject
    LoyaltyPointsService loyaltyPointsService;

    @Transactional
    public DailyCheckinResponse performDailyCheckin(String accountId) {
        LocalDate today = LocalDate.now();

        DailyCheckin existingCheckin = DailyCheckin.<DailyCheckin>find(
            "accountId = ?1 and checkinDate = ?2", accountId, today)
            .firstResult();

        if (existingCheckin != null) {
            throw new IllegalStateException("Already checked in today");
        }

        Integer streak = calculateStreak(accountId);
        streak++;

        Integer pointsEarned = calculateStreakPoints(streak);

        DailyCheckin checkin = new DailyCheckin();
        checkin.accountId = accountId;
        checkin.checkinDate = today;
        checkin.streakCount = streak;
        checkin.pointsEarned = pointsEarned;
        checkin.persist();

        if (pointsEarned > 0) {
            loyaltyPointsService.addPoints(new CreateLoyaltyPointsRequest(
                accountId,
                "checkin-" + checkin.id,
                LoyaltyPoints.TransactionType.EARNED,
                pointsEarned,
                LocalDateTime.now().plusMonths(12)
            ));
        }

        addXp(accountId, 5, XpTransaction.SourceType.CHECKIN, null);

        checkAndAwardBadges(accountId, streak, null);

        LOG.infof("Daily check-in: accountId=%s, streak=%s, points=%s", 
            accountId, streak, pointsEarned);

        return toCheckinResponse(checkin);
    }

    public DailyCheckinResponse getTodayCheckin(String accountId) {
        LocalDate today = LocalDate.now();
        DailyCheckin checkin = DailyCheckin.<DailyCheckin>find(
            "accountId = ?1 and checkinDate = ?2", accountId, today)
            .firstResult();
        return checkin != null ? toCheckinResponse(checkin) : null;
    }

    public Integer getCurrentStreak(String accountId) {
        DailyCheckin lastCheckin = DailyCheckin.<DailyCheckin>find(
            "accountId = ?1 order by checkinDate desc", accountId)
            .firstResult();

        if (lastCheckin == null) {
            return 0;
        }

        LocalDate today = LocalDate.now();
        LocalDate lastCheckinDate = lastCheckin.checkinDate;

        if (lastCheckinDate.equals(today)) {
            return lastCheckin.streakCount;
        } else if (lastCheckinDate.equals(today.minusDays(1))) {
            return lastCheckin.streakCount;
        }

        return 0;
    }

    public Long getTotalCheckins(String accountId) {
        return DailyCheckin.count("accountId", accountId);
    }

    @Transactional
    public GamificationEventResponse processTransaction(ProcessTransactionRequest request) {
        String accountId = request.accountId();
        String transactionId = request.transactionId();
        BigDecimal amount = request.amount();

        XpTransaction xpTx = XpTransaction.<XpTransaction>find(
            "transactionId = ?1", transactionId)
            .firstResult();

        if (xpTx != null) {
            LOG.infof("Transaction already processed: %s", transactionId);
            return new GamificationEventResponse(
                Collections.emptyList(),
                null,
                0,
                0
            );
        }

        UserLevel oldUserLevel = getOrCreateUserLevel(accountId);
        Integer oldLevel = oldUserLevel.level;

        Integer xpEarned = calculateTransactionXp(amount);
        Integer newLevel = addXp(accountId, xpEarned, XpTransaction.SourceType.TRANSACTION, transactionId);

        UserLevelResponse levelUp = null;
        if (newLevel > oldLevel) {
            UserLevel updatedUserLevel = UserLevel.<UserLevel>find("accountId", accountId).firstResult();
            if (updatedUserLevel != null) {
                levelUp = toUserLevelResponse(updatedUserLevel);
            }
            grantLevelRewards(accountId, newLevel);
        }

        List<EarnedBadgeResponse> badgesEarned = checkAndAwardBadges(
            accountId, null, transactionId);

        LOG.infof("Transaction processed: accountId=%s, xp=%s, level=%s",
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
        UserLevel userLevel = UserLevel.<UserLevel>find("accountId", accountId).firstResult();
        if (userLevel == null) {
            return null;
        }
        return toUserLevelResponse(userLevel);
    }

    public List<EarnedBadgeResponse> getUserBadges(String accountId) {
        List<UserBadge> userBadges = UserBadge.<UserBadge>find("accountId", accountId).list();
        List<UUID> badgeIds = userBadges.stream()
            .map(ub -> ub.badgeId)
            .collect(Collectors.toList());

        if (badgeIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<Badge> badges = Badge.<Badge>list("id in ?1", badgeIds);
        Map<UUID, Badge> badgeMap = badges.stream()
            .collect(Collectors.toMap(b -> b.id, b -> b));

        return userBadges.stream()
            .map(ub -> toEarnedBadgeResponse(ub, badgeMap.get(ub.badgeId)))
            .collect(Collectors.toList());
    }

    public List<BadgeProgressResponse> getBadgeProgress(String accountId) {
        List<Badge> allBadges = Badge.<Badge>list("isActive", true);
        List<UserBadge> userBadges = UserBadge.<UserBadge>find("accountId", accountId).list();
        Set<UUID> earnedBadgeIds = userBadges.stream()
            .map(ub -> ub.badgeId)
            .collect(Collectors.toSet());

        UserLevel userLevel = UserLevel.<UserLevel>find("accountId", accountId).firstResult();
        Long totalCheckins = DailyCheckin.count("accountId", accountId);

        Integer currentLevel = userLevel != null ? userLevel.level : 1;
        Integer currentXp = userLevel != null ? userLevel.xp : 0;
        Long transactionCount = XpTransaction.count(
            "accountId = ?1 and sourceType = ?2", accountId, XpTransaction.SourceType.TRANSACTION);

        BigDecimal totalTransactionAmount = XpTransaction.<XpTransaction>find(
            "accountId = ?1 and transactionId is not null", accountId)
            .stream()
            .map(tx -> getTransactionAmount(tx.transactionId))
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        return allBadges.stream()
            .map(badge -> toBadgeProgressResponse(
                badge,
                earnedBadgeIds.contains(badge.id),
                currentLevel,
                currentXp,
                totalCheckins.intValue(),
                transactionCount.intValue(),
                totalTransactionAmount))
            .sorted((a, b) -> Boolean.compare(a.isEligible(), b.isEligible()))
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

        DailyCheckin yesterdayCheckin = DailyCheckin.<DailyCheckin>find(
            "accountId = ?1 and checkinDate = ?2", accountId, yesterday)
            .firstResult();

        if (yesterdayCheckin == null) {
            return 0;
        }

        return yesterdayCheckin.streakCount;
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
        UserLevel userLevel = UserLevel.<UserLevel>find("accountId", accountId).firstResult();
        if (userLevel == null) {
            userLevel = new UserLevel();
            userLevel.accountId = accountId;
            userLevel.level = 1;
            userLevel.xp = 0;
            userLevel.levelName = "Pemula";
            userLevel.persist();
        }
        return userLevel;
    }

    private Integer addXp(String accountId, Integer xpToAdd, XpTransaction.SourceType sourceType, String transactionId) {
        UserLevel userLevel = getOrCreateUserLevel(accountId);

        Integer currentXp = userLevel.xp;
        Integer newXp = currentXp + xpToAdd;

        Integer currentLevel = userLevel.level;
        Integer newLevel = calculateLevel(newXp);

        userLevel.xp = newXp;
        userLevel.level = newLevel;
        userLevel.levelName = getLevelName(newLevel);
        userLevel.updatedAt = LocalDateTime.now();
        userLevel.persist();

        XpTransaction xpTx = new XpTransaction();
        xpTx.accountId = accountId;
        xpTx.transactionId = transactionId;
        xpTx.sourceType = sourceType;
        xpTx.xpEarned = xpToAdd;
        xpTx.xpAfter = newXp;
        xpTx.persist();

        entityManager.flush();

        LOG.infof("XP added: accountId=%s, xp=%s, level=%s -> %s",
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
        LevelReward reward = LevelReward.<LevelReward>find("level", level).firstResult();
        if (reward != null && reward.pointsReward > 0) {
            loyaltyPointsService.addPoints(new CreateLoyaltyPointsRequest(
                accountId,
                "level-reward-" + level,
                LoyaltyPoints.TransactionType.EARNED,
                reward.pointsReward,
                LocalDateTime.now().plusMonths(12)
            ));
            LOG.infof("Level reward granted: accountId=%s, level=%s, points=%s", 
                accountId, level, reward.pointsReward);
        }
    }

    private List<EarnedBadgeResponse> checkAndAwardBadges(String accountId, Integer streak, String transactionId) {
        List<EarnedBadgeResponse> earnedBadges = new ArrayList<>();
        List<Badge> allBadges = Badge.<Badge>list("isActive", true);
        List<UserBadge> userBadges = UserBadge.<UserBadge>find("accountId", accountId).list();
        Set<UUID> earnedBadgeIds = userBadges.stream()
            .map(ub -> ub.badgeId)
            .collect(Collectors.toSet());

        UserLevel userLevel = UserLevel.<UserLevel>find("accountId", accountId).firstResult();
        Integer currentLevel = userLevel != null ? userLevel.level : 1;
        Long totalCheckins = DailyCheckin.count("accountId", accountId);
        Long transactionCount = XpTransaction.count(
            "accountId = ?1 and sourceType = ?2", accountId, XpTransaction.SourceType.TRANSACTION);
        BigDecimal totalTransactionAmount = XpTransaction.<XpTransaction>find(
            "accountId = ?1 and transactionId is not null", accountId)
            .stream()
            .map(tx -> getTransactionAmount(tx.transactionId))
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        for (Badge badge : allBadges) {
            if (earnedBadgeIds.contains(badge.id)) {
                continue;
            }

            if (checkBadgeRequirement(badge, streak, currentLevel, totalCheckins.intValue(), 
                    transactionCount.intValue(), totalTransactionAmount)) {
                UserBadge userBadge = new UserBadge();
                userBadge.accountId = accountId;
                userBadge.badgeId = badge.id;
                userBadge.persist();

                if (badge.pointsReward > 0) {
                    loyaltyPointsService.addPoints(new CreateLoyaltyPointsRequest(
                        accountId,
                        "badge-reward-" + badge.id,
                        LoyaltyPoints.TransactionType.EARNED,
                        badge.pointsReward,
                        LocalDateTime.now().plusMonths(12)
                    ));
                }

                earnedBadges.add(toEarnedBadgeResponse(userBadge, badge));

                LOG.infof("Badge awarded: accountId=%s, badge=%s", accountId, badge.name);
            }
        }

        return earnedBadges;
    }

    private boolean checkBadgeRequirement(Badge badge, Integer streak, Integer level, 
            Integer totalCheckins, Integer transactionCount, BigDecimal totalAmount) {
        BigDecimal requirement = badge.requirementValue;

        return switch (badge.requirementType) {
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
        DailyCheckin checkin = DailyCheckin.<DailyCheckin>find(
            "accountId = ?1 order by checkinDate desc", accountId)
            .firstResult();
        return checkin != null ? toCheckinResponse(checkin) : null;
    }

    private DailyCheckinResponse toCheckinResponse(DailyCheckin checkin) {
        return new DailyCheckinResponse(
            checkin.id,
            checkin.accountId,
            checkin.checkinDate,
            checkin.streakCount,
            checkin.pointsEarned,
            checkin.createdAt
        );
    }

    private UserLevelResponse toUserLevelResponse(UserLevel userLevel) {
        return new UserLevelResponse(
            userLevel.id,
            userLevel.accountId,
            userLevel.level,
            userLevel.levelName,
            userLevel.xp,
            getXpToNextLevel(userLevel.level),
            userLevel.createdAt,
            userLevel.updatedAt
        );
    }

    private EarnedBadgeResponse toEarnedBadgeResponse(UserBadge userBadge, Badge badge) {
        return new EarnedBadgeResponse(
            userBadge.id,
            badge.id,
            badge.name,
            badge.description,
            badge.iconUrl,
            badge.requirementType,
            badge.requirementValue,
            badge.pointsReward,
            badge.category,
            userBadge.earnedAt
        );
    }

    private BadgeProgressResponse toBadgeProgressResponse(Badge badge, Boolean isEarned, 
            Integer currentLevel, Integer currentXp, Integer totalCheckins, 
            Integer transactionCount, BigDecimal totalAmount) {
        BigDecimal currentProgress = BigDecimal.ZERO;

        switch (badge.requirementType) {
            case STREAK_DAYS -> currentProgress = BigDecimal.valueOf(totalCheckins);
            case LEVEL_REACHED -> currentProgress = BigDecimal.valueOf(currentLevel);
            case TRANSACTION_COUNT -> currentProgress = BigDecimal.valueOf(transactionCount);
            case TOTAL_AMOUNT -> currentProgress = totalAmount;
        }

        boolean isEligible = !isEarned && currentProgress.compareTo(badge.requirementValue) >= 0;

        return new BadgeProgressResponse(
            badge.id,
            badge.name,
            badge.description,
            badge.iconUrl,
            badge.requirementType,
            badge.requirementValue,
            currentProgress,
            isEarned,
            isEligible,
            null
        );
    }
}
