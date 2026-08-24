-- B1.2 / ADR-0029: idempotent suspense journaling.
-- One journal per clearing lifecycle stage per business reference:
-- duplicate pacs.008/pain.001 or pacs.002 callbacks must fail fast at the DB
-- instead of double-posting suspense legs (app-level upsert-skip is the
-- graceful path; this index is the race backstop).
CREATE UNIQUE INDEX IF NOT EXISTS uq_journal_clearing_reference
    ON journal_entries(reference_type, reference_id)
    WHERE reference_type IN ('CLEARING_HOLD', 'CLEARING_SETTLE', 'CLEARING_REVERSE')
      AND reference_id IS NOT NULL;
