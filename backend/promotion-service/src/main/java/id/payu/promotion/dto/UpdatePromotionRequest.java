package id.payu.promotion.dto;

import id.payu.promotion.domain.Promotion;
import java.time.LocalDateTime;

public record UpdatePromotionRequest(
    String name,
    String description,
    Promotion.Status status,
    LocalDateTime startDate,
    LocalDateTime endDate
) {}
