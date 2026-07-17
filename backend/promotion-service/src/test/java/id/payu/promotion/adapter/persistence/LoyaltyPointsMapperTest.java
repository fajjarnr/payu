package id.payu.promotion.adapter.persistence;

import id.payu.promotion.adapter.persistence.entity.LoyaltyPointsEntity;
import id.payu.promotion.domain.TransactionType;
import id.payu.promotion.domain.model.LoyaltyPoints;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LoyaltyPointsMapperTest {
    @Test
    void preservesPersistenceContractRoundTrip() {
        LoyaltyPoints source = new LoyaltyPoints();
        source.setId(UUID.randomUUID());
        source.setAccountId("account-1");
        source.setTransactionId("tx-1");
        source.setTransactionType(TransactionType.EARNED);
        source.setPoints(25);
        source.setBalanceAfter(125);
        source.setExpiryDate(LocalDateTime.of(2026, 8, 17, 10, 0));
        source.setRedeemedAt(LocalDateTime.of(2026, 7, 17, 11, 0));
        source.setCreatedAt(LocalDateTime.of(2026, 7, 17, 10, 0));
        source.setVersion(3L);

        LoyaltyPointsMapper mapper = new LoyaltyPointsMapper();
        LoyaltyPointsEntity entity = mapper.toEntity(source);
        LoyaltyPoints result = mapper.toDomain(entity);

        assertEquals(source.getId(), result.getId());
        assertEquals(source.getAccountId(), result.getAccountId());
        assertEquals(source.getTransactionId(), result.getTransactionId());
        assertEquals(source.getTransactionType(), result.getTransactionType());
        assertEquals(source.getPoints(), result.getPoints());
        assertEquals(source.getBalanceAfter(), result.getBalanceAfter());
        assertEquals(source.getExpiryDate(), result.getExpiryDate());
        assertEquals(source.getRedeemedAt(), result.getRedeemedAt());
        assertEquals(source.getCreatedAt(), result.getCreatedAt());
        assertEquals(source.getVersion(), result.getVersion());
    }
}
