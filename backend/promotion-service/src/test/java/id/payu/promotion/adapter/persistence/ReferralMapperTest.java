package id.payu.promotion.adapter.persistence;

import id.payu.promotion.adapter.persistence.entity.ReferralEntity;
import id.payu.promotion.domain.ReferralRewardType;
import id.payu.promotion.domain.ReferralStatus;
import id.payu.promotion.domain.model.Referral;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ReferralMapperTest {
    @Test
    void preservesMoneyAndStatusRoundTrip() {
        Referral source = new Referral();
        source.setId(UUID.randomUUID());
        source.setReferralCode("REF12345");
        source.setReferrerAccountId("referrer");
        source.setRefereeAccountId("referee");
        source.setReferrerReward(new BigDecimal("10.2500"));
        source.setRefereeReward(new BigDecimal("5.1250"));
        source.setRewardType(ReferralRewardType.CASHBACK);
        source.setStatus(ReferralStatus.COMPLETED);
        source.setCompletedAt(LocalDateTime.of(2026, 7, 17, 10, 0));
        source.setExpiryDate(LocalDateTime.of(2026, 8, 17, 10, 0));
        source.setCreatedAt(LocalDateTime.of(2026, 7, 1, 10, 0));
        source.setVersion(7L);

        ReferralMapper mapper = new ReferralMapper();
        ReferralEntity entity = mapper.toEntity(source);
        Referral result = mapper.toDomain(entity);

        assertEquals(source.getId(), result.getId());
        assertEquals(source.getReferrerAccountId(), result.getReferrerAccountId());
        assertEquals(source.getRefereeAccountId(), result.getRefereeAccountId());
        assertEquals(source.getReferralCode(), result.getReferralCode());
        assertEquals(source.getReferrerReward(), result.getReferrerReward());
        assertEquals(source.getRefereeReward(), result.getRefereeReward());
        assertEquals(source.getRewardType(), result.getRewardType());
        assertEquals(source.getStatus(), result.getStatus());
        assertEquals(source.getCompletedAt(), result.getCompletedAt());
        assertEquals(source.getExpiryDate(), result.getExpiryDate());
        assertEquals(source.getCreatedAt(), result.getCreatedAt());
        assertEquals(source.getVersion(), result.getVersion());
    }
}
