package id.payu.wallet.adapter.persistence.mapper;

import id.payu.mapper.config.MappingConfig;
import id.payu.mapper.spi.BaseMapper;
import id.payu.wallet.adapter.persistence.entity.JournalEntryEntity;
import id.payu.wallet.adapter.persistence.entity.LedgerEntryEntity;
import id.payu.wallet.domain.model.LedgerEntry;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import id.payu.wallet.domain.model.EntryType;

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
    @Mapping(target = "journalEntry", ignore = true)
    @Mapping(target = "entryType", expression = "java(mapEntryTypeToEntity(domain.getEntryType()))")
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
    @Mapping(target = "journalEntryId", expression = "java(entity.getJournalEntry() != null ? entity.getJournalEntry().getId() : null)")
    @Mapping(target = "entryType", expression = "java(mapEntryTypeToDomain(entity.getEntryType()))")
    LedgerEntry toDomain(LedgerEntryEntity entity);

    /**
     * Map EntryType to String for entity.
     *
     * @param entryType the domain entry type
     * @return the entity entry type string
     */
    default String mapEntryTypeToEntity(EntryType entryType) {
        if (entryType == null) {
            return null;
        }
        return entryType.name();
    }

    /**
     * Map String to EntryType for domain.
     *
     * @param entryType the entity entry type string
     * @return the domain entry type
     */
    default EntryType mapEntryTypeToDomain(String entryType) {
        if (entryType == null) {
            return null;
        }
        return EntryType.valueOf(entryType);
    }
}
