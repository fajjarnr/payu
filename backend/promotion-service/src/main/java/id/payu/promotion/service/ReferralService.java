package id.payu.promotion.service;

import id.payu.promotion.domain.Referral;
import id.payu.promotion.domain.Reward;
import id.payu.promotion.domain.LoyaltyPoints;
import id.payu.promotion.dto.CreateReferralRequest;
import id.payu.promotion.dto.CompleteReferralRequest;
import id.payu.promotion.dto.ReferralSummaryResponse;
import id.payu.promotion.repository.ReferralRepository;
import id.payu.promotion.repository.RewardRepository;
import id.payu.promotion.repository.LoyaltyPointsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class ReferralService {

    private static final Logger LOG = LoggerFactory.getLogger(ReferralService.class);

    private final ReferralRepository referralRepository;
    private final RewardRepository rewardRepository;
    private final LoyaltyPointsRepository loyaltyPointsRepository;
    private final KafkaTemplate<String, Map<String, Object>> kafkaTemplate;
    private final String promotionEventsTopic;

    public ReferralService(
            ReferralRepository referralRepository,
            RewardRepository rewardRepository,
            LoyaltyPointsRepository loyaltyPointsRepository,
            KafkaTemplate<String, Map<String, Object>> kafkaTemplate,
            @Value("${app.kafka.topics.promotion-events:promotion-events}") String promotionEventsTopic) {
        this.referralRepository = referralRepository;
        this.rewardRepository = rewardRepository;
        this.loyaltyPointsRepository = loyaltyPointsRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.promotionEventsTopic = promotionEventsTopic;
    }

    @Transactional
    public Referral createReferral(CreateReferralRequest request) {
        LOG.info("Creating referral: referrer={}", request.referrerAccountId());

        String referralCode = generateReferralCode();

        Referral referral = new Referral();
        referral.setReferrerAccountId(request.referrerAccountId());
        referral.setReferralCode(referralCode);
        referral.setReferrerReward(request.referrerReward());
        referral.setRefereeReward(request.refereeReward());
        referral.setRewardType(request.rewardType());
        referral.setExpiryDate(request.expiryDate());
        referral.setStatus(Referral.Status.PENDING);

        referral = referralRepository.save(referral);

        publishReferralEvent(referral, "CREATED");

        LOG.info("Referral created: id={}, code={}", referral.getId(), referralCode);

        return referral;
    }

    @Transactional
    public Referral completeReferral(CompleteReferralRequest request) {
        Referral referral = referralRepository.findByReferralCode(request.referralCode())
                .orElseThrow(() -> new IllegalArgumentException("Invalid referral code"));

        if (referral.getStatus() != Referral.Status.PENDING) {
            throw new IllegalArgumentException("Referral already completed or expired");
        }

        if (referral.getExpiryDate() != null && LocalDateTime.now().isAfter(referral.getExpiryDate())) {
            referral.setStatus(Referral.Status.EXPIRED);
            referralRepository.save(referral);
            throw new IllegalArgumentException("Referral code has expired");
        }

        referral.setRefereeAccountId(request.refereeAccountId());
        referral.setStatus(Referral.Status.COMPLETED);
        referral.setCompletedAt(LocalDateTime.now());
        referral = referralRepository.save(referral);

        grantReferralRewards(referral);

        publishReferralEvent(referral, "COMPLETED");

        LOG.info("Referral completed: code={}, referrer={}, referee={}",
            request.referralCode(), referral.getReferrerAccountId(), request.refereeAccountId());

        return referral;
    }

    public Optional<Referral> getReferral(UUID id) {
        return referralRepository.findById(id);
    }

    public Optional<Referral> getReferralByCode(String code) {
        return referralRepository.findByReferralCode(code);
    }

    public List<Referral> getReferralsByReferrer(String referrerAccountId) {
        return referralRepository.findByReferrerAccountId(referrerAccountId);
    }

    public ReferralSummaryResponse getReferralSummary(String referrerAccountId) {
        List<Referral> referrals = referralRepository.findByReferrerAccountId(referrerAccountId);
        long totalReferrals = referrals.size();
        long completedReferrals = referrals.stream()
            .filter(r -> r.getStatus() == Referral.Status.COMPLETED)
            .count();
        long pendingReferrals = referrals.stream()
            .filter(r -> r.getStatus() == Referral.Status.PENDING)
            .count();

        Optional<Referral> lastReferral = referrals.stream()
            .findFirst();

        String referralCode = lastReferral
            .map(Referral::getReferralCode)
            .orElse(null);

        return new ReferralSummaryResponse(
            referralCode,
            (int) totalReferrals,
            (int) completedReferrals,
            (int) pendingReferrals
        );
    }

    private void grantReferralRewards(Referral referral) {
        if (referral.getRewardType() == Referral.RewardType.CASHBACK) {
            grantCashbackReward(referral.getReferrerAccountId(), referral.getReferrerReward(),
                referral.getReferralCode(), "REFERRER");
            grantCashbackReward(referral.getRefereeAccountId(), referral.getRefereeReward(),
                referral.getReferralCode(), "REFEREE");
        } else if (referral.getRewardType() == Referral.RewardType.POINTS) {
            grantLoyaltyPoints(referral.getReferrerAccountId(), referral.getReferrerReward().intValue(),
                referral.getReferralCode(), LoyaltyPoints.TransactionType.REFERRAL_BONUS);
            grantLoyaltyPoints(referral.getRefereeAccountId(), referral.getRefereeReward().intValue(),
                referral.getReferralCode(), LoyaltyPoints.TransactionType.REFERRAL_BONUS);
        }
    }

    private void grantCashbackReward(String accountId, BigDecimal amount,
        String transactionId, String rewardType) {
        Reward reward = new Reward();
        reward.setAccountId(accountId);
        reward.setTransactionId(transactionId);
        reward.setType(Reward.RewardType.REFERRAL_BONUS);
        reward.setAmount(amount);
        reward.setTransactionAmount(BigDecimal.ZERO);
        reward.setStatus(Reward.Status.AWARDED);
        rewardRepository.save(reward);
    }

    private void grantLoyaltyPoints(String accountId, Integer points,
        String transactionId, LoyaltyPoints.TransactionType type) {
        Integer currentBalance = 0;

        LoyaltyPoints loyaltyPoints = new LoyaltyPoints();
        loyaltyPoints.setAccountId(accountId);
        loyaltyPoints.setTransactionId(transactionId);
        loyaltyPoints.setTransactionType(type);
        loyaltyPoints.setPoints(points);
        loyaltyPoints.setBalanceAfter(currentBalance + points);
        loyaltyPointsRepository.save(loyaltyPoints);
    }

    private String generateReferralCode() {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < 8; i++) {
            code.append(chars.charAt((int) (Math.random() * chars.length())));
        }
        return code.toString();
    }

    private void publishReferralEvent(Referral referral, String eventType) {
        try {
            Map<String, Object> event = Map.of(
                "referralId", referral.getId().toString(),
                "referralCode", referral.getReferralCode(),
                "status", referral.getStatus().name(),
                "eventType", eventType,
                "timestamp", LocalDateTime.now().toString()
            );
            kafkaTemplate.send(promotionEventsTopic, referral.getReferralCode(), event);
        } catch (Exception e) {
            LOG.warn("Failed to publish referral event: {}", e.getMessage());
        }
    }
}
