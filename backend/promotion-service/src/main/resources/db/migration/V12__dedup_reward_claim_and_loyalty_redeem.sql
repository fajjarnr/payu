-- V12: PROMO-002/PROMO-003 — durable dedup guards for reward claims and
-- loyalty redemptions.
--
-- PROMO-003 (CB-032): at most one reward per (account_id, transaction_id).
-- PROMO-002 (CB-027): at most one redemption per (account_id, transaction_id).
-- Existing duplicates are collapsed to the earliest record (same strategy as
-- V11 cashback dedup) before the unique partial indexes are created.

DELETE FROM rewards a USING rewards b
WHERE a.id > b.id
  AND a.account_id = b.account_id
  AND a.transaction_id = b.transaction_id
  AND a.transaction_id IS NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uq_rewards_account_transaction
    ON rewards (account_id, transaction_id)
    WHERE transaction_id IS NOT NULL;

DELETE FROM loyalty_points a USING loyalty_points b
WHERE a.id > b.id
  AND a.account_id = b.account_id
  AND a.transaction_id = b.transaction_id
  AND a.transaction_type = 'REDEEMED'
  AND b.transaction_type = 'REDEEMED'
  AND a.transaction_id IS NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uq_loyalty_redeem_account_transaction
    ON loyalty_points (account_id, transaction_id)
    WHERE transaction_type = 'REDEEMED' AND transaction_id IS NOT NULL;
