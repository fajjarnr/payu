package id.payu.wallet.adapter.persistence.mapper;

import id.payu.wallet.adapter.persistence.entity.JournalEntryEntity;
import id.payu.wallet.adapter.persistence.entity.LedgerEntryEntity;
import id.payu.wallet.domain.model.LedgerEntry;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link LedgerEntryMapper}.
 *
 * <p>Verifies:
 * <ul>
 *   <li>Domain to entity mapping</li>
 *   <li>Entity to domain mapping</li>
 *   <li>Collection mapping</li>
 *   <li>Enum mapping (EntryType)</li>
 *   <li>JournalEntry relationship handling</li>
 *   <li>Null handling</li>
 * </ul>
 *
 * <p>IMP-069: MapStruct Entity-Domain Mapping</p>
 *
 * @since IMP-069
 */
class LedgerEntryMapperTest {

    private final LedgerEntryMapper mapper = Mappers.getMapper(LedgerEntryMapper.class);

    @Test
    void toEntity_shouldMapDomainToEntity() {
        // Given
        UUID id = UUID.randomUUID();
        UUID transactionId = UUID.randomUUID();
        LedgerEntry domain = LedgerEntry.builder()
                .id(id)
                .transactionId(transactionId)
                .accountId("ACC-123")
                .coaCode("1001")
                .entryType(LedgerEntry.EntryType.DEBIT)
                .amount(new BigDecimal("500.00"))
                .currency("IDR")
                .balanceAfter(new BigDecimal("500.00"))
                .referenceType("TRANSFER")
                .referenceId("REF-001")
                .createdAt(LocalDateTime.now())
                .build();

        // When
        LedgerEntryEntity entity = mapper.toEntity(domain);

        // Then
        assertThat(entity).isNotNull();
        assertThat(entity.getId()).isEqualTo(id);
        assertThat(entity.getTransactionId()).isEqualTo(transactionId);
        assertThat(entity.getAccountId()).isEqualTo("ACC-123");
        assertThat(entity.getCoaCode()).isEqualTo("1001");
        assertThat(entity.getEntryType()).isEqualTo("DEBIT");
        assertThat(entity.getAmount()).isEqualTo(new BigDecimal("500.00"));
        assertThat(entity.getCurrency()).isEqualTo("IDR");
        assertThat(entity.getBalanceAfter()).isEqualTo(new BigDecimal("500.00"));
        assertThat(entity.getReferenceType()).isEqualTo("TRANSFER");
        assertThat(entity.getReferenceId()).isEqualTo("REF-001");
        // journalEntry should be null (ignored in mapping)
        assertThat(entity.getJournalEntry()).isNull();
    }

    @Test
    void toDomain_shouldMapEntityToDomain() {
        // Given
        UUID id = UUID.randomUUID();
        UUID transactionId = UUID.randomUUID();
        UUID journalEntryId = UUID.randomUUID();
        JournalEntryEntity journalEntry = new JournalEntryEntity();
        journalEntry.setId(journalEntryId);

        LedgerEntryEntity entity = LedgerEntryEntity.builder()
                .id(id)
                .transactionId(transactionId)
                .accountId("ACC-123")
                .coaCode("1001")
                .entryType("CREDIT")
                .amount(new BigDecimal("1000.00"))
                .currency("IDR")
                .balanceAfter(new BigDecimal("1500.00"))
                .referenceType("DEPOSIT")
                .referenceId("REF-002")
                .createdAt(LocalDateTime.now())
                .build();
        entity.setJournalEntry(journalEntry);

        // When
        LedgerEntry domain = mapper.toDomain(entity);

        // Then
        assertThat(domain).isNotNull();
        assertThat(domain.getId()).isEqualTo(id);
        assertThat(domain.getTransactionId()).isEqualTo(transactionId);
        assertThat(domain.getJournalEntryId()).isEqualTo(journalEntryId);
        assertThat(domain.getAccountId()).isEqualTo("ACC-123");
        assertThat(domain.getCoaCode()).isEqualTo("1001");
        assertThat(domain.getEntryType()).isEqualTo(LedgerEntry.EntryType.CREDIT);
        assertThat(domain.getAmount()).isEqualTo(new BigDecimal("1000.00"));
        assertThat(domain.getCurrency()).isEqualTo("IDR");
        assertThat(domain.getBalanceAfter()).isEqualTo(new BigDecimal("1500.00"));
        assertThat(domain.getReferenceType()).isEqualTo("DEPOSIT");
        assertThat(domain.getReferenceId()).isEqualTo("REF-002");
    }

    @Test
    void toDomain_shouldHandleNullJournalEntry() {
        // Given
        LedgerEntryEntity entity = LedgerEntryEntity.builder()
                .id(UUID.randomUUID())
                .transactionId(UUID.randomUUID())
                .accountId("ACC-123")
                .entryType("DEBIT")
                .amount(new BigDecimal("100.00"))
                .currency("IDR")
                .balanceAfter(new BigDecimal("900.00"))
                .createdAt(LocalDateTime.now())
                .build();
        // journalEntry is null

        // When
        LedgerEntry domain = mapper.toDomain(entity);

        // Then
        assertThat(domain.getJournalEntryId()).isNull();
    }

    @Test
    void toDomainList_shouldMapEntityListToDomainList() {
        // Given
        LedgerEntryEntity entity1 = LedgerEntryEntity.builder()
                .id(UUID.randomUUID())
                .transactionId(UUID.randomUUID())
                .accountId("ACC-1")
                .entryType("DEBIT")
                .amount(new BigDecimal("100.00"))
                .currency("IDR")
                .balanceAfter(new BigDecimal("900.00"))
                .createdAt(LocalDateTime.now())
                .build();
        LedgerEntryEntity entity2 = LedgerEntryEntity.builder()
                .id(UUID.randomUUID())
                .transactionId(UUID.randomUUID())
                .accountId("ACC-1")
                .entryType("CREDIT")
                .amount(new BigDecimal("200.00"))
                .currency("IDR")
                .balanceAfter(new BigDecimal("1100.00"))
                .createdAt(LocalDateTime.now())
                .build();

        // When
        List<LedgerEntry> domains = mapper.toDomainList(Arrays.asList(entity1, entity2));

        // Then
        assertThat(domains).hasSize(2);
        assertThat(domains.get(0).getEntryType()).isEqualTo(LedgerEntry.EntryType.DEBIT);
        assertThat(domains.get(1).getEntryType()).isEqualTo(LedgerEntry.EntryType.CREDIT);
    }

    @Test
    void mapEntryType_shouldMapAllEnumValues() {
        // Test DEBIT
        assertThat(mapper.mapEntryTypeToDomain("DEBIT"))
                .isEqualTo(LedgerEntry.EntryType.DEBIT);
        assertThat(mapper.mapEntryTypeToEntity(LedgerEntry.EntryType.DEBIT))
                .isEqualTo("DEBIT");

        // Test CREDIT
        assertThat(mapper.mapEntryTypeToDomain("CREDIT"))
                .isEqualTo(LedgerEntry.EntryType.CREDIT);
        assertThat(mapper.mapEntryTypeToEntity(LedgerEntry.EntryType.CREDIT))
                .isEqualTo("CREDIT");
    }

    @Test
    void mapEntryType_shouldHandleNull() {
        assertThat(mapper.mapEntryTypeToDomain(null)).isNull();
        assertThat(mapper.mapEntryTypeToEntity(null)).isNull();
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
