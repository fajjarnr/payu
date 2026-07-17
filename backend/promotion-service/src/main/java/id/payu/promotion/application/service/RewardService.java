package id.payu.promotion.application.service;

import id.payu.promotion.domain.model.Reward;
import id.payu.promotion.domain.port.out.RewardPersistencePort;
import id.payu.promotion.dto.RewardSummaryResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import id.payu.promotion.domain.RewardType;

@Service
public class RewardService {

    private static final Logger LOG = LoggerFactory.getLogger(RewardService.class);

    private final RewardPersistencePort rewardRepository;

    public RewardService(RewardPersistencePort rewardRepository) {
        this.rewardRepository = rewardRepository;
    }

    public Optional<Reward> getReward(UUID id) {
        return rewardRepository.findById(id);
    }

    public List<Reward> getRewardsByAccount(String accountId) {
        return rewardRepository.findByAccountId(accountId);
    }

    public List<Reward> getRewardsByAccount(String accountId, int limit, int offset) {
        List<Reward> allRewards = rewardRepository.findByAccountId(accountId);
        int start = offset;
        int end = Math.min(offset + limit, allRewards.size());
        if (start >= allRewards.size()) {
            return List.of();
        }
        return allRewards.subList(start, end);
    }

    public RewardSummaryResponse getRewardSummary(String accountId) {
        List<Reward> rewards = rewardRepository.findByAccountId(accountId);

        BigDecimal totalCashback = rewards.stream()
            .filter(r -> r.type() == RewardType.CASHBACK)
            .map(Reward::amount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        Integer totalPoints = rewards.stream()
            .filter(r -> r.type() == RewardType.LOYALTY_POINTS)
            .map(Reward::pointsEarned)
            .filter(points -> points != null)
            .reduce(0, Integer::sum);

        int transactionCount = rewards.size();

        return new RewardSummaryResponse(
            totalCashback != null ? totalCashback : BigDecimal.ZERO,
            totalPoints != null ? totalPoints : 0,
            transactionCount
        );
    }
}
