package id.payu.promotion.service;

import id.payu.promotion.domain.Referral;
import id.payu.promotion.domain.Reward;
import id.payu.promotion.domain.LoyaltyPoints;
import id.payu.promotion.dto.CreateReferralRequest;
import id.payu.promotion.dto.CompleteReferralRequest;
import id.payu.promotion.dto.ReferralSummaryResponse;
import io.smallrye.reactive.messaging.kafka.KafkaRecord;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.jboss.logging.Logger;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class ReferralService {

    private static final Logger LOG = Logger.getLogger(ReferralService.class);

    @jakarta.inject.Inject
    EntityManager entityManager;

    @Inject
    @Channel("promotion-events")
    Emitter<Map<String, Object>> promotionEvents;

    @Transactional
    public Referral createReferral(CreateReferralRequest request) {
        LOG.infof("Creating referral: referrer=%s", request.referrerAccountId());

        String referralCode = generateReferralCode();

        Referral referral = new Referral();
        referral.referrerAccountId = request.referrerAccountId();
        referral.referralCode = referralCode;
        referral.referrerReward = request.referrerReward();
        referral.refereeReward = request.refereeReward();
        referral.rewardType = request.rewardType();
        referral.expiryDate = request.expiryDate();
        referral.status = Referral.Status.PENDING;

        referral.persist();

        publishReferralEvent(referral, "CREATED");

        LOG.infof("Referral created: id=%s, code=%s", referral.id, referralCode);

        return referral;
    }

    @Transactional
    public Referral completeReferral(CompleteReferralRequest request) {
        Referral referral = Referral.<Referral>find("referralCode", request.referralCode())
            .firstResult();

        if (referral == null) {
            throw new IllegalArgumentException("Invalid referral code");
        }

        if (referral.status != Referral.Status.PENDING) {
            throw new IllegalArgumentException("Referral already completed or expired");
        }

        if (referral.expiryDate != null && LocalDateTime.now().isAfter(referral.expiryDate)) {
            referral.status = Referral.Status.EXPIRED;
            referral.persist();
            throw new IllegalArgumentException("Referral code has expired");
        }

        referral.refereeAccountId = request.refereeAccountId();
        referral.status = Referral.Status.COMPLETED;
        referral.completedAt = LocalDateTime.now();
        referral.persist();

        grantReferralRewards(referral);

        publishReferralEvent(referral, "COMPLETED");

        LOG.infof("Referral completed: code=%s, referrer=%s, referee=%s",
            request.referralCode(), referral.referrerAccountId, request.refereeAccountId());

        return referral;
    }

    public Optional<Referral> getReferral(UUID id) {
        return Referral.findByIdOptional(id);
    }

    public Optional<Referral> getReferralByCode(String code) {
        return Referral.<Referral>find("referralCode", code).firstResultOptional();
    }

    public List<Referral> getReferralsByReferrer(String referrerAccountId) {
        return Referral.list("referrerAccountId", referrerAccountId);
    }

    public ReferralSummaryResponse getReferralSummary(String referrerAccountId) {
        List<Referral> referrals = Referral.list("referrerAccountId", referrerAccountId);
        long totalReferrals = referrals.size();
        long completedReferrals = referrals.stream()
            .filter(r -> r.status == Referral.Status.COMPLETED)
            .count();
        long pendingReferrals = referrals.stream()
            .filter(r -> r.status == Referral.Status.PENDING)
            .count();

        Optional<Referral> lastReferral = referrals.stream()
            .findFirst();

        String referralCode = lastReferral
            .map(r -> r.referralCode)
            .orElse(null);

        return new ReferralSummaryResponse(
            referralCode,
            (int) totalReferrals,
            (int) completedReferrals,
            (int) pendingReferrals
        );
    }

    private void grantReferralRewards(Referral referral) {
        if (referral.rewardType == Referral.RewardType.CASHBACK) {
            grantCashbackReward(referral.referrerAccountId, referral.referrerReward, 
                referral.referralCode, "REFERRER");
            grantCashbackReward(referral.refereeAccountId, referral.refereeReward,
                referral.referralCode, "REFEREE");
        } else if (referral.rewardType == Referral.RewardType.POINTS) {
            grantLoyaltyPoints(referral.referrerAccountId, referral.referrerReward.intValue(),
                referral.referralCode, LoyaltyPoints.TransactionType.REFERRAL_BONUS);
            grantLoyaltyPoints(referral.refereeAccountId, referral.refereeReward.intValue(),
                referral.referralCode, LoyaltyPoints.TransactionType.REFERRAL_BONUS);
        }
    }

    private void grantCashbackReward(String accountId, BigDecimal amount, 
        String transactionId, String rewardType) {
        Reward reward = new Reward();
        reward.accountId = accountId;
        reward.transactionId = transactionId;
        reward.type = Reward.RewardType.REFERRAL_BONUS;
        reward.amount = amount;
        reward.transactionAmount = BigDecimal.ZERO;
        reward.status = Reward.Status.AWARDED;
        reward.persist();
    }

    private void grantLoyaltyPoints(String accountId, Integer points, 
        String transactionId, LoyaltyPoints.TransactionType type) {
        Integer currentBalance = 0;

        LoyaltyPoints loyaltyPoints = new LoyaltyPoints();
        loyaltyPoints.accountId = accountId;
        loyaltyPoints.transactionId = transactionId;
        loyaltyPoints.transactionType = type;
        loyaltyPoints.points = points;
        loyaltyPoints.balanceAfter = currentBalance + points;
        loyaltyPoints.persist();
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
                "referralId", referral.id.toString(),
                "referralCode", referral.referralCode,
                "status", referral.status.name(),
                "eventType", eventType,
                "timestamp", LocalDateTime.now().toString()
            );
            promotionEvents.send(KafkaRecord.of(referral.referralCode, event));
        } catch (Exception e) {
            LOG.warnf("Failed to publish referral event: %s", e.getMessage());
        }
    }
}
