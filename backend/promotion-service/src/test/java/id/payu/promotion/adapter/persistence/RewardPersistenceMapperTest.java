package id.payu.promotion.adapter.persistence;

import id.payu.promotion.adapter.persistence.entity.RewardEntity;
import id.payu.promotion.domain.RewardStatus;
import id.payu.promotion.domain.RewardType;
import id.payu.promotion.domain.model.Reward;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class RewardPersistenceMapperTest {

    private final RewardPersistenceMapper mapper = new RewardPersistenceMapper();

    @Test
    void roundTripsEveryPersistedRewardField() {
        Reward reward = new Reward(
            UUID.randomUUID(), "account-1", "transaction-1", "PROMO10",
            RewardType.CASHBACK, new BigDecimal("10.2500"), 25,
            new BigDecimal("100.0000"), "merchant-1", "5411",
            RewardStatus.AWARDED, LocalDateTime.of(2026, 8, 1, 0, 0),
            LocalDateTime.of(2026, 7, 17, 12, 0), 3L
        );

        RewardEntity entity = mapper.toEntity(reward);

        assertThat(mapper.toDomain(entity)).isEqualTo(reward);
    }
}
