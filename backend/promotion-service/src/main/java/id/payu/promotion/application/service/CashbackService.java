package id.payu.promotion.application.service;

import id.payu.promotion.domain.Cashback;
import id.payu.promotion.dto.CreateCashbackRequest;
import id.payu.promotion.dto.CashbackSummaryResponse;
import id.payu.promotion.adapter.persistence.repository.CashbackRepository;
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
public class CashbackService {

    private static final Logger LOG = LoggerFactory.getLogger(CashbackService.class);

    private final CashbackRepository cashbackRepository;
    private final KafkaTemplate<String, Map<String, Object>> kafkaTemplate;
    private final String promotionEventsTopic;

    public CashbackService(
            CashbackRepository cashbackRepository,
            KafkaTemplate<String, Map<String, Object>> kafkaTemplate,
            @Value("${app.kafka.topics.promotion-events:promotion-events}") String promotionEventsTopic) {
        this.cashbackRepository = cashbackRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.promotionEventsTopic = promotionEventsTopic;
    }

    @Transactional
    public Cashback createCashback(CreateCashbackRequest request) {
        LOG.info("Creating cashback: accountId={}, transactionId={}",
            request.accountId(), request.transactionId());

        BigDecimal cashbackAmount = calculateCashback(request.transactionAmount(),
            request.merchantCode(), request.categoryCode());

        Cashback cashback = new Cashback();
        cashback.setAccountId(request.accountId());
        cashback.setTransactionId(request.transactionId());
        cashback.setTransactionAmount(request.transactionAmount());
        cashback.setCashbackAmount(cashbackAmount);
        cashback.setPercentage(calculatePercentage(cashbackAmount, request.transactionAmount()));
        cashback.setMerchantCode(request.merchantCode());
        cashback.setCategoryCode(request.categoryCode());
        cashback.setCashbackCode(request.cashbackCode());
        cashback.setStatus(Cashback.Status.CREDITED);
        cashback.setCreditedAt(LocalDateTime.now());

        cashback = cashbackRepository.save(cashback);

        publishCashbackEvent(cashback);

        LOG.info("Cashback created: id={}, amount={}", cashback.getId(), cashbackAmount);

        return cashback;
    }

    public Optional<Cashback> getCashback(UUID id) {
        return cashbackRepository.findById(id);
    }

    public List<Cashback> getCashbacksByAccount(String accountId) {
        return cashbackRepository.findByAccountId(accountId);
    }

    public CashbackSummaryResponse getCashbackSummary(String accountId) {
        List<Cashback> cashbacks = cashbackRepository.findByAccountId(accountId);

        BigDecimal totalCashback = cashbacks.stream()
            .map(Cashback::getCashbackAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal pendingCashback = cashbacks.stream()
            .filter(c -> c.getStatus() == Cashback.Status.PENDING)
            .map(Cashback::getCashbackAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal creditedCashback = cashbacks.stream()
            .filter(c -> c.getStatus() == Cashback.Status.CREDITED)
            .map(Cashback::getCashbackAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        int transactionCount = cashbacks.size();

        return new CashbackSummaryResponse(
            totalCashback != null ? totalCashback : BigDecimal.ZERO,
            pendingCashback != null ? pendingCashback : BigDecimal.ZERO,
            creditedCashback != null ? creditedCashback : BigDecimal.ZERO,
            transactionCount
        );
    }

    private BigDecimal calculateCashback(BigDecimal transactionAmount, String merchantCode, String categoryCode) {
        double percentage = 0.01;

        if (categoryCode != null) {
            percentage = switch (categoryCode.toUpperCase()) {
                case "GROCERY" -> 0.02;
                case "DINING" -> 0.03;
                case "SHOPPING" -> 0.015;
                default -> 0.01;
            };
        }

        return transactionAmount.multiply(BigDecimal.valueOf(percentage))
            .setScale(2, BigDecimal.ROUND_HALF_UP);
    }

    private BigDecimal calculatePercentage(BigDecimal cashbackAmount, BigDecimal transactionAmount) {
        if (transactionAmount.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return cashbackAmount.divide(transactionAmount, 4, BigDecimal.ROUND_HALF_UP)
            .multiply(BigDecimal.valueOf(100));
    }

    private void publishCashbackEvent(Cashback cashback) {
        try {
            Map<String, Object> event = Map.of(
                "cashbackId", cashback.getId().toString(),
                "accountId", cashback.getAccountId(),
                "amount", cashback.getCashbackAmount().toString(),
                "status", cashback.getStatus().name(),
                "timestamp", LocalDateTime.now().toString()
            );
            kafkaTemplate.send(promotionEventsTopic, cashback.getAccountId(), event);
        } catch (Exception e) {
            LOG.warn("Failed to publish cashback event: {}", e.getMessage());
        }
    }
}
