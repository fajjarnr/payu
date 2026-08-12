-- PROMO-001 / CB-026: one cashback record per source transaction.
-- Events are at-least-once and the saga retries, so the same transaction must
-- never produce a second cashback record. The unique index is the durable guard;
-- the saga treats a duplicate insert as a replay and returns the existing record.

DELETE FROM cashbacks
WHERE id NOT IN (SELECT MIN(id) FROM cashbacks GROUP BY transaction_id);

CREATE UNIQUE INDEX IF NOT EXISTS uq_cashback_transaction_id ON cashbacks (transaction_id);
