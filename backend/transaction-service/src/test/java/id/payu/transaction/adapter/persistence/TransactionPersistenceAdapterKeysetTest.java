package id.payu.transaction.adapter.persistence;

import id.payu.transaction.adapter.persistence.entity.TransactionEntity;
import id.payu.transaction.adapter.persistence.repository.TransactionJpaRepository;
import id.payu.transaction.config.ShardingConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionPersistenceAdapterKeysetTest {

    @Mock
    private TransactionJpaRepository transactionJpaRepository;

    @Mock
    private ShardingConfig shardingConfig;

    @InjectMocks
    private TransactionPersistenceAdapter adapter;

    private UUID accountId;
    private Instant cursorCreatedAt;
    private UUID cursorId;

    @BeforeEach
    void setUp() {
        accountId = UUID.randomUUID();
        cursorCreatedAt = Instant.parse("2026-08-17T05:00:00Z");
        cursorId = UUID.randomUUID();
    }

    @Test
    @DisplayName("ARCH-PAGE-001: findByAccountIdKeyset with cursor delegates to keyset query")
    void testFindByAccountIdKeyset_withCursor() {
        TransactionEntity entity = new TransactionEntity();
        when(transactionJpaRepository.findByAccountIdKeyset(
                eq(accountId), eq(cursorCreatedAt), eq(cursorId), eq(PageRequest.of(0, 20))))
                .thenReturn(List.of(entity));

        List<TransactionEntity> result = adapter.findByAccountIdKeyset(accountId, cursorCreatedAt, cursorId, 20);

        assertThat(result).hasSize(1);
        verify(transactionJpaRepository).findByAccountIdKeyset(
                eq(accountId), eq(cursorCreatedAt), eq(cursorId), eq(PageRequest.of(0, 20)));
    }

    @Test
    @DisplayName("ARCH-PAGE-001: findByAccountIdKeyset without cursor falls back to first page offset query")
    void testFindByAccountIdKeyset_withoutCursor() {
        TransactionEntity entity = new TransactionEntity();
        when(transactionJpaRepository.findByAccountId(eq(accountId), eq(PageRequest.of(0, 20))))
                .thenReturn(List.of(entity));

        List<TransactionEntity> result = adapter.findByAccountIdKeyset(accountId, null, null, 20);

        assertThat(result).hasSize(1);
        verify(transactionJpaRepository).findByAccountId(eq(accountId), eq(PageRequest.of(0, 20)));
    }
}
