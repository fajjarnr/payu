package id.payu.wallet.adapter.persistence.mapper;

import id.payu.mapper.config.MappingConfig;
import id.payu.mapper.spi.BaseMapper;
import id.payu.wallet.adapter.persistence.entity.WalletEntity;
import id.payu.wallet.domain.model.Wallet;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

/**
 * MapStruct mapper for Wallet entity-domain conversion.
 *
 * <p>Converts between {@link WalletEntity} (JPA entity) and {@link Wallet} (domain model).
 * Replaces manual mapping in {@code WalletPersistenceAdapter}.</p>
 *
 * <p>Features:
 * <ul>
 *   <li>Automatic mapping of fields with matching names</li>
 *   <li>Custom mapping for WalletStatus enum</li>
 *   <li>TenantId is ignored in domain model (infrastructure concern)</li>
 * </ul>
 *
 * <p>IMP-069: MapStruct Entity-Domain Mapping</p>
 *
 * @see BaseMapper
 * @since IMP-069
 */
@Mapper(
        componentModel = "spring",
        config = MappingConfig.class
)
public interface WalletMapper extends BaseMapper<WalletEntity, Wallet> {

    /**
     * Convert domain Wallet to WalletEntity.
     *
     * <p>TenantId is not mapped as it's an infrastructure concern.
     * The tenant context is handled separately by the multi-tenancy layer.</p>
     *
     * @param domain the domain Wallet
     * @return the WalletEntity
     */
    @Override
    @Mapping(target = "tenantId", ignore = true)
    WalletEntity toEntity(Wallet domain);

    /**
     * Convert WalletEntity to domain Wallet.
     *
     * @param entity the WalletEntity
     * @return the domain Wallet
     */
    @Override
    Wallet toDomain(WalletEntity entity);

    /**
     * Map WalletEntity.WalletStatus to Wallet.WalletStatus.
     *
     * @param status the entity status
     * @return the domain status
     */
    @Named("mapStatusToDomain")
    default Wallet.WalletStatus mapStatusToDomain(WalletEntity.WalletStatus status) {
        if (status == null) {
            return null;
        }
        return Wallet.WalletStatus.valueOf(status.name());
    }

    /**
     * Map Wallet.WalletStatus to WalletEntity.WalletStatus.
     *
     * @param status the domain status
     * @return the entity status
     */
    @Named("mapStatusToEntity")
    default WalletEntity.WalletStatus mapStatusToEntity(Wallet.WalletStatus status) {
        if (status == null) {
            return null;
        }
        return WalletEntity.WalletStatus.valueOf(status.name());
    }
}
