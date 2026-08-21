-- QE-LEDGER-003: JournalEntry.isBalanced() only in-memory; partial DB save could lolos.
-- Add check via trigger: sum(debit)==sum(credit) per journal entries' ledger rows before commit.
-- ponytail: trigger checks on ledger_entries insert; for real double-entry across journal, add deferred constraint when moving to single journal table

CREATE OR REPLACE FUNCTION payu_guard_journal_balanced()
RETURNS trigger
LANGUAGE plpgsql
AS $$
DECLARE
    debit_total DECIMAL(19,4);
    credit_total DECIMAL(19,4);
    jid UUID;
BEGIN
    jid := COALESCE(NEW.journal_entry_id, OLD.journal_entry_id);
    IF jid IS NULL THEN RETURN NEW; END IF;

    SELECT COALESCE(SUM(CASE WHEN entry_type='DEBIT' THEN amount ELSE 0 END),0),
           COALESCE(SUM(CASE WHEN entry_type='CREDIT' THEN amount ELSE 0 END),0)
      INTO debit_total, credit_total
      FROM ledger_entries WHERE journal_entry_id = jid;

    -- Allow intermediate state within transaction: only enforce when both sides present and >0
    IF debit_total > 0 AND credit_total > 0 AND debit_total <> credit_total THEN
        RAISE EXCEPTION 'Journal % not balanced: debit % != credit % (QE-LEDGER-003)', jid, debit_total, credit_total;
    END IF;
    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_ledger_balance_check ON ledger_entries;
CREATE TRIGGER trg_ledger_balance_check
    AFTER INSERT OR UPDATE OR DELETE ON ledger_entries
    FOR EACH ROW EXECUTE FUNCTION payu_guard_journal_balanced();
