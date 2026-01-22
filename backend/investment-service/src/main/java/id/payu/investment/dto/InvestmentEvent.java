package id.payu.investment.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record InvestmentEvent(
    UUID id,
    String userId,
    String type,
    String investmentType,
    BigDecimal amount,
    String status,
    LocalDateTime createdAt
) {}
