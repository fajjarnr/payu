package id.payu.wallet.application.service;

import id.payu.wallet.domain.model.EntryType;
import id.payu.wallet.domain.model.JournalEntry;
import id.payu.wallet.domain.model.LedgerEntry;
import id.payu.wallet.domain.port.out.JournalPersistencePort;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JournalServiceTest {

    private final JournalPersistencePort persistencePort = mock(JournalPersistencePort.class);
    private final JournalService service = new JournalService(persistencePort);

    @Test
    void letsJpaGenerateIdsForNewJournalAndLedgerEntries() {
        when(persistencePort.generateJournalNumber()).thenReturn("JRN-20260804-00001");
        when(persistencePort.saveJournal(any(JournalEntry.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        JournalEntry saved = service.createAndPostJournal(
                "Loan repayment",
                "LOAN_REPAYMENT",
                "repayment-key",
                List.of(
                        LedgerEntry.builder()
                                .entryType(EntryType.DEBIT)
                                .amount(new BigDecimal("100.0000"))
                                .build(),
                        LedgerEntry.builder()
                                .entryType(EntryType.CREDIT)
                                .amount(new BigDecimal("100.0000"))
                                .build()),
                "system");

        assertThat(saved.getId()).isNull();
        assertThat(saved.getEntries()).allSatisfy(entry -> assertThat(entry.getId()).isNull());
    }
}
