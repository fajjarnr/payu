-- Fix V117 double-entry bug: unique on (reference_type, reference_id) blocked DEBIT+CREDIT pair (TRANSFER 500)
-- Correct to (reference_type, reference_id, entry_type) to allow double-entry while preventing duplicate replay per type
DROP INDEX IF EXISTS uq_ledger_entries_reference_id;
CREATE UNIQUE INDEX IF NOT EXISTS uq_ledger_entries_reference_id
    ON ledger_entries(reference_type, reference_id, entry_type)
    WHERE reference_id IS NOT NULL AND reference_id <> '' AND reference_id <> 'INTERNAL';
