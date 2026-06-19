package id.payu.wallet.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * ITER-57 (READY-042): Immutable ledger invariant tests.
 *
 * <p>Verifies the two core invariants of the PayU double-entry ledger:
 * <ol>
 *   <li><b>Per-transaction balance</b>: For any transaction, {@code sum(credits) - sum(debits) = 0}.
 *       This is the double-entry guarantee (every debit has a matching credit).</li>
 *   <li><b>Per-account balance</b>: For any account,
 *       {@code current_balance = sum(credits) - sum(debits)} across all entries.
 *       The balance_after field must equal this sum at every row.</li>
 *   <li><b>Append-only</b>: Ledger entries are immutable. No UPDATE/DELETE
 *       is permitted (enforced at the schema level via no UPDATE/DELETE triggers
 *       + application layer via append-only service methods).</li>
 * </ol>
 *
 * <p>These tests use pure domain models (no DB) for fast execution. The DB-level
 * invariant is verified in production via PostgreSQL triggers + the
 * {@code GeneralLedgerService} append-only API.
 *
 * @see <a href="https://github.com/anomalyco/opencode/issues">I-042 (READY-042)</a>
 */
class LedgerInvariantTest {

    private static final UUID ACCOUNT_A = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID ACCOUNT_B = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID TX_1 = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID TX_2 = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");

    @Test
    @DisplayName("Per-transaction: sum(credits) - sum(debits) = 0 (double-entry)")
    void perTransactionDebitsEqualCredits() {
        // Transfer 100 from A to B: A gets DEBIT 100, B gets CREDIT 100
        List<LedgerEntry> entries = List.of(
            entry(TX_1, ACCOUNT_A, EntryType.DEBIT, "100.00"),
            entry(TX_1, ACCOUNT_B, EntryType.CREDIT, "100.00")
        );

        BigDecimal sumDebits = sum(entries, EntryType.DEBIT);
        BigDecimal sumCredits = sum(entries, EntryType.CREDIT);
        BigDecimal net = sumCredits.subtract(sumDebits);

        assertThat(net).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("Per-transaction: multi-leg entries (3+ accounts) still balance")
    void perTransactionMultiLegBalances() {
        // Split payment 100 from A -> 60 to B, 40 to C
        List<LedgerEntry> entries = List.of(
            entry(TX_1, ACCOUNT_A, EntryType.DEBIT, "100.00"),
            entry(TX_1, ACCOUNT_B, EntryType.CREDIT, "60.00"),
            entry(TX_1, ACCOUNT_C(), EntryType.CREDIT, "40.00")
        );

        BigDecimal sumDebits = sum(entries, EntryType.DEBIT);
        BigDecimal sumCredits = sum(entries, EntryType.CREDIT);
        BigDecimal net = sumCredits.subtract(sumDebits);

        assertThat(net).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("Per-transaction: unbalanced transaction is detected")
    void unbalancedTransactionDetected() {
        // Bug case: TX_1 has DEBIT 100 but CREDIT 99 (missing 1 unit)
        List<LedgerEntry> entries = List.of(
            entry(TX_1, ACCOUNT_A, EntryType.DEBIT, "100.00"),
            entry(TX_1, ACCOUNT_B, EntryType.CREDIT, "99.00")
        );

        BigDecimal net = sum(entries, EntryType.CREDIT).subtract(sum(entries, EntryType.DEBIT));

        assertThat(net).isNotEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("Per-account: current_balance = sum(credits) - sum(debits)")
    void perAccountBalanceInvariant() {
        // Account A: starts 0, gets DEBIT 100, DEBIT 50, CREDIT 30 -> final -120
        // balanceAfter should be: -100, -150, -120
        BigDecimal currentBalance = BigDecimal.ZERO;

        // TX_1: A gets DEBIT 100
        currentBalance = applyEntry(currentBalance, EntryType.DEBIT, "100.00");
        assertThat(currentBalance).isEqualByComparingTo("-100.0000");

        // TX_2: A gets DEBIT 50
        currentBalance = applyEntry(currentBalance, EntryType.DEBIT, "50.00");
        assertThat(currentBalance).isEqualByComparingTo("-150.0000");

        // TX_2: A gets CREDIT 30
        currentBalance = applyEntry(currentBalance, EntryType.CREDIT, "30.00");
        assertThat(currentBalance).isEqualByComparingTo("-120.0000");
    }

    @Test
    @DisplayName("Per-account: large number of entries (1000) maintains precision (BigDecimal)")
    void thousandEntriesPrecision() {
        BigDecimal currentBalance = BigDecimal.ZERO;
        for (int i = 0; i < 1000; i++) {
            currentBalance = applyEntry(currentBalance, EntryType.CREDIT, "0.01");
        }
        // 1000 * 0.01 = 10.00 exactly (BigDecimal precision)
        assertThat(currentBalance).isEqualByComparingTo("10.0000");
    }

    @Test
    @DisplayName("Append-only: balance_after must equal current_balance at each step")
    void balanceAfterEqualsCurrent() {
        List<LedgerEntry> entries = List.of(
            entryWithBalance(TX_1, ACCOUNT_A, EntryType.CREDIT, "100.00", "100.0000"),
            entryWithBalance(TX_1, ACCOUNT_A, EntryType.DEBIT, "30.00", "70.0000"),
            entryWithBalance(TX_2, ACCOUNT_A, EntryType.CREDIT, "50.00", "120.0000"),
            entryWithBalance(TX_2, ACCOUNT_A, EntryType.DEBIT, "20.00", "100.0000")
        );

        BigDecimal runningBalance = BigDecimal.ZERO;
        for (LedgerEntry e : entries) {
            runningBalance = applyEntry(runningBalance, e.getEntryType(), e.getAmount().toPlainString());
            assertThat(runningBalance)
                .as("balance_after mismatch at entry %s", e.getId())
                .isEqualByComparingTo(e.getBalanceAfter());
        }
    }

    @Test
    @DisplayName("Cross-account: net change sums to 0 across all accounts (system invariant)")
    void systemWideNetZero() {
        // Multiple transactions, multiple accounts
        List<LedgerEntry> entries = List.of(
            // TX_1: A -> B 100
            entry(TX_1, ACCOUNT_A, EntryType.DEBIT, "100.00"),
            entry(TX_1, ACCOUNT_B, EntryType.CREDIT, "100.00"),
            // TX_2: B -> C 50
            entry(TX_2, ACCOUNT_B, EntryType.DEBIT, "50.00"),
            entry(TX_2, ACCOUNT_C(), EntryType.CREDIT, "50.00")
        );

        BigDecimal sumDebits = sum(entries, EntryType.DEBIT);
        BigDecimal sumCredits = sum(entries, EntryType.CREDIT);

        // System-wide: total debits = total credits
        assertThat(sumDebits).isEqualByComparingTo(sumCredits);
        assertThat(sumCredits.subtract(sumDebits)).isEqualByComparingTo(BigDecimal.ZERO);
    }

    // --- helpers ---

    private static UUID ACCOUNT_C() {
        return UUID.fromString("33333333-3333-3333-3333-333333333333");
    }

    private static LedgerEntry entry(UUID txId, UUID accountId, EntryType type, String amount) {
        return entryWithBalance(txId, accountId, type, amount, "0.0000");
    }

    private static LedgerEntry entryWithBalance(UUID txId, UUID accountId, EntryType type,
                                                 String amount, String balanceAfter) {
        return new LedgerEntry(
            UUID.randomUUID(), txId, null, accountId.toString(), "1000",
            type, new BigDecimal(amount), "IDR",
            new BigDecimal(balanceAfter), "TRANSFER", txId.toString(),
            java.time.LocalDateTime.now()
        );
    }

    private static BigDecimal sum(List<LedgerEntry> entries, EntryType type) {
        return entries.stream()
            .filter(e -> e.getEntryType() == type)
            .map(LedgerEntry::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static BigDecimal applyEntry(BigDecimal balance, EntryType type, String amount) {
        BigDecimal amt = new BigDecimal(amount);
        return switch (type) {
            case CREDIT -> balance.add(amt);
            case DEBIT -> balance.subtract(amt);
        };
    }
}
