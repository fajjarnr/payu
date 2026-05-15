package id.payu.promotion.application.service;

import id.payu.promotion.adapter.persistence.entity.PromotionEntity;
import id.payu.promotion.adapter.persistence.entity.RewardEntity;
import id.payu.promotion.dto.*;
import id.payu.promotion.adapter.persistence.repository.PromotionRepository;
import id.payu.promotion.adapter.persistence.repository.RewardRepository;
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
import id.payu.promotion.domain.PromotionStatus;
import id.payu.promotion.domain.PromotionType;
import id.payu.promotion.domain.RewardStatus;
import id.payu.promotion.domain.RewardType;

@Service
public class PromotionService {

    private static final Logger LOG = LoggerFactory.getLogger(PromotionService.class);

    private final PromotionRepository promotionRepository;
    private final RewardRepository rewardRepository;
    private final KafkaTemplate<String, Map<String, Object>> kafkaTemplate;
    private final String promotionEventsTopic;
    private final jakarta.persistence.EntityManager entityManager;

    public PromotionService(
            PromotionRepository promotionRepository,
            RewardRepository rewardRepository,
            KafkaTemplate<String, Map<String, Object>> kafkaTemplate,
            @Value("${app.kafka.topics.promotion-events:promotion-events}") String promotionEventsTopic,
            jakarta.persistence.EntityManager entityManager) {
        this.promotionRepository = promotionRepository;
        this.rewardRepository = rewardRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.promotionEventsTopic = promotionEventsTopic;
        this.entityManager = entityManager;
    }

    @Transactional
    public PromotionEntity createPromotion(CreatePromotionRequest request) {
        LOG.info("Creating promotion: code={}, type={}", request.code(), request.promotionType());

        validatePromotionDates(request.startDate(), request.endDate());

        PromotionEntity promotion = new PromotionEntity();
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
    public PromotionEntity updatePromotion(UUID id, UpdatePromotionRequest request) {
        PromotionEntity promotion = promotionRepository.findById(id)
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
    public PromotionEntity activatePromotion(UUID id) {
        PromotionEntity promotion = promotionRepository.findById(id)
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

    public Optional<PromotionEntity> getPromotion(UUID id) {
        return promotionRepository.findById(id);
    }

    public Optional<PromotionEntity> getPromotionByCode(String code) {
        return promotionRepository.findByCode(code);
    }

    public List<PromotionEntity> getActivePromotions() {
        LocalDateTime now = LocalDateTime.now();
        return promotionRepository.findActivePromotions(PromotionStatus.ACTIVE, now);
    }

    @Transactional
    public RewardEntity claimPromotion(String code, ClaimPromotionRequest request) {
        PromotionEntity promotion = getPromotionByCode(code)
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
        int updated = promotionRepository.atomicIncrementRedemptionCount(promotion.getId());
        if (updated == 0) {
            throw new IllegalArgumentException("PromotionEntity has reached maximum redemptions");
        }

        // Refresh promotion to reflect the atomic increment in the current persistence context
        entityManager.refresh(promotion);

        if (promotion.getMinTransactionAmount() != null &&
            request.transactionAmount().compareTo(promotion.getMinTransactionAmount()) < 0) {
            throw new IllegalArgumentException("Transaction amount below minimum required");
        }

        BigDecimal rewardAmount = calculateRewardAmount(promotion, request.transactionAmount());

        RewardEntity reward = new RewardEntity();
        reward.setAccountId(request.accountId());
        reward.setTransactionId(request.transactionId());
        reward.setPromotionCode(promotion.getCode());
        reward.setType(RewardType.PROMOTION_REWARD);
        reward.setAmount(rewardAmount);
        reward.setTransactionAmount(request.transactionAmount());
        reward.setMerchantCode(request.merchantCode());
        reward.setCategoryCode(request.categoryCode());
        reward.setStatus(RewardStatus.AWARDED);

        if (promotion.getPromotionType() == PromotionType.REWARD_POINTS) {
            reward.setPointsEarned(rewardAmount.intValue());
        }

        reward = rewardRepository.save(reward);

        publishPromotionEvent(promotion, "CLAIMED");
        publishRewardEvent(reward);

        LOG.info("PromotionEntity claimed: code={}, accountId={}, reward={}",
            code, request.accountId(), rewardAmount);

        return reward;
    }

    private BigDecimal calculateRewardAmount(PromotionEntity promotion, BigDecimal transactionAmount) {
        return switch (promotion.getRewardType()) {
            case PERCENTAGE -> transactionAmount.multiply(promotion.getRewardValue())
                .divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);
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

    private void publishPromotionEvent(PromotionEntity promotion, String eventType) {
        try {
            Map<String, Object> event = Map.of(
                "promotionId", promotion.getId().toString(),
                "code", promotion.getCode(),
                "type", promotion.getPromotionType().name(),
                "status", promotion.getStatus().name(),
                "eventType", eventType,
                "timestamp", LocalDateTime.now().toString()
            );
            kafkaTemplate.send(promotionEventsTopic, promotion.getCode(), event);
        } catch (Exception e) {
            LOG.warn("Failed to publish promotion event: {}", e.getMessage());
        }
    }

    private void publishRewardEvent(RewardEntity reward) {
        try {
            Map<String, Object> event = Map.of(
                "rewardId", reward.getId().toString(),
                "accountId", reward.getAccountId(),
                "amount", reward.getAmount().toString(),
                "status", reward.getStatus().name(),
                "timestamp", LocalDateTime.now().toString()
            );
            kafkaTemplate.send(promotionEventsTopic, reward.getAccountId(), event);
        } catch (Exception e) {
            LOG.warn("Failed to publish reward event: {}", e.getMessage());
        }
    }
}
