package id.payu.wallet.adapter.persistence.mapper;

import id.payu.wallet.adapter.persistence.entity.WalletEntity;
import id.payu.wallet.domain.model.Wallet;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link WalletMapper}.
 *
 * <p>Verifies:
 * <ul>
 *   <li>Domain to entity mapping</li>
 *   <li>Entity to domain mapping</li>
 *   <li>Collection mapping</li>
 *   <li>Enum mapping</li>
 *   <li>Null handling</li>
 * </ul>
 *
 * <p>IMP-069: MapStruct Entity-Domain Mapping</p>
 *
 * @since IMP-069
 */
class WalletMapperTest {

    private final WalletMapper mapper = Mappers.getMapper(WalletMapper.class);

    @Test
    void toEntity_shouldMapDomainToEntity() {
        // Given
        UUID id = UUID.randomUUID();
        Wallet domain = Wallet.builder()
                .id(id)
                .accountId("ACC-123")
                .balance(new BigDecimal("1000.00"))
                .reservedBalance(new BigDecimal("100.00"))
                .currency("IDR")
                .status(Wallet.WalletStatus.ACTIVE)
                .version(1L)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        // When
        WalletEntity entity = mapper.toEntity(domain);

        // Then
        assertThat(entity).isNotNull();
        assertThat(entity.getId()).isEqualTo(id);
        assertThat(entity.getAccountId()).isEqualTo("ACC-123");
        assertThat(entity.getBalance()).isEqualTo(new BigDecimal("1000.00"));
        assertThat(entity.getReservedBalance()).isEqualTo(new BigDecimal("100.00"));
        assertThat(entity.getCurrency()).isEqualTo("IDR");
        assertThat(entity.getStatus()).isEqualTo(WalletEntity.WalletStatus.ACTIVE);
        assertThat(entity.getVersion()).isEqualTo(1L);
        // tenantId should be null (ignored in mapping)
        assertThat(entity.getTenantId()).isNull();
    }

    @Test
    void toDomain_shouldMapEntityToDomain() {
        // Given
        UUID id = UUID.randomUUID();
        WalletEntity entity = WalletEntity.builder()
                .id(id)
                .tenantId("TENANT-1")
                .accountId("ACC-123")
                .balance(new BigDecimal("1000.00"))
                .reservedBalance(new BigDecimal("100.00"))
                .currency("IDR")
                .status(WalletEntity.WalletStatus.ACTIVE)
                .version(1L)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        // When
        Wallet domain = mapper.toDomain(entity);

        // Then
        assertThat(domain).isNotNull();
        assertThat(domain.getId()).isEqualTo(id);
        assertThat(domain.getAccountId()).isEqualTo("ACC-123");
        assertThat(domain.getBalance()).isEqualTo(new BigDecimal("1000.00"));
        assertThat(domain.getReservedBalance()).isEqualTo(new BigDecimal("100.00"));
        assertThat(domain.getCurrency()).isEqualTo("IDR");
        assertThat(domain.getStatus()).isEqualTo(Wallet.WalletStatus.ACTIVE);
        assertThat(domain.getVersion()).isEqualTo(1L);
    }

    @Test
    void toDomainList_shouldMapEntityListToDomainList() {
        // Given
        WalletEntity entity1 = WalletEntity.builder()
                .id(UUID.randomUUID())
                .accountId("ACC-1")
                .balance(new BigDecimal("100.00"))
                .currency("IDR")
                .status(WalletEntity.WalletStatus.ACTIVE)
                .build();
        WalletEntity entity2 = WalletEntity.builder()
                .id(UUID.randomUUID())
                .accountId("ACC-2")
                .balance(new BigDecimal("200.00"))
                .currency("IDR")
                .status(WalletEntity.WalletStatus.FROZEN)
                .build();

        // When
        List<Wallet> domains = mapper.toDomainList(Arrays.asList(entity1, entity2));

        // Then
        assertThat(domains).hasSize(2);
        assertThat(domains.get(0).getAccountId()).isEqualTo("ACC-1");
        assertThat(domains.get(0).getStatus()).isEqualTo(Wallet.WalletStatus.ACTIVE);
        assertThat(domains.get(1).getAccountId()).isEqualTo("ACC-2");
        assertThat(domains.get(1).getStatus()).isEqualTo(Wallet.WalletStatus.FROZEN);
    }

    @Test
    void mapStatus_shouldMapAllEnumValues() {
        // Test ACTIVE
        assertThat(mapper.mapStatusToDomain(WalletEntity.WalletStatus.ACTIVE))
                .isEqualTo(Wallet.WalletStatus.ACTIVE);
        assertThat(mapper.mapStatusToEntity(Wallet.WalletStatus.ACTIVE))
                .isEqualTo(WalletEntity.WalletStatus.ACTIVE);

        // Test FROZEN
        assertThat(mapper.mapStatusToDomain(WalletEntity.WalletStatus.FROZEN))
                .isEqualTo(Wallet.WalletStatus.FROZEN);
        assertThat(mapper.mapStatusToEntity(Wallet.WalletStatus.FROZEN))
                .isEqualTo(WalletEntity.WalletStatus.FROZEN);

        // Test CLOSED
        assertThat(mapper.mapStatusToDomain(WalletEntity.WalletStatus.CLOSED))
                .isEqualTo(Wallet.WalletStatus.CLOSED);
        assertThat(mapper.mapStatusToEntity(Wallet.WalletStatus.CLOSED))
                .isEqualTo(WalletEntity.WalletStatus.CLOSED);
    }

    @Test
    void mapStatus_shouldHandleNull() {
        assertThat(mapper.mapStatusToDomain(null)).isNull();
        assertThat(mapper.mapStatusToEntity(null)).isNull();
    }

    @Test
    void toEntity_shouldHandleNullDomain() {
        assertThat(mapper.toEntity(null)).isNull();
    }

    @Test
    void toDomain_shouldHandleNullEntity() {
        assertThat(mapper.toDomain(null)).isNull();
    }
}
