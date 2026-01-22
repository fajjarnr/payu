package id.payu.promotion.service;

import id.payu.promotion.domain.Promotion;
import id.payu.promotion.domain.Reward;
import id.payu.promotion.dto.*;
import io.smallrye.reactive.messaging.kafka.KafkaRecord;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.jboss.logging.Logger;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class PromotionService {

    private static final Logger LOG = Logger.getLogger(PromotionService.class);

    @Inject
    @Channel("promotion-events")
    Emitter<Map<String, Object>> promotionEvents;

    @Transactional
    public Promotion createPromotion(CreatePromotionRequest request) {
        LOG.infof("Creating promotion: code=%s, type=%s", request.code(), request.promotionType());

        validatePromotionDates(request.startDate(), request.endDate());

        Promotion promotion = new Promotion();
        promotion.code = request.code();
        promotion.name = request.name();
        promotion.description = request.description();
        promotion.promotionType = request.promotionType();
        promotion.rewardType = request.rewardType();
        promotion.rewardValue = request.rewardValue();
        promotion.maxRedemptions = request.maxRedemptions();
        promotion.minTransactionAmount = request.minTransactionAmount();
        promotion.startDate = request.startDate();
        promotion.endDate = request.endDate();
        promotion.status = Promotion.Status.DRAFT;

        promotion.persist();
        LOG.infof("Promotion created: id=%s, code=%s", promotion.id, promotion.code);

        publishPromotionEvent(promotion, "CREATED");

        return promotion;
    }

    @Transactional
    public Promotion updatePromotion(UUID id, UpdatePromotionRequest request) {
        Promotion promotion = Promotion.findById(id);
        if (promotion == null) {
            throw new IllegalArgumentException("Promotion not found");
        }

        if (request.name() != null) {
            promotion.name = request.name();
        }
        if (request.description() != null) {
            promotion.description = request.description();
        }
        if (request.startDate() != null) {
            validatePromotionDates(request.startDate(), 
                request.endDate() != null ? request.endDate() : promotion.endDate);
            promotion.startDate = request.startDate();
        }
        if (request.endDate() != null) {
            validatePromotionDates(promotion.startDate, request.endDate());
            promotion.endDate = request.endDate();
        }
        if (request.status() != null) {
            promotion.status = request.status();
        }

        promotion.persist();
        LOG.infof("Promotion updated: id=%s", promotion.id);

        publishPromotionEvent(promotion, "UPDATED");

        return promotion;
    }

    @Transactional
    public Promotion activatePromotion(UUID id) {
        Promotion promotion = Promotion.findById(id);
        if (promotion == null) {
            throw new IllegalArgumentException("Promotion not found");
        }

        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(promotion.startDate) || now.isAfter(promotion.endDate)) {
            throw new IllegalArgumentException("Cannot activate promotion outside its validity period");
        }

        promotion.status = Promotion.Status.ACTIVE;
        promotion.persist();

        publishPromotionEvent(promotion, "ACTIVATED");

        return promotion;
    }

    public Optional<Promotion> getPromotion(UUID id) {
        return Promotion.findByIdOptional(id);
    }

    public Optional<Promotion> getPromotionByCode(String code) {
        return Promotion.find("code", code).firstResultOptional();
    }

    @Transactional
    public Reward claimPromotion(String code, ClaimPromotionRequest request) {
        Promotion promotion = getPromotionByCode(code)
            .orElseThrow(() -> new IllegalArgumentException("Invalid promotion code"));

        if (promotion.status != Promotion.Status.ACTIVE) {
            throw new IllegalArgumentException("Promotion is not active");
        }

        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(promotion.startDate) || now.isAfter(promotion.endDate)) {
            throw new IllegalArgumentException("Promotion is expired or not yet started");
        }

        if (promotion.maxRedemptions != null && promotion.redemptionCount >= promotion.maxRedemptions) {
            throw new IllegalArgumentException("Promotion has reached maximum redemptions");
        }

        if (promotion.minTransactionAmount != null && 
            request.transactionAmount().compareTo(promotion.minTransactionAmount) < 0) {
            throw new IllegalArgumentException("Transaction amount below minimum required");
        }

        BigDecimal rewardAmount = calculateRewardAmount(promotion, request.transactionAmount());

        Reward reward = new Reward();
        reward.accountId = request.accountId();
        reward.transactionId = request.transactionId();
        reward.promotionCode = promotion.code;
        reward.type = Reward.RewardType.PROMOTION_REWARD;
        reward.amount = rewardAmount;
        reward.transactionAmount = request.transactionAmount();
        reward.merchantCode = request.merchantCode();
        reward.categoryCode = request.categoryCode();
        reward.status = Reward.Status.AWARDED;

        if (promotion.promotionType == Promotion.PromotionType.REWARD_POINTS) {
            reward.pointsEarned = rewardAmount.intValue();
        }

        reward.persist();

        promotion.redemptionCount++;
        promotion.persist();

        publishPromotionEvent(promotion, "CLAIMED");
        publishRewardEvent(reward);

        LOG.infof("Promotion claimed: code=%s, accountId=%s, reward=%s", 
            code, request.accountId(), rewardAmount);

        return reward;
    }

    private BigDecimal calculateRewardAmount(Promotion promotion, BigDecimal transactionAmount) {
        return switch (promotion.rewardType) {
            case PERCENTAGE -> transactionAmount.multiply(promotion.rewardValue)
                .divide(BigDecimal.valueOf(100), 2, BigDecimal.ROUND_HALF_UP);
            case FIXED_AMOUNT -> promotion.rewardValue;
            case POINTS -> promotion.rewardValue;
        };
    }

    private void validatePromotionDates(LocalDateTime startDate, LocalDateTime endDate) {
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("Start date and end date are required");
        }
        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("End date must be after start date");
        }
    }

    private void publishPromotionEvent(Promotion promotion, String eventType) {
        try {
            Map<String, Object> event = Map.of(
                "promotionId", promotion.id.toString(),
                "code", promotion.code,
                "type", promotion.promotionType.name(),
                "status", promotion.status.name(),
                "eventType", eventType,
                "timestamp", LocalDateTime.now().toString()
            );
            promotionEvents.send(KafkaRecord.of(promotion.code, event));
        } catch (Exception e) {
            LOG.warnf("Failed to publish promotion event: %s", e.getMessage());
        }
    }

    private void publishRewardEvent(Reward reward) {
        try {
            Map<String, Object> event = Map.of(
                "rewardId", reward.id.toString(),
                "accountId", reward.accountId,
                "amount", reward.amount,
                "status", reward.status.name(),
                "timestamp", LocalDateTime.now().toString()
            );
            promotionEvents.send(KafkaRecord.of(reward.accountId, event));
        } catch (Exception e) {
            LOG.warnf("Failed to publish reward event: %s", e.getMessage());
        }
    }
}
