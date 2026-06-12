package id.payu.promotion.application.service;

import id.payu.promotion.adapter.persistence.entity.LoyaltyPointsEntity;
import id.payu.promotion.dto.CreateLoyaltyPointsRequest;
import id.payu.promotion.dto.RedeemLoyaltyPointsRequest;
import id.payu.promotion.dto.LoyaltyBalanceResponse;
import id.payu.promotion.adapter.persistence.repository.LoyaltyPointsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import id.payu.outbox.service.OutboxService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import id.payu.promotion.domain.TransactionType;

@Service
public class LoyaltyPointsService {

    private static final Logger LOG = LoggerFactory.getLogger(LoyaltyPointsService.class);

    private final LoyaltyPointsRepository loyaltyPointsRepository;
    private final OutboxService outboxService;
    private final String promotionEventsTopic;
    private final jakarta.persistence.EntityManager entityManager;

    public LoyaltyPointsService(
            LoyaltyPointsRepository loyaltyPointsRepository,
            OutboxService outboxService,
            @Value("${app.kafka.topics.promotion-events:payu.promotion.loyalty-event.v1}") String promotionEventsTopic,
            jakarta.persistence.EntityManager entityManager) {
        this.loyaltyPointsRepository = loyaltyPointsRepository;
        this.outboxService = outboxService;
        this.promotionEventsTopic = promotionEventsTopic;
        this.entityManager = entityManager;
    }

    /**
     * Add loyalty points to an account with race condition protection.
     * Uses pessimistic locking (SELECT FOR UPDATE) to prevent lost updates.
     *
     * @param request the points addition request
     * @return the created loyalty points record
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public LoyaltyPointsEntity addPoints(CreateLoyaltyPointsRequest request) {
        LOG.info("Adding points: accountId={}, points={}", request.accountId(), request.points());

        // Use atomic balance calculation with pessimistic lock to prevent race conditions
        Integer currentBalance = calculateCurrentBalanceWithLock(request.accountId());

        LoyaltyPointsEntity loyaltyPoints = new LoyaltyPointsEntity();
        loyaltyPoints.setAccountId(request.accountId());
        loyaltyPoints.setTransactionId(request.transactionId());
        loyaltyPoints.setTransactionType(request.transactionType());
        loyaltyPoints.setPoints(request.points());
        loyaltyPoints.setBalanceAfter(currentBalance + request.points());
        loyaltyPoints.setExpiryDate(request.expiryDate());

        loyaltyPoints = loyaltyPointsRepository.save(loyaltyPoints);
        entityManager.flush();

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
    public LoyaltyPointsEntity redeemPoints(RedeemLoyaltyPointsRequest request) {
        LOG.info("Redeeming points: accountId={}, points={}",
            request.accountId(), request.points());

        // Use atomic balance calculation with pessimistic lock to prevent race conditions
        Integer currentBalance = calculateCurrentBalanceWithLock(request.accountId());

        if (currentBalance < request.points()) {
            throw new IllegalArgumentException("Insufficient loyalty points balance");
        }

        LoyaltyPointsEntity loyaltyPoints = new LoyaltyPointsEntity();
        loyaltyPoints.setAccountId(request.accountId());
        loyaltyPoints.setTransactionId(request.transactionId());
        loyaltyPoints.setTransactionType(TransactionType.REDEEMED);
        loyaltyPoints.setPoints(-request.points());
        loyaltyPoints.setBalanceAfter(currentBalance - request.points());
        loyaltyPoints.setRedeemedAt(LocalDateTime.now());

        loyaltyPoints = loyaltyPointsRepository.save(loyaltyPoints);

        publishLoyaltyEvent(loyaltyPoints);

        LOG.info("Points redeemed: accountId={}, balance={}",
            request.accountId(), loyaltyPoints.getBalanceAfter());

        return loyaltyPoints;
    }

    public Optional<LoyaltyPointsEntity> getLoyaltyPoints(UUID id) {
        return loyaltyPointsRepository.findById(id);
    }

    public List<LoyaltyPointsEntity> getLoyaltyPointsByAccount(String accountId) {
        return loyaltyPointsRepository.findByAccountIdOrderByCreatedAtDesc(accountId);
    }

    public LoyaltyBalanceResponse getBalance(String accountId) {
        Integer currentBalance = calculateCurrentBalance(accountId);

        List<LoyaltyPointsEntity> allPoints = loyaltyPointsRepository.findByAccountIdOrderByCreatedAtDesc(accountId);
        // BUG-BE-065 Fix: Use .sum() of actual points, not .count() of records.
        // .count() returned number of transactions, not total points value.
        long totalEarned = allPoints.stream()
            .filter(p -> p.getTransactionType() == TransactionType.EARNED)
            .mapToInt(LoyaltyPointsEntity::getPoints)
            .sum();

        long totalRedeemed = allPoints.stream()
            .filter(p -> p.getTransactionType() == TransactionType.REDEEMED)
            .mapToInt(p -> Math.abs(p.getPoints()))
            .sum();

        long expiredPointsCount = allPoints.stream()
            .filter(p -> p.getTransactionType() == TransactionType.EXPIRED)
            .mapToInt(p -> Math.abs(p.getPoints()))
            .sum();

        // XBUG-012 FIX: Compute pointsExpiring and nearest expiryDate
        // Points that are EARNED, not yet expired, and have an expiry date in the future
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        java.time.LocalDateTime expiryWindow = now.plusDays(30); // 30-day look-ahead

        // Find the nearest expiry date among earned, non-expired points  
        java.time.LocalDateTime nearestExpiry = allPoints.stream()
            .filter(p -> p.getTransactionType() == TransactionType.EARNED)
            .filter(p -> p.getExpiryDate() != null)
            .filter(p -> p.getExpiryDate().isAfter(now))
            .filter(p -> p.getExpiryDate().isBefore(expiryWindow))
            .map(LoyaltyPointsEntity::getExpiryDate)
            .min(java.time.LocalDateTime::compareTo)
            .orElse(null);

        // Sum points expiring within the 30-day window
        int pointsExpiring = nearestExpiry == null ? 0 : allPoints.stream()
            .filter(p -> p.getTransactionType() == TransactionType.EARNED)
            .filter(p -> p.getExpiryDate() != null)
            .filter(p -> p.getExpiryDate().isAfter(now) && p.getExpiryDate().isBefore(expiryWindow))
            .mapToInt(LoyaltyPointsEntity::getPoints)
            .sum();

        java.time.Instant expiryInstant = nearestExpiry != null
            ? nearestExpiry.atZone(java.time.ZoneId.systemDefault()).toInstant()
            : null;

        return new LoyaltyBalanceResponse(
            currentBalance != null ? currentBalance : 0,
            (int) totalEarned,
            (int) totalRedeemed,
            (int) expiredPointsCount,
            pointsExpiring,
            expiryInstant
        );
    }

    /**
     * Calculate current balance with pessimistic locking to prevent race conditions.
     * Uses PostgreSQL advisory lock to ensure serialization even when no previous
     * records exist for the account, preventing phantom reads/lost updates.
     *
     * @param accountId the account ID
     * @return the current balance (0 if no records)
     */
    @Transactional(readOnly = false) // MUST be readWrite for advisory lock to hold correctly in PG
    public Integer calculateCurrentBalanceWithLock(String accountId) {
        // Acquire transaction-level advisory lock using Postgres hash function
        entityManager.createNativeQuery("SELECT pg_advisory_xact_lock(hashtext(:accountId))")
                     .setParameter("accountId", accountId)
                     .getSingleResult();

        // Safe to read the balance now, no other transaction can concurrently insert for this account
        Integer balance = loyaltyPointsRepository.calculateBalanceByAccountId(accountId);
        return balance != null ? balance : 0;
    }

    /**
     * Calculate current balance without locking (for read-only operations).
     * Use this for queries that don't modify the balance.
     *
     * @param accountId the account ID
     * @return the current balance (0 if no records)
     */
    public Integer calculateCurrentBalance(String accountId) {
        Integer balance = loyaltyPointsRepository.calculateBalanceByAccountId(accountId);
        return balance != null ? balance : 0;
    }

    private void publishLoyaltyEvent(LoyaltyPointsEntity loyaltyPoints) {
        outboxService.createEvent(
                "LoyaltyPoints",
                loyaltyPoints.getId().toString(),
                loyaltyPoints.getTransactionType().name(),
                Map.of(
                        "pointsId", loyaltyPoints.getId().toString(),
                        "accountId", loyaltyPoints.getAccountId(),
                        "points", loyaltyPoints.getPoints(),
                        "balanceAfter", loyaltyPoints.getBalanceAfter(),
                        "transactionType", loyaltyPoints.getTransactionType().name(),
                        "timestamp", LocalDateTime.now().toString()
                ),
                null,
                promotionEventsTopic
        );
    }
}
