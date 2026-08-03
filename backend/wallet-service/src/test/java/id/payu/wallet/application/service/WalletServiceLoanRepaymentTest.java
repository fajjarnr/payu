package id.payu.wallet.application.service;

import id.payu.cache.service.CacheService;
import id.payu.wallet.domain.model.EntryType;
import id.payu.wallet.domain.model.LedgerEntry;
import id.payu.wallet.domain.model.Wallet;
import id.payu.wallet.domain.port.in.JournalUseCase;
import id.payu.wallet.domain.port.out.WalletEventPublisherPort;
import id.payu.wallet.domain.port.out.WalletPersistencePort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WalletServiceLoanRepaymentTest {

    @Mock private WalletPersistencePort walletPersistencePort;
    @Mock private WalletEventPublisherPort walletEventPublisher;
    @Mock private CacheService cacheService;
    @Mock private JournalUseCase journalUseCase;

    private WalletService walletService;

    @BeforeEach
    void setUp() {
        walletService = new WalletService(walletPersistencePort, walletEventPublisher, cacheService, journalUseCase);
    }

    @Test
    void repayLoan_shouldDebitWalletAndPostBalancedJournal() {
        Wallet wallet = wallet("user-1", "1000.0000");
        when(walletPersistencePort.findByTransactionId(any())).thenReturn(List.of());
        when(walletPersistencePort.findByAccountIdForUpdate("user-1")).thenReturn(java.util.Optional.of(wallet));
        when(walletPersistencePort.save(any(Wallet.class))).thenAnswer(inv -> inv.getArgument(0));

        String transactionId = walletService.repayLoan(
                "user-1", "loan-1", new BigDecimal("125.0000"), "IDR", "repayment-key-1", "loan repayment");

        assertThat(transactionId).isNotBlank();
        assertThat(wallet.getBalance()).isEqualByComparingTo("875.0000");
        ArgumentCaptor<List<LedgerEntry>> entries = ArgumentCaptor.forClass(List.class);
        verify(journalUseCase).createAndPostJournal(
                eq("Loan repayment: loan-1"), eq("LOAN_REPAYMENT"), eq("repayment-key-1"),
                entries.capture(), eq("lending-service"));
        assertThat(entries.getValue()).hasSize(2);
        assertThat(entries.getValue()).extracting(LedgerEntry::getEntryType)
                .containsExactlyInAnyOrder(EntryType.DEBIT, EntryType.CREDIT);
        assertThat(entries.getValue().get(0).getTransactionId())
                .isEqualTo(entries.getValue().get(1).getTransactionId());
    }

    @Test
    void repayLoan_shouldReturnSameTransactionForReplay() {
        UUID transactionId = UUID.nameUUIDFromBytes("LOAN_REPAYMENT:repayment-key-1".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        LedgerEntry existing = LedgerEntry.builder()
                .transactionId(transactionId)
                .accountId("user-1")
                .entryType(EntryType.DEBIT)
                .amount(new BigDecimal("125.0000"))
                .currency("IDR")
                .referenceType("LOAN_REPAYMENT")
                .referenceId("repayment-key-1")
                .build();
        when(walletPersistencePort.findByTransactionId(transactionId)).thenReturn(List.of(existing));

        String result = walletService.repayLoan(
                "user-1", "loan-1", new BigDecimal("125.0000"), "IDR", "repayment-key-1", "loan repayment");

        assertThat(result).isEqualTo(transactionId.toString());
        verifyNoInteractions(journalUseCase, walletEventPublisher);
        verify(walletPersistencePort, never()).findByAccountIdForUpdate(any());
    }

    private Wallet wallet(String accountId, String balance) {
        return Wallet.builder()
                .id(UUID.randomUUID())
                .accountId(accountId)
                .balance(new BigDecimal(balance))
                .reservedBalance(BigDecimal.ZERO)
                .currency("IDR")
                .build();
    }
}
