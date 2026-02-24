package id.payu.promotion.application.service;

import id.payu.promotion.domain.LoyaltyPoints;
import id.payu.promotion.dto.CreateLoyaltyPointsRequest;
import id.payu.promotion.dto.RedeemLoyaltyPointsRequest;
import id.payu.promotion.dto.LoyaltyBalanceResponse;
import id.payu.promotion.adapter.persistence.repository.LoyaltyPointsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
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

    /**
     * Add loyalty points to an account with race condition protection.
     * Uses pessimistic locking (SELECT FOR UPDATE) to prevent lost updates.
     *
     * @param request the points addition request
     * @return the created loyalty points record
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public LoyaltyPoints addPoints(CreateLoyaltyPointsRequest request) {
        LOG.info("Adding points: accountId={}, points={}", request.accountId(), request.points());

        // Use atomic balance calculation with pessimistic lock to prevent race conditions
        Integer currentBalance = calculateCurrentBalanceWithLock(request.accountId());

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

    /**
     * Redeem loyalty points from an account with race condition protection.
     * Uses pessimistic locking (SELECT FOR UPDATE) to prevent concurrent overdrafts.
     *
     * @param request the points redemption request
     * @return the created redemption record
     * @throws IllegalArgumentException if insufficient balance
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public LoyaltyPoints redeemPoints(RedeemLoyaltyPointsRequest request) {
        LOG.info("Redeeming points: accountId={}, points={}",
            request.accountId(), request.points());

        // Use atomic balance calculation with pessimistic lock to prevent race conditions
        Integer currentBalance = calculateCurrentBalanceWithLock(request.accountId());

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

    /**
     * Calculate current balance with pessimistic locking to prevent race conditions.
     * This method acquires a database lock on the most recent record for the account,
     * ensuring concurrent transactions serialize their access.
     *
     * @param accountId the account ID
     * @return the current balance (0 if no records)
     */
    @Transactional(readOnly = true)
    public Integer calculateCurrentBalanceWithLock(String accountId) {
        Optional<LoyaltyPoints> latestRecord = loyaltyPointsRepository
            .findTopByAccountIdOrderByCreatedAtDescWithLock(accountId);
        return latestRecord.map(LoyaltyPoints::getBalanceAfter).orElse(0);
    }

    /**
     * Calculate current balance without locking (for read-only operations).
     * Use this for queries that don't modify the balance.
     *
     * @param accountId the account ID
     * @return the current balance (0 if no records)
     */
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
