package id.payu.promotion.service;

import id.payu.promotion.domain.LoyaltyPoints;
import id.payu.promotion.dto.CreateLoyaltyPointsRequest;
import id.payu.promotion.dto.RedeemLoyaltyPointsRequest;
import id.payu.promotion.dto.LoyaltyPointsResponse;
import id.payu.promotion.dto.LoyaltyBalanceResponse;
import io.smallrye.reactive.messaging.kafka.KafkaRecord;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.jboss.logging.Logger;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class LoyaltyPointsService {

    private static final Logger LOG = Logger.getLogger(LoyaltyPointsService.class);

    @jakarta.inject.Inject
    EntityManager entityManager;

    @Inject
    @Channel("promotion-events")
    Emitter<Map<String, Object>> promotionEvents;

    @Transactional
    public LoyaltyPoints addPoints(CreateLoyaltyPointsRequest request) {
        LOG.infof("Adding points: accountId=%s, points=%s", request.accountId(), request.points());

        Integer currentBalance = calculateCurrentBalance(request.accountId());

        LoyaltyPoints loyaltyPoints = new LoyaltyPoints();
        loyaltyPoints.accountId = request.accountId();
        loyaltyPoints.transactionId = request.transactionId();
        loyaltyPoints.transactionType = request.transactionType();
        loyaltyPoints.points = request.points();
        loyaltyPoints.balanceAfter = currentBalance + request.points();
        loyaltyPoints.expiryDate = request.expiryDate();

        loyaltyPoints.persist();

        publishLoyaltyEvent(loyaltyPoints);

        LOG.infof("Points added: accountId=%s, balance=%s", 
            request.accountId(), loyaltyPoints.balanceAfter);

        return loyaltyPoints;
    }

    @Transactional
    public LoyaltyPoints redeemPoints(RedeemLoyaltyPointsRequest request) {
        LOG.infof("Redeeming points: accountId=%s, points=%s", 
            request.accountId(), request.points());

        Integer currentBalance = calculateCurrentBalance(request.accountId());

        if (currentBalance < request.points()) {
            throw new IllegalArgumentException("Insufficient loyalty points balance");
        }

        LoyaltyPoints loyaltyPoints = new LoyaltyPoints();
        loyaltyPoints.accountId = request.accountId();
        loyaltyPoints.transactionId = request.transactionId();
        loyaltyPoints.transactionType = LoyaltyPoints.TransactionType.REDEEMED;
        loyaltyPoints.points = -request.points();
        loyaltyPoints.balanceAfter = currentBalance - request.points();
        loyaltyPoints.redeemedAt = LocalDateTime.now();

        loyaltyPoints.persist();

        publishLoyaltyEvent(loyaltyPoints);

        LOG.infof("Points redeemed: accountId=%s, balance=%s", 
            request.accountId(), loyaltyPoints.balanceAfter);

        return loyaltyPoints;
    }

    public Optional<LoyaltyPoints> getLoyaltyPoints(UUID id) {
        return LoyaltyPoints.findByIdOptional(id);
    }

    public List<LoyaltyPoints> getLoyaltyPointsByAccount(String accountId) {
        return LoyaltyPoints.<LoyaltyPoints>find("accountId = ?1 order by createdAt desc", accountId)
            .list();
    }

    public LoyaltyBalanceResponse getBalance(String accountId) {
        Integer currentBalance = calculateCurrentBalance(accountId);

        Long totalEarned = LoyaltyPoints.count("accountId = ?1 and transactionType = ?2", 
            accountId, LoyaltyPoints.TransactionType.EARNED);

        Long totalRedeemed = LoyaltyPoints.count("accountId = ?1 and transactionType = ?2", 
            accountId, LoyaltyPoints.TransactionType.REDEEMED);

        Long expiredPointsCount = LoyaltyPoints.count("accountId = ?1 and transactionType = ?2", 
            accountId, LoyaltyPoints.TransactionType.EXPIRED);

        return new LoyaltyBalanceResponse(
            currentBalance != null ? currentBalance : 0,
            totalEarned != null ? totalEarned.intValue() : 0,
            totalRedeemed != null ? totalRedeemed.intValue() : 0,
            expiredPointsCount != null ? expiredPointsCount.intValue() : 0
        );
    }

    static Integer calculateCurrentBalance(String accountId) {
        LoyaltyPoints latestRecord = LoyaltyPoints.<LoyaltyPoints>find(
            "accountId = ?1 order by createdAt desc", accountId)
            .firstResult();
        
        if (latestRecord != null) {
            return latestRecord.balanceAfter;
        }
        return 0;
    }

    private void publishLoyaltyEvent(LoyaltyPoints loyaltyPoints) {
        try {
            Map<String, Object> event = Map.of(
                "pointsId", loyaltyPoints.id.toString(),
                "accountId", loyaltyPoints.accountId,
                "points", loyaltyPoints.points,
                "balanceAfter", loyaltyPoints.balanceAfter,
                "transactionType", loyaltyPoints.transactionType.name(),
                "timestamp", LocalDateTime.now().toString()
            );
            promotionEvents.send(KafkaRecord.of(loyaltyPoints.accountId, event));
        } catch (Exception e) {
            LOG.warnf("Failed to publish loyalty event: %s", e.getMessage());
        }
    }
}
