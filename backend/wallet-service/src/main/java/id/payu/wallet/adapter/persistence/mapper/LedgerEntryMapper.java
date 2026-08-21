package id.payu.wallet.adapter.persistence.mapper;

import id.payu.mapper.config.MappingConfig;
import id.payu.mapper.spi.BaseMapper;
import id.payu.wallet.adapter.persistence.entity.JournalEntryEntity;
import id.payu.wallet.adapter.persistence.entity.LedgerEntryEntity;
import id.payu.wallet.domain.model.LedgerEntry;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

/**
 * MapStruct mapper for LedgerEntry entity-domain conversion.
 *
 * <p>Converts between {@link LedgerEntryEntity} (JPA entity) and {@link LedgerEntry} (domain model).
 * Replaces manual mapping in {@code WalletPersistenceAdapter}.</p>
 *
 * <p>Features:
 * <ul>
 *   <li>Automatic mapping of fields with matching names</li>
 *   <li>Custom mapping for EntryType enum</li>
 *   <li>Handles JournalEntry relationship (maps to journalEntryId)</li>
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
public interface LedgerEntryMapper extends BaseMapper<LedgerEntryEntity, LedgerEntry> {

    /**
     * Convert domain LedgerEntry to LedgerEntryEntity.
     *
     * <p>JournalEntry relationship is not mapped directly; it's handled by JPA
     * when persisting. The journalEntryId is stored as a simple field.</p>
     *
     * @param domain the domain LedgerEntry
     * @return the LedgerEntryEntity
     */
    @Override
    @BeanMapping(ignoreUnmappedSourceProperties = "journalEntryId")
    @Mapping(target = "journalEntry", ignore = true)
    LedgerEntryEntity toEntity(LedgerEntry domain);

    /**
     * Custom after-mapping to set journalEntry via setter (not via builder).
     */
    default LedgerEntryEntity toEntityWithJournal(LedgerEntry domain, JournalEntryEntity journalEntry) {
        LedgerEntryEntity entity = toEntity(domain);
        entity.setJournalEntry(journalEntry);
        return entity;
    }

    /**
     * Convert LedgerEntryEntity to domain LedgerEntry.
     *
     * <p>Maps the journalEntry relationship to journalEntryId in the domain.</p>
     *
     * @param entity the LedgerEntryEntity
     * @return the domain LedgerEntry
     */
    @Override
    @BeanMapping(ignoreUnmappedSourceProperties = {"new", "journalEntry"})
    @Mapping(target = "journalEntryId", expression = "java(entity.getJournalEntry() != null ? entity.getJournalEntry().getId() : null)")
    LedgerEntry toDomain(LedgerEntryEntity entity);

    @Override
    default void updateEntityFromDomain(LedgerEntry domain, @MappingTarget LedgerEntryEntity entity) {
        throw new UnsupportedOperationException("ledger append-only: use toEntity + persist, not update (WL-001)");
    }

    @Override
    @BeanMapping(ignoreUnmappedSourceProperties = {"new", "journalEntry"})
    @Mapping(target = "journalEntryId", expression = "java(entity.getJournalEntry() != null ? entity.getJournalEntry().getId() : null)")
    void updateDomainFromEntity(LedgerEntryEntity entity, @MappingTarget LedgerEntry domain);
}
