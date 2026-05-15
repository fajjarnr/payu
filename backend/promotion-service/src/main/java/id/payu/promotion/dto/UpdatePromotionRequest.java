package id.payu.promotion.dto;

import id.payu.promotion.adapter.persistence.entity.PromotionEntity;
import java.time.LocalDateTime;
import id.payu.promotion.domain.PromotionStatus;

public record UpdatePromotionRequest(
    String name,
    String description,
    PromotionStatus status,
    LocalDateTime startDate,
    LocalDateTime endDate
) {}
