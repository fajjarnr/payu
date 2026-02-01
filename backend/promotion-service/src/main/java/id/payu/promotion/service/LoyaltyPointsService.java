package id.payu.promotion.service;

import id.payu.promotion.domain.LoyaltyPoints;
import id.payu.promotion.dto.CreateLoyaltyPointsRequest;
import id.payu.promotion.dto.RedeemLoyaltyPointsRequest;
import id.payu.promotion.dto.LoyaltyBalanceResponse;
import id.payu.promotion.repository.LoyaltyPointsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class LoyaltyPointsService {

    private static final Logger LOG = LoggerFactory.getLogger(LoyaltyPointsService.class);

    private final LoyaltyPointsRepository loyaltyPointsRepository;
    private final KafkaTemplate<String, Map<String, Object>> kafkaTemplate;
    private final String promotionEventsTopic;

    public LoyaltyPointsService(
            LoyaltyPointsRepository loyaltyPointsRepository,
            KafkaTemplate<String, Map<String, Object>> kafkaTemplate,
            @Value("${app.kafka.topics.promotion-events:promotion-events}") String promotionEventsTopic) {
        this.loyaltyPointsRepository = loyaltyPointsRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.promotionEventsTopic = promotionEventsTopic;
    }

    @Transactional
    public LoyaltyPoints addPoints(CreateLoyaltyPointsRequest request) {
        LOG.info("Adding points: accountId={}, points={}", request.accountId(), request.points());

        Integer currentBalance = calculateCurrentBalance(request.accountId());

        LoyaltyPoints loyaltyPoints = new LoyaltyPoints();
        loyaltyPoints.setAccountId(request.accountId());
        loyaltyPoints.setTransactionId(request.transactionId());
        loyaltyPoints.setTransactionType(request.transactionType());
        loyaltyPoints.setPoints(request.points());
        loyaltyPoints.setBalanceAfter(currentBalance + request.points());
        loyaltyPoints.setExpiryDate(request.expiryDate());

        loyaltyPoints = loyaltyPointsRepository.save(loyaltyPoints);

        publishLoyaltyEvent(loyaltyPoints);

        LOG.info("Points added: accountId={}, balance={}",
            request.accountId(), loyaltyPoints.getBalanceAfter());

        return loyaltyPoints;
    }

    @Transactional
    public LoyaltyPoints redeemPoints(RedeemLoyaltyPointsRequest request) {
        LOG.info("Redeeming points: accountId={}, points={}",
            request.accountId(), request.points());

        Integer currentBalance = calculateCurrentBalance(request.accountId());

        if (currentBalance < request.points()) {
            throw new IllegalArgumentException("Insufficient loyalty points balance");
        }

        LoyaltyPoints loyaltyPoints = new LoyaltyPoints();
        loyaltyPoints.setAccountId(request.accountId());
        loyaltyPoints.setTransactionId(request.transactionId());
        loyaltyPoints.setTransactionType(LoyaltyPoints.TransactionType.REDEEMED);
        loyaltyPoints.setPoints(-request.points());
        loyaltyPoints.setBalanceAfter(currentBalance - request.points());
        loyaltyPoints.setRedeemedAt(LocalDateTime.now());

        loyaltyPoints = loyaltyPointsRepository.save(loyaltyPoints);

        publishLoyaltyEvent(loyaltyPoints);

        LOG.info("Points redeemed: accountId={}, balance={}",
            request.accountId(), loyaltyPoints.getBalanceAfter());

        return loyaltyPoints;
    }

    public Optional<LoyaltyPoints> getLoyaltyPoints(UUID id) {
        return loyaltyPointsRepository.findById(id);
    }

    public List<LoyaltyPoints> getLoyaltyPointsByAccount(String accountId) {
        return loyaltyPointsRepository.findByAccountIdOrderByCreatedAtDesc(accountId);
    }

    public LoyaltyBalanceResponse getBalance(String accountId) {
        Integer currentBalance = calculateCurrentBalance(accountId);

        List<LoyaltyPoints> allPoints = loyaltyPointsRepository.findByAccountIdOrderByCreatedAtDesc(accountId);
        long totalEarned = allPoints.stream()
            .filter(p -> p.getTransactionType() == LoyaltyPoints.TransactionType.EARNED)
            .count();

        long totalRedeemed = allPoints.stream()
            .filter(p -> p.getTransactionType() == LoyaltyPoints.TransactionType.REDEEMED)
            .count();

        long expiredPointsCount = allPoints.stream()
            .filter(p -> p.getTransactionType() == LoyaltyPoints.TransactionType.EXPIRED)
            .count();

        return new LoyaltyBalanceResponse(
            currentBalance != null ? currentBalance : 0,
            (int) totalEarned,
            (int) totalRedeemed,
            (int) expiredPointsCount
        );
    }

    public Integer calculateCurrentBalance(String accountId) {
        List<LoyaltyPoints> pointsList = loyaltyPointsRepository.findByAccountIdOrderByCreatedAtDesc(accountId);
        if (pointsList.isEmpty()) {
            return 0;
        }
        return pointsList.get(0).getBalanceAfter();
    }

    private void publishLoyaltyEvent(LoyaltyPoints loyaltyPoints) {
        try {
            Map<String, Object> event = Map.of(
                "pointsId", loyaltyPoints.getId().toString(),
                "accountId", loyaltyPoints.getAccountId(),
                "points", loyaltyPoints.getPoints(),
                "balanceAfter", loyaltyPoints.getBalanceAfter(),
                "transactionType", loyaltyPoints.getTransactionType().name(),
                "timestamp", LocalDateTime.now().toString()
            );
            kafkaTemplate.send(promotionEventsTopic, loyaltyPoints.getAccountId(), event);
        } catch (Exception e) {
            LOG.warn("Failed to publish loyalty event: {}", e.getMessage());
        }
    }
}
