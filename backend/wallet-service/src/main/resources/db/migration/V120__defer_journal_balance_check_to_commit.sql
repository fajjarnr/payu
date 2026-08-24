-- B1.2 / ADR-0029: make the V118 journal balance guard commit-time.
-- The immediate per-row trigger rejects legitimate multi-leg journals
-- (e.g. clearing hold = debit CASA(amount+fee), credit suspense, credit fee)
-- because intermediate insert order leaves debit != credit inside the tx.
-- Deferring to COMMIT keeps the invariant enforced exactly where it matters:
-- no unbalanced journal can ever be visible outside the transaction.
-- ponytail: row-level UPDATE case dropped — ledger immutability trigger (V112) blocks updates anyway

DROP TRIGGER IF EXISTS trg_ledger_balance_check ON ledger_entries;

CREATE CONSTRAINT TRIGGER trg_ledger_balance_check
    AFTER INSERT OR DELETE ON ledger_entries
    DEFERRABLE INITIALLY DEFERRED
    FOR EACH ROW EXECUTE FUNCTION payu_guard_journal_balanced();
