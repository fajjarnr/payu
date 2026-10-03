package id.payu.mapper.spi;

import org.mapstruct.MappingTarget;

import java.util.List;
import java.util.Set;


/**
 * Base mapper interface for entity-domain mapping.
 *
 * <p>This interface defines standard mapping methods that all mappers should implement.
 * It provides compile-time type-safe mapping between domain models and JPA entities,
 * eliminating manual mapping code.</p>
 *
 * <p>Features:
 * <ul>
 *   <li>Single object mapping (toEntity, toDomain)</li>
 *   <li>Collection mapping (List, Set)</li>
 *   <li>Update mapping (merge domain into existing entity)</li>
 *   <li>Null-safe mapping</li>
 * </ul>
 *
 * <p>Example usage:
 * <pre>
 * @Mapper(componentModel = "spring", uses = {ReferenceMapper.class})
 * public interface WalletMapper extends BaseMapper<WalletEntity, Wallet> {
 *     // Custom mappings can be defined here
 *
 *     @Mapping(target = "status", expression = "java(mapStatus(entity.getStatus()))")
 *     Wallet toDomain(WalletEntity entity);
 *
 *     default WalletStatus mapStatus(WalletStatus status) {
 *         return WalletStatus.valueOf(status.name());
 *     }
 * }
 * </pre>
 *
 * @param <E> the entity type (JPA entity)
 * @param <D> the domain type (domain model)
 *
 * @see org.mapstruct.Mapper
 * @since IMP-069
 */
public interface BaseMapper<E, D> {

    E toEntity(D domain);

    D toDomain(E entity);

    List<D> toDomainList(List<E> entities);

    List<E> toEntityList(List<D> domains);

    Set<D> toDomainSet(Set<E> entities);

    Set<E> toEntitySet(Set<D> domains);

    /**
     * Update an existing entity with values from a domain object.
     *
     * <p>This method merges non-null values from the domain object into the entity,
     * preserving the entity's identity and any fields not present in the domain.</p>
     *
     * @param domain the domain object with updated values
     * @param entity the entity to update (will be modified)
     */
    void updateEntityFromDomain(D domain, @MappingTarget E entity);

    /**
     * Update an existing domain object with values from an entity.
     *
     * @param entity the entity with updated values
     * @param domain the domain object to update (will be modified)
     */
    void updateDomainFromEntity(E entity, @MappingTarget D domain);
}
