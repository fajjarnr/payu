package id.payu.promotion.service;

import id.payu.promotion.domain.Cashback;
import id.payu.promotion.dto.CreateCashbackRequest;
import id.payu.promotion.dto.CashbackSummaryResponse;
import io.smallrye.reactive.messaging.kafka.KafkaRecord;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.jboss.logging.Logger;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class CashbackService {

    private static final Logger LOG = Logger.getLogger(CashbackService.class);

    @jakarta.inject.Inject
    EntityManager entityManager;

    @Inject
    @Channel("promotion-events")
    Emitter<Map<String, Object>> promotionEvents;

    @Transactional
    public Cashback createCashback(CreateCashbackRequest request) {
        LOG.infof("Creating cashback: accountId=%s, transactionId=%s", 
            request.accountId(), request.transactionId());

        BigDecimal cashbackAmount = calculateCashback(request.transactionAmount(), 
            request.merchantCode(), request.categoryCode());

        Cashback cashback = new Cashback();
        cashback.accountId = request.accountId();
        cashback.transactionId = request.transactionId();
        cashback.transactionAmount = request.transactionAmount();
        cashback.cashbackAmount = cashbackAmount;
        cashback.percentage = calculatePercentage(cashbackAmount, request.transactionAmount());
        cashback.merchantCode = request.merchantCode();
        cashback.categoryCode = request.categoryCode();
        cashback.cashbackCode = request.cashbackCode();
        cashback.status = Cashback.Status.CREDITED;
        cashback.creditedAt = LocalDateTime.now();

        cashback.persist();

        publishCashbackEvent(cashback);

        LOG.infof("Cashback created: id=%s, amount=%s", cashback.id, cashbackAmount);

        return cashback;
    }

    public Optional<Cashback> getCashback(UUID id) {
        return Cashback.findByIdOptional(id);
    }

    public List<Cashback> getCashbacksByAccount(String accountId) {
        return Cashback.list("accountId", accountId);
    }

    public CashbackSummaryResponse getCashbackSummary(String accountId) {
        BigDecimal totalCashback = new BigDecimal(
            (Double) Cashback.getEntityManager()
                .createQuery("select COALESCE(sum(c.cashbackAmount), 0) from Cashback c where c.accountId = ?1")
                .setParameter(1, accountId)
                .getSingleResult());

        BigDecimal pendingCashback = new BigDecimal(
            (Double) Cashback.getEntityManager()
                .createQuery("select COALESCE(sum(c.cashbackAmount), 0) from Cashback c where c.accountId = ?1 and c.status = 'PENDING'")
                .setParameter(1, accountId)
                .getSingleResult());

        BigDecimal creditedCashback = new BigDecimal(
            (Double) Cashback.getEntityManager()
                .createQuery("select COALESCE(sum(c.cashbackAmount), 0) from Cashback c where c.accountId = ?1 and c.status = 'CREDITED'")
                .setParameter(1, accountId)
                .getSingleResult());

        long transactionCount = Cashback.count("accountId", accountId);

        return new CashbackSummaryResponse(
            totalCashback != null ? totalCashback : BigDecimal.ZERO,
            pendingCashback != null ? pendingCashback : BigDecimal.ZERO,
            creditedCashback != null ? creditedCashback : BigDecimal.ZERO,
            (int) transactionCount
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
                "cashbackId", cashback.id.toString(),
                "accountId", cashback.accountId,
                "amount", cashback.cashbackAmount,
                "status", cashback.status.name(),
                "timestamp", LocalDateTime.now().toString()
            );
            promotionEvents.send(KafkaRecord.of(cashback.accountId, event));
        } catch (Exception e) {
            LOG.warnf("Failed to publish cashback event: %s", e.getMessage());
        }
    }
}
