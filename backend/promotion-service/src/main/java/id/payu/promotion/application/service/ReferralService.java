package id.payu.promotion.application.service;

import id.payu.promotion.adapter.persistence.entity.ReferralEntity;
import id.payu.promotion.adapter.persistence.entity.RewardEntity;
import id.payu.promotion.adapter.persistence.entity.LoyaltyPointsEntity;
import id.payu.promotion.dto.CreateReferralRequest;
import id.payu.promotion.dto.CompleteReferralRequest;
import id.payu.promotion.dto.ReferralSummaryResponse;
import id.payu.promotion.adapter.persistence.repository.ReferralRepository;
import id.payu.promotion.adapter.persistence.repository.RewardRepository;
import id.payu.promotion.adapter.persistence.repository.LoyaltyPointsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import id.payu.outbox.service.OutboxService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import id.payu.promotion.domain.ReferralRewardType;
import id.payu.promotion.domain.ReferralStatus;
import id.payu.promotion.domain.RewardStatus;
import id.payu.promotion.domain.RewardType;
import id.payu.promotion.domain.TransactionType;

@Service
public class ReferralService {

    private static final Logger LOG = LoggerFactory.getLogger(ReferralService.class);

    private final ReferralRepository referralRepository;
    private final RewardRepository rewardRepository;
    private final LoyaltyPointsRepository loyaltyPointsRepository;
    private final OutboxService outboxService;
    private final String promotionEventsTopic;

    public ReferralService(
            ReferralRepository referralRepository,
            RewardRepository rewardRepository,
            LoyaltyPointsRepository loyaltyPointsRepository,
            OutboxService outboxService,
            @Value("${app.kafka.topics.promotion-events:payu.promotion.referral-event.v1}") String promotionEventsTopic) {
        this.referralRepository = referralRepository;
        this.rewardRepository = rewardRepository;
        this.loyaltyPointsRepository = loyaltyPointsRepository;
        this.outboxService = outboxService;
        this.promotionEventsTopic = promotionEventsTopic;
    }

    @Transactional
    public ReferralEntity createReferral(CreateReferralRequest request) {
        LOG.info("Creating referral: referrer={}", request.referrerAccountId());

        if (request.referrerAccountId() == null || request.referrerAccountId().isBlank()) {
            throw new IllegalArgumentException("Referrer account ID is required");
        }

        String referralCode = generateReferralCode();

        ReferralEntity referral = new ReferralEntity();
        referral.setReferrerAccountId(request.referrerAccountId());
        referral.setReferralCode(referralCode);
        referral.setReferrerReward(request.referrerReward());
        referral.setRefereeReward(request.refereeReward());
        referral.setRewardType(request.rewardType());
        referral.setExpiryDate(request.expiryDate());
        referral.setStatus(ReferralStatus.PENDING);

        referral = referralRepository.save(referral);

        publishReferralEvent(referral, "CREATED");

        LOG.info("ReferralEntity created: id={}, code={}", referral.getId(), referralCode);

        return referral;
    }

    @Transactional
    public ReferralEntity completeReferral(CompleteReferralRequest request) {
        ReferralEntity referral = referralRepository.findByReferralCode(request.referralCode())
                .orElseThrow(() -> new IllegalArgumentException("Invalid referral code"));

        if (referral.getStatus() != ReferralStatus.PENDING) {
            throw new IllegalArgumentException("ReferralEntity already completed or expired");
        }

        if (referral.getExpiryDate() != null && LocalDateTime.now().isAfter(referral.getExpiryDate())) {
            referral.setStatus(ReferralStatus.EXPIRED);
            referralRepository.save(referral);
            throw new IllegalArgumentException("ReferralEntity code has expired");
        }

        referral.setRefereeAccountId(request.refereeAccountId());
        referral.setStatus(ReferralStatus.COMPLETED);
        referral.setCompletedAt(LocalDateTime.now());
        referral = referralRepository.save(referral);

        grantReferralRewards(referral);

        publishReferralEvent(referral, "COMPLETED");

        LOG.info("ReferralEntity completed: code={}, referrer={}, referee={}",
            request.referralCode(), referral.getReferrerAccountId(), request.refereeAccountId());

        return referral;
    }

    public Optional<ReferralEntity> getReferral(UUID id) {
        return referralRepository.findById(id);
    }

    public Optional<ReferralEntity> getReferralByCode(String code) {
        return referralRepository.findByReferralCode(code);
    }

    public List<ReferralEntity> getReferralsByReferrer(String referrerAccountId) {
        return referralRepository.findByReferrerAccountId(referrerAccountId);
    }

    public ReferralSummaryResponse getReferralSummary(String referrerAccountId) {
        List<ReferralEntity> referrals = referralRepository.findByReferrerAccountId(referrerAccountId);
        long totalReferrals = referrals.size();
        long completedReferrals = referrals.stream()
            .filter(r -> r.getStatus() == ReferralStatus.COMPLETED)
            .count();
        long pendingReferrals = referrals.stream()
            .filter(r -> r.getStatus() == ReferralStatus.PENDING)
            .count();

        Optional<ReferralEntity> lastReferral = referrals.stream()
            .sorted(java.util.Comparator.comparing(ReferralEntity::getCreatedAt).reversed())
            .findFirst();

        String referralCode = lastReferral
            .map(ReferralEntity::getReferralCode)
            .orElse(null);

        return new ReferralSummaryResponse(
            referralCode,
            (int) totalReferrals,
            (int) completedReferrals,
            (int) pendingReferrals
        );
    }

    private void grantReferralRewards(ReferralEntity referral) {
        if (referral.getRewardType() == ReferralRewardType.CASHBACK) {
            grantCashbackReward(referral.getReferrerAccountId(), referral.getReferrerReward(),
                referral.getReferralCode(), "REFERRER");
            grantCashbackReward(referral.getRefereeAccountId(), referral.getRefereeReward(),
                referral.getReferralCode(), "REFEREE");
        } else if (referral.getRewardType() == ReferralRewardType.POINTS) {
            grantLoyaltyPoints(referral.getReferrerAccountId(), referral.getReferrerReward().intValue(),
                referral.getReferralCode(), TransactionType.REFERRAL_BONUS);
            grantLoyaltyPoints(referral.getRefereeAccountId(), referral.getRefereeReward().intValue(),
                referral.getReferralCode(), TransactionType.REFERRAL_BONUS);
        }
    }

    private void grantCashbackReward(String accountId, BigDecimal amount,
        String transactionId, String rewardType) {
        RewardEntity reward = new RewardEntity();
        reward.setAccountId(accountId);
        reward.setTransactionId(transactionId);
        reward.setType(RewardType.REFERRAL_BONUS);
        reward.setAmount(amount);
        reward.setTransactionAmount(BigDecimal.ZERO);
        reward.setStatus(RewardStatus.AWARDED);
        rewardRepository.save(reward);
    }

    private void grantLoyaltyPoints(String accountId, Integer points,
        String transactionId, TransactionType type) {
        Integer currentBalance = 0;

        LoyaltyPointsEntity loyaltyPoints = new LoyaltyPointsEntity();
        loyaltyPoints.setAccountId(accountId);
        loyaltyPoints.setTransactionId(transactionId);
        loyaltyPoints.setTransactionType(type);
        loyaltyPoints.setPoints(points);
        loyaltyPoints.setBalanceAfter(currentBalance + points);
        loyaltyPointsRepository.save(loyaltyPoints);
    }

    private static final java.security.SecureRandom SECURE_RANDOM = new java.security.SecureRandom();

    private String generateReferralCode() {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < 8; i++) {
            code.append(chars.charAt(SECURE_RANDOM.nextInt(chars.length())));
        }
        return code.toString();
    }

    private void publishReferralEvent(ReferralEntity referral, String eventType) {
        outboxService.createEvent(
                "Referral",
                referral.getId().toString(),
                eventType,
                Map.of(
                        "referralId", referral.getId().toString(),
                        "referralCode", referral.getReferralCode(),
                        "status", referral.getStatus().name(),
                        "eventType", eventType,
                        "timestamp", LocalDateTime.now().toString()
                ),
                null,
                promotionEventsTopic
        );
    }
}
