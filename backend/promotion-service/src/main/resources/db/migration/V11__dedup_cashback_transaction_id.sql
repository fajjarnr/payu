-- PROMO-001 / CB-026: one cashback record per source transaction.
-- Events are at-least-once and the saga retries, so the same transaction must
-- never produce a second cashback record. The unique index is the durable guard;
-- the saga treats a duplicate insert as a replay and returns the existing record.

-- id is a UUID — PostgreSQL has no min(uuid) aggregate; keep the earliest row
-- per transaction_id via DISTINCT ON instead.
DELETE FROM cashbacks
WHERE id NOT IN (
    SELECT id FROM (
        SELECT DISTINCT ON (transaction_id) id
        FROM cashbacks
        ORDER BY transaction_id, created_at
    ) keep
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_cashback_transaction_id ON cashbacks (transaction_id);
