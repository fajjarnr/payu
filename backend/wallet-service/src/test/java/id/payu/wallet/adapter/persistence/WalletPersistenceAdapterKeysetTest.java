package id.payu.wallet.adapter.persistence;

import id.payu.wallet.adapter.persistence.entity.WalletTransactionEntity;
import id.payu.wallet.adapter.persistence.repository.LedgerEntryJpaRepository;
import id.payu.wallet.adapter.persistence.repository.WalletJpaRepository;
import id.payu.wallet.adapter.persistence.repository.WalletTransactionJpaRepository;
import id.payu.wallet.adapter.persistence.mapper.LedgerEntryMapper;
import id.payu.wallet.domain.model.WalletTransaction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WalletPersistenceAdapterKeysetTest {

    @Mock
    private WalletJpaRepository walletRepository;

    @Mock
    private WalletTransactionJpaRepository transactionRepository;

    @Mock
    private LedgerEntryJpaRepository ledgerEntryRepository;

    @Mock
    private LedgerEntryMapper ledgerEntryMapper;

    @InjectMocks
    private WalletPersistenceAdapter adapter;

    private UUID walletId;
    private LocalDateTime cursorCreatedAt;
    private UUID cursorId;

    @BeforeEach
    void setUp() {
        walletId = UUID.randomUUID();
        cursorCreatedAt = LocalDateTime.of(2026, 8, 17, 5, 0, 0);
        cursorId = UUID.randomUUID();
    }

    @Test
    @DisplayName("ARCH-PAGE-001: findTransactionsByWalletIdKeyset with cursor delegates to keyset query")
    void testFindTransactionsByWalletIdKeyset_withCursor() {
        WalletTransactionEntity entity = new WalletTransactionEntity();
        entity.setId(UUID.randomUUID());
        entity.setWalletId(walletId);
        entity.setReferenceId("REF-001");
        entity.setType(id.payu.wallet.adapter.persistence.entity.TransactionType.CREDIT);
        entity.setAmount(new BigDecimal("100000.0000"));
        entity.setBalanceAfter(new BigDecimal("200000.0000"));

        when(transactionRepository.findByWalletIdKeyset(
                eq(walletId), eq(cursorCreatedAt), eq(cursorId), eq(PageRequest.of(0, 10))))
                .thenReturn(List.of(entity));

        List<WalletTransaction> result = adapter.findTransactionsByWalletIdKeyset(walletId, cursorCreatedAt, cursorId, 10);

        assertThat(result).hasSize(1);
        verify(transactionRepository).findByWalletIdKeyset(
                eq(walletId), eq(cursorCreatedAt), eq(cursorId), eq(PageRequest.of(0, 10)));
    }

    @Test
    @DisplayName("ARCH-PAGE-001: findTransactionsByWalletIdKeyset without cursor falls back to offset query")
    void testFindTransactionsByWalletIdKeyset_withoutCursor() {
        WalletTransactionEntity entity = new WalletTransactionEntity();
        entity.setId(UUID.randomUUID());
        entity.setWalletId(walletId);
        entity.setReferenceId("REF-002");
        entity.setType(id.payu.wallet.adapter.persistence.entity.TransactionType.DEBIT);
        entity.setAmount(new BigDecimal("50000.0000"));
        entity.setBalanceAfter(new BigDecimal("150000.0000"));

        when(transactionRepository.findByWalletIdOrderByCreatedAtDesc(
                eq(walletId), eq(PageRequest.of(0, 10))))
                .thenReturn(List.of(entity));

        List<WalletTransaction> result = adapter.findTransactionsByWalletIdKeyset(walletId, null, null, 10);

        assertThat(result).hasSize(1);
        verify(transactionRepository).findByWalletIdOrderByCreatedAtDesc(
                eq(walletId), eq(PageRequest.of(0, 10)));
    }
}
