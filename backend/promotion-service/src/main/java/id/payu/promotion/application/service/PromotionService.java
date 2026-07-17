package id.payu.promotion.application.service;

import id.payu.promotion.domain.model.Promotion;
import id.payu.promotion.domain.model.Reward;
import id.payu.promotion.dto.*;
import id.payu.promotion.domain.port.out.PromotionPersistencePort;
import id.payu.promotion.domain.port.out.RewardPersistencePort;
import id.payu.promotion.domain.port.out.PromotionEventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import id.payu.promotion.domain.PromotionStatus;
import id.payu.promotion.domain.PromotionType;
import id.payu.promotion.domain.RewardStatus;
import id.payu.promotion.domain.RewardType;

@Service
public class PromotionService {

    private static final Logger LOG = LoggerFactory.getLogger(PromotionService.class);

    private final PromotionPersistencePort promotionRepository;
    private final RewardPersistencePort rewardRepository;
    private final PromotionEventPublisher eventPublisher;
    private final String promotionEventsTopic;

    public PromotionService(
            PromotionPersistencePort promotionRepository,
            RewardPersistencePort rewardRepository,
            PromotionEventPublisher eventPublisher,
            @Value("${app.kafka.topics.promotion-events:payu.promotion.promotion-event.v1}") String promotionEventsTopic) {
        this.promotionRepository = promotionRepository;
        this.rewardRepository = rewardRepository;
        this.eventPublisher = eventPublisher;
        this.promotionEventsTopic = promotionEventsTopic;
    }

    @Transactional
    public Promotion createPromotion(CreatePromotionRequest request) {
        LOG.info("Creating promotion: code={}, type={}", request.code(), request.promotionType());

        validatePromotionDates(request.startDate(), request.endDate());

        Promotion promotion = new Promotion();
        promotion.setCode(request.code());
        promotion.setName(request.name());
        promotion.setDescription(request.description());
        promotion.setPromotionType(request.promotionType());
        promotion.setRewardType(request.rewardType());
        promotion.setRewardValue(request.rewardValue());
        promotion.setMaxRedemptions(request.maxRedemptions());
        promotion.setMinTransactionAmount(request.minTransactionAmount());
        promotion.setStartDate(request.startDate());
        promotion.setEndDate(request.endDate());
        promotion.setStatus(PromotionStatus.DRAFT);
        promotion.setRedemptionCount(0);

        promotion = promotionRepository.save(promotion);
        LOG.info("PromotionEntity created: id={}, code={}", promotion.getId(), promotion.getCode());

        publishPromotionEvent(promotion, "CREATED");

        return promotion;
    }

    @Transactional
    public Promotion updatePromotion(UUID id, UpdatePromotionRequest request) {
        Promotion promotion = promotionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("PromotionEntity not found"));

        if (request.name() != null) {
            promotion.setName(request.name());
        }
        if (request.description() != null) {
            promotion.setDescription(request.description());
        }
        if (request.startDate() != null) {
            validatePromotionDates(request.startDate(),
                request.endDate() != null ? request.endDate() : promotion.getEndDate());
            promotion.setStartDate(request.startDate());
        }
        if (request.endDate() != null) {
            validatePromotionDates(promotion.getStartDate(), request.endDate());
            promotion.setEndDate(request.endDate());
        }
        if (request.status() != null) {
            promotion.setStatus(request.status());
        }

        promotion = promotionRepository.save(promotion);
        LOG.info("PromotionEntity updated: id={}", promotion.getId());

        publishPromotionEvent(promotion, "UPDATED");

        return promotion;
    }

    @Transactional
    public Promotion activatePromotion(UUID id) {
        Promotion promotion = promotionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("PromotionEntity not found"));

        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(promotion.getStartDate()) || now.isAfter(promotion.getEndDate())) {
            throw new IllegalArgumentException("Cannot activate promotion outside its validity period");
        }

        promotion.setStatus(PromotionStatus.ACTIVE);
        promotion = promotionRepository.save(promotion);

        publishPromotionEvent(promotion, "ACTIVATED");

        return promotion;
    }

    public Optional<Promotion> getPromotion(UUID id) {
        return promotionRepository.findById(id);
    }

    public Optional<Promotion> getPromotionByCode(String code) {
        return promotionRepository.findByCode(code);
    }

    public List<Promotion> getActivePromotions() {
        LocalDateTime now = LocalDateTime.now();
        return promotionRepository.findActivePromotions(PromotionStatus.ACTIVE, now);
    }

    @Transactional
    public Reward claimPromotion(String code, ClaimPromotionRequest request) {
        Promotion promotion = getPromotionByCode(code)
            .orElseThrow(() -> new IllegalArgumentException("Invalid promotion code"));

        if (promotion.getStatus() != PromotionStatus.ACTIVE) {
            throw new IllegalArgumentException("PromotionEntity is not active");
        }

        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(promotion.getStartDate()) || now.isAfter(promotion.getEndDate())) {
            throw new IllegalArgumentException("PromotionEntity is expired or not yet started");
        }

        // BUG-BE-063 Fix: Use atomic increment to prevent race condition on maxRedemptions
        // The old code: read count → check < max → increment was vulnerable to concurrent claims
        // both passing the check before either increments.
        // atomicIncrementRedemptionCount returns 0 if maxRedemptions already reached.
        Optional<Promotion> incremented = promotionRepository.incrementRedemptionIfAvailable(promotion.getId());
        if (incremented.isEmpty()) {
            throw new IllegalArgumentException("PromotionEntity has reached maximum redemptions");
        }

        // Refresh promotion to reflect the atomic increment in the current persistence context
        promotion = incremented.get();

        if (promotion.getMinTransactionAmount() != null &&
            request.transactionAmount().compareTo(promotion.getMinTransactionAmount()) < 0) {
            throw new IllegalArgumentException("Transaction amount below minimum required");
        }

        BigDecimal rewardAmount = calculateRewardAmount(promotion, request.transactionAmount());

        Integer points = promotion.getPromotionType() == PromotionType.REWARD_POINTS ? rewardAmount.intValue() : null;
        Reward reward = new Reward(null, request.accountId(), request.transactionId(), promotion.getCode(),
            RewardType.PROMOTION_REWARD, rewardAmount, points, request.transactionAmount(), request.merchantCode(),
            request.categoryCode(), RewardStatus.AWARDED, null, null, null);

        reward = rewardRepository.save(reward);

        publishPromotionEvent(promotion, "CLAIMED");
        publishRewardEvent(reward);

        LOG.info("PromotionEntity claimed: code={}, accountId={}, reward={}",
            code, request.accountId(), rewardAmount);

        return reward;
    }

    private BigDecimal calculateRewardAmount(Promotion promotion, BigDecimal transactionAmount) {
        return switch (promotion.getRewardType()) {
            case PERCENTAGE -> transactionAmount.multiply(promotion.getRewardValue())
                .divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_EVEN);
            case FIXED_AMOUNT -> promotion.getRewardValue();
            case POINTS -> promotion.getRewardValue();
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
        eventPublisher.publish(
                "Promotion",
                promotion.getId().toString(),
                eventType,
                Map.of(
                        "promotionId", promotion.getId().toString(),
                        "code", promotion.getCode(),
                        "type", promotion.getPromotionType().name(),
                        "status", promotion.getStatus().name(),
                        "eventType", eventType,
                        "timestamp", LocalDateTime.now().toString()
                ),
                promotionEventsTopic
        );
    }

    private void publishRewardEvent(Reward reward) {
        eventPublisher.publish(
                "Reward",
                reward.id().toString(),
                "RewardAwarded",
                Map.of(
                        "rewardId", reward.id().toString(),
                        "accountId", reward.accountId(),
                        "amount", reward.amount().toString(),
                        "status", reward.status().name(),
                        "timestamp", LocalDateTime.now().toString()
                ),
                promotionEventsTopic
        );
    }
}
