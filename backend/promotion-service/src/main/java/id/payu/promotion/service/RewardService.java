package id.payu.promotion.service;

import id.payu.promotion.domain.Reward;
import id.payu.promotion.dto.RewardSummaryResponse;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.jboss.logging.Logger;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class RewardService {

    private static final Logger LOG = Logger.getLogger(RewardService.class);

    @jakarta.inject.Inject
    EntityManager entityManager;

    public Optional<Reward> getReward(UUID id) {
        return Reward.findByIdOptional(id);
    }

    public List<Reward> getRewardsByAccount(String accountId) {
        return Reward.list("accountId", accountId);
    }

    public List<Reward> getRewardsByAccount(String accountId, int limit, int offset) {
        return Reward.<Reward>find("accountId", accountId)
            .page(limit, offset)
            .list();
    }

    public RewardSummaryResponse getRewardSummary(String accountId) {
        BigDecimal totalCashback = new BigDecimal(
            (Double) Reward.getEntityManager()
                .createQuery("select COALESCE(sum(r.amount), 0) from Reward r where r.accountId = ?1")
                .setParameter(1, accountId)
                .getSingleResult());

        Integer totalPoints = ((Long) Reward.getEntityManager()
                .createQuery("select COALESCE(sum(r.pointsEarned), 0) from Reward r where r.accountId = ?1")
                .setParameter(1, accountId)
                .getSingleResult()).intValue();

        long transactionCount = Reward.count("accountId", accountId);

        return new RewardSummaryResponse(
            totalCashback != null ? totalCashback : BigDecimal.ZERO,
            totalPoints != null ? totalPoints : 0,
            (int) transactionCount
        );
    }
}
